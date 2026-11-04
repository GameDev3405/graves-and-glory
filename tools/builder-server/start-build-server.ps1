# Builder server supervisor for Graves and Glory: one save, gg-build, holding all seven builder worlds (camp, lobby and the five
# maps) as dimensions; /map in game moves a player between them with no restart (Kort 2026-11-03). Run it in a window or as the
# GGBuilderServer scheduled task (install-boot-task.ps1). Windows PowerShell 5.1 compatible.
#
# Added for plan task 2A.1: restart after a crash with exponential backoff, a backup before every start, a backup
# schedule while it runs, an offsite copy, log rotation, a version stamp, and a refusal to loop on a missing EULA.
param(
    [ValidateRange(2,32)][int]$MemoryGB = 6,
    [ValidateRange(5,1440)][int]$BackupIntervalMinutes = 60,   # scheduled backup when the world changed and the newest backup is older than this
    [ValidateRange(1,600)][int]$BackoffStartSeconds = 5,       # first restart delay after a crash; doubles each time
    [ValidateRange(1,3600)][int]$BackoffMaxSeconds = 300,
    [ValidateRange(30,3600)][int]$HealthyAfterSeconds = 600,   # a run this long resets the backoff
    [ValidateRange(1,100)][int]$MaxQuickFailures = 10,         # give up after this many failed starts in a row
    [string]$Python = 'python',
    [string]$JavaExe = 'java',
    [long]$CheckCrashedSince = -1,      # dry run for tests: report Test-RunCrashed for -CheckDir since this Unix time, then exit (1 crash, 0 not)
    [string]$CheckDir = $PSScriptRoot
)
# The server PC has no Python (2026-11-03), so the exit-0 crash check is pure PowerShell. Minecraft can exit 0 after a crash: a
# tick-loop exception, then a normal shutdown. The run crashed if crash-reports/ got a file at or after the start, or if
# logs/latest.log was written at or after the start and holds a crash line. Returns the reason, or $null for a clean run.
function Test-RunCrashed([string]$dir, [DateTime]$sinceUtc) {
    $reports = Join-Path $dir 'crash-reports'
    if (Test-Path -LiteralPath $reports -PathType Container) {
        $new = @(Get-ChildItem -LiteralPath $reports -File -ErrorAction SilentlyContinue | Where-Object { $_.LastWriteTimeUtc -ge $sinceUtc } | ForEach-Object { $_.Name } | Sort-Object)
        if ($new.Count -gt 0) { return 'new crash report: ' + ($new -join ', ') }
    }
    $log = Join-Path $dir 'logs/latest.log'
    try {
        $item = Get-Item -LiteralPath $log -ErrorAction Stop
        if ($item.LastWriteTimeUtc -ge $sinceUtc) {
            $text = [System.IO.File]::ReadAllText($item.FullName)
            foreach ($m in @('Exception in server tick loop', 'Encountered an unexpected exception')) {
                if ($text.Contains($m)) { return 'latest.log: ' + $m }
            }
        }
    } catch { }
    return $null
}
if ($CheckCrashedSince -ge 0) {
    $why = Test-RunCrashed $CheckDir ([DateTimeOffset]::FromUnixTimeSeconds($CheckCrashedSince).UtcDateTime)
    if ($why) { Write-Host "crashed: $why"; exit 1 } else { Write-Host 'clean'; exit 0 }
}
Set-Location -LiteralPath $PSScriptRoot
$ErrorActionPreference = 'Stop'
$world = 'gg-build'   # the save folder and the backups/<world> folder name
$ops = Join-Path $PSScriptRoot 'builder_ops.py'
New-Item -ItemType Directory -Force -Path 'logs' | Out-Null

function Write-Log([string]$msg) {
    $line = '{0} [builder] {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $msg
    Write-Host $line   # Write-Host, not Write-Output: Invoke-Ops callers read its return value as the exit code
    Add-Content -LiteralPath 'logs/supervisor.log' -Value $line
}
# Maintenance calls must never take the server down: a failing helper is logged and the loop carries on.
function Invoke-Ops([string[]]$opsArgs) {
    try {
        $ErrorActionPreference = 'Continue'   # Windows PowerShell 5.1 turns native stderr into terminating errors otherwise
        $out = & $Python $ops --server-dir $PSScriptRoot @opsArgs 2>&1
        $rc = $LASTEXITCODE
        foreach ($l in $out) { Write-Log "ops $($opsArgs[0]): $l" }
        return $rc
    } catch { Write-Log "ops $($opsArgs[0]) failed: $_"; return 99 }
}

