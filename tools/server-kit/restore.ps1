<#
Restore the world from a backup archive made by backup.ps1 (.zip).
  .\restore.ps1 -Backup backups\<file>.zip [-DryRun] [-Root DIR]
The server must be stopped. The current world is backed up first (label prerestore), then moved aside
to <world>.replaced-<stamp>. Nothing is deleted or overwritten.
#>
param([Parameter(Mandatory = $true)][string]$Backup, [string]$Root, [switch]$DryRun)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib.ps1')
try {
    Initialize-Kit $PSScriptRoot $Root ([bool]$DryRun)
    Set-Location -LiteralPath $script:Root
    if (!(Test-Path -LiteralPath $Backup -PathType Leaf)) { throw "Archive not found: $Backup" }
    $archive = (Resolve-Path -LiteralPath $Backup).ProviderPath
    if ($archive -notlike '*.zip') { throw 'Only .zip archives (from backup.ps1) can be restored here. For .tar.gz use restore.sh.' }
    $world = Get-WorldName
    if (Get-ServerProcess) { throw 'Server is running. Stop it first (.\stop.ps1).' }
    $zip = [System.IO.Compression.ZipFile]::OpenRead($archive)
    try { $names = @($zip.Entries | ForEach-Object { $_.FullName.Replace('\', '/') }) } finally { $zip.Dispose() }
    if ($names -notcontains "$world/level.dat") { throw "Archive does not contain '$world/level.dat'. Wrong world name or wrong archive." }
    [void](New-Backup 'prerestore')
    if ($DryRun) { Write-Kit "[dry-run] would extract $archive, move '$world' to $world.replaced-<stamp>, install the restored world"; exit 0 }
    $temp = ".restore-tmp-$PID"
    if (Test-Path -LiteralPath $temp) { throw "Leftover $temp exists; remove it by hand." }
    New-Item -ItemType Directory -Path $temp | Out-Null
    try {
        [System.IO.Compression.ZipFile]::ExtractToDirectory($archive, (Join-Path $script:Root $temp))
        if (!(Test-Path -LiteralPath (Join-Path (Join-Path $temp $world) 'level.dat'))) { throw 'Extracted world has no level.dat.' }
    } catch {
        Remove-Item -LiteralPath $temp -Recurse -Force -ErrorAction SilentlyContinue
        throw "Extraction failed; the world is untouched. $($_.Exception.Message)"
    }
    $aside = $null
    if (Test-Path -LiteralPath $world) { $aside = "$world.replaced-$(Get-Stamp)"; Move-Item -LiteralPath $world -Destination $aside }
    try { Move-Item -LiteralPath (Join-Path $temp $world) -Destination $world }
    catch { if ($aside) { Move-Item -LiteralPath $aside -Destination $world }; throw "Install failed; the old world was put back. $($_.Exception.Message)" }
    Remove-Item -LiteralPath $temp -Recurse -Force
    $kept = '<none>'
    if ($aside) { $kept = $aside }
    Write-Kit "Restored '$world' from $archive. Previous world kept at $kept and in the prerestore backup."
} catch { Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red; exit 1 }
