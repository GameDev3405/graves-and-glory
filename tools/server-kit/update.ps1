<#
Update loop (task 2A.8): stop, back up, swap the mod jar, pin it, optionally restart.
  .\update.ps1 -Jar PATH [-Restart] [-MemoryGB N] [-DryRun] [-Root DIR]
The new jar is checked first (mod id, Minecraft 1.20.1, build record). The old jar is moved to
mods-retired\, never deleted. Running this script is the human approval that re-pins mod-lock.json.
After a restart, join 25580 yourself as the smoke test. Maps and structures live in the world,
so a map change is a restore from a world archive (.\restore.ps1), not part of this script.
#>
param([Parameter(Mandatory = $true)][string]$Jar, [switch]$Restart, [ValidateRange(2, 64)][int]$MemoryGB = 6, [string]$Root, [switch]$DryRun)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib.ps1')
try {
    Initialize-Kit $PSScriptRoot $Root ([bool]$DryRun)
    Set-Location -LiteralPath $script:Root
    $new = Get-JarInfo $Jar
    Write-Kit "New jar OK: $(Split-Path -Leaf $new.Path) version $($new.Version) build $(Get-Short $new.Build)"
    New-Item -ItemType Directory -Force -Path 'mods' | Out-Null
    $oldFiles = @(Get-ChildItem -LiteralPath 'mods' -File | Where-Object { $_.Name -like 'graves-and-glory*.jar' -or $_.Name -like 'gravesandglory*.jar' -or $_.Name -like 'bannerbound*.jar' })
    if ($oldFiles.Count -gt 1) { throw 'More than one Graves and Glory jar in mods\; fix by hand first.' }
    $old = $null
    if ($oldFiles.Count -eq 1) {
        $old = $oldFiles[0]
        if ((Get-FileHash -LiteralPath $old.FullName -Algorithm SHA256).Hash.ToLowerInvariant() -eq $new.Sha) { Write-Kit 'mods\ already holds this exact jar. Nothing to do.'; exit 0 }
    }

    if (Get-ServerProcess) {
        if ($DryRun) { Write-Kit '[dry-run] would run .\stop.ps1' }
        else { & (Join-Path $PSScriptRoot 'stop.ps1') -Root $script:Root; if ($LASTEXITCODE -ne 0) { throw 'Could not stop the server; nothing was changed.' } }
    }
    # The server is stopped, so a missing or failed backup aborts before any file moves.
    [void](New-Backup 'preupdate' 20 ([bool]$DryRun))
    if ($DryRun) {
        $oldName = 'no old jar'
        if ($old) { $oldName = $old.Name }
        Write-Kit "[dry-run] would move $oldName to mods-retired\, copy $(Split-Path -Leaf $new.Path) into mods\, pin it"
        if ($Restart) { Write-Kit "[dry-run] would restart with .\start.ps1 -MemoryGB $MemoryGB" }
        Write-Kit '[dry-run] nothing was changed.'
        exit 0
    }

    New-Item -ItemType Directory -Force -Path 'mods-retired' | Out-Null
    $retired = $null
    if ($old) {
        $retired = Join-Path 'mods-retired' "$($old.Name).$(Get-Stamp)"
        Move-Item -LiteralPath $old.FullName -Destination $retired
    }
    $dest = Join-Path 'mods' "graves-and-glory-$($new.Version).jar"
    try {
        Copy-Item -LiteralPath $new.Path -Destination "$dest.tmp" -Force
        Move-Item -LiteralPath "$dest.tmp" -Destination $dest -Force
    } catch {
        Remove-Item -LiteralPath "$dest.tmp" -Force -ErrorAction SilentlyContinue
        if ($retired) { Move-Item -LiteralPath $retired -Destination $old.FullName }
        throw "Copy failed; the old jar was put back. $($_.Exception.Message)"
    }
    Set-Pin (Get-JarInfo $dest)
    [void](Test-Gate)
    $kept = '<none>'
    if ($retired) { $kept = $retired }
    Write-Kit "Updated to $($new.Version). Old jar kept at $kept."
    if ($Restart) {
        New-Item -ItemType Directory -Force -Path 'logs' | Out-Null
        $shell = (Get-Process -Id $PID).Path
        Start-Process -FilePath $shell -WindowStyle Minimized -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $PSScriptRoot 'start.ps1'), '-Root', $script:Root, '-MemoryGB', $MemoryGB)
        Write-Kit 'Restart launched in a minimized window. Now join 25580 as the smoke test.'
    } else {
        Write-Kit 'Start it with .\start.ps1, then join 25580 as the smoke test.'
    }
} catch { Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red; exit 1 }