if (!(Test-Path -LiteralPath 'fabric-server-launch.jar')) { Write-Log 'fabric-server-launch.jar is missing.'; exit 3 }
$failures = 0
$backoff = $BackoffStartSeconds

while ($true) {
    # The EULA is the owner's to accept. This script never writes eula.txt; without it the server cannot start, so stop here
    # instead of crash-looping.
    $pre = Invoke-Ops @('preflight')
    if ($pre -eq 3) { Write-Log 'start blocked by preflight (see lines above); fix it and start again.'; exit 3 }

    Set-Content -LiteralPath 'current-world.txt' -Value $world -NoNewline
    $lines = Get-Content -LiteralPath 'server.properties' | ForEach-Object { if ($_ -like 'level-name=*') { "level-name=$world" } else { $_ } }
    Set-Content -LiteralPath 'server.properties' -Value $lines -Encoding ascii

    [void](Invoke-Ops @('rotate-logs'))
    [void](Invoke-Ops @('backup', '--world', $world, '--reason', 'prestart', '--changed-only'))
    [void](Invoke-Ops @('stamp', '--world', $world))
    [void](Invoke-Ops @('offsite'))

    Write-Log "starting world: $world"
    $started = Get-Date
    $startedUtc = [DateTime]::UtcNow.AddSeconds(-2)   # Test-RunCrashed counts crash reports and a latest.log written from here on
    $p = Start-Process -FilePath $JavaExe -NoNewWindow -PassThru -ArgumentList @('-Xms1G', "-Xmx${MemoryGB}G", "-Dgg.build=all", '-jar', 'fabric-server-launch.jar', 'nogui')
    $null = $p.Handle   # keeps the process handle so ExitCode is readable after exit
    Set-Content -LiteralPath 'server.pid' -Value $p.Id -NoNewline
    $nextCheck = (Get-Date).AddMinutes($BackupIntervalMinutes)
    while (-not $p.WaitForExit(30000)) {
        if ((Get-Date) -ge $nextCheck) {
            $nextCheck = (Get-Date).AddMinutes($BackupIntervalMinutes)
            [void](Invoke-Ops @('backup', '--world', $world, '--if-due', "$BackupIntervalMinutes"))
            [void](Invoke-Ops @('offsite'))
            [void](Invoke-Ops @('rotate-logs'))
        }
    }
    $code = $p.ExitCode
    Remove-Item -LiteralPath 'server.pid' -ErrorAction SilentlyContinue
    $ranSeconds = ((Get-Date) - $started).TotalSeconds
    Write-Log "server exited with $code after $([int]$ranSeconds) s"
    [void](Invoke-Ops @('offsite'))   # the mod writes a backup on stop; ship it

    Remove-Item -LiteralPath 'next-world.txt' -ErrorAction SilentlyContinue   # left by the old world-switching mod; no longer used
    # Minecraft can exit 0 after a crash (2026-11-03), so exit 0 is a clean stop only when the run left no crash report and no
    # crash line in latest.log (Test-RunCrashed, pure PowerShell: no Python on the server PC).
    if ($code -eq 0) {
        $why = $null
        try { $why = Test-RunCrashed $PSScriptRoot $startedUtc } catch { Write-Log "crash check failed: $_" }
        if ($why) { Write-Log "exit 0, but the run crashed ($why); restarting as after a crash" }
        else { Write-Log 'clean stop; supervisor ending.'; break }
    }

    if ($ranSeconds -ge $HealthyAfterSeconds) { $failures = 0; $backoff = $BackoffStartSeconds }
    $failures++
    if ($failures -ge $MaxQuickFailures) { Write-Log "giving up after $failures failed starts in a row; check logs/latest.log"; exit 2 }
    Write-Log "crash $failures of $MaxQuickFailures; restarting in $backoff seconds"
    Start-Sleep -Seconds $backoff
    $backoff = [Math]::Min($backoff * 2, $BackoffMaxSeconds)
}
