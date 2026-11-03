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
    [string]$JavaExe = 'java'
)
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
    if ($code -eq 0) { Write-Log 'clean stop; supervisor ending.'; break }

    if ($ranSeconds -ge $HealthyAfterSeconds) { $failures = 0; $backoff = $BackoffStartSeconds }
    $failures++
    if ($failures -ge $MaxQuickFailures) { Write-Log "giving up after $failures failed starts in a row; check logs/latest.log"; exit 2 }
    Write-Log "crash $failures of $MaxQuickFailures; restarting in $backoff seconds"
    Start-Sleep -Seconds $backoff
    $backoff = [Math]::Min($backoff * 2, $BackoffMaxSeconds)
}
