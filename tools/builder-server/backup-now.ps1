# Manual backup of the current world. With the server running the zip is a best-effort "live" copy
# (use /backup in game for a guaranteed-consistent one). -Offsite also copies to the offsite folder.
param([string]$Python = 'python', [switch]$Offsite)
$ops = Join-Path $PSScriptRoot 'builder_ops.py'
& $Python $ops --server-dir $PSScriptRoot backup --reason manual
if ($Offsite) { & $Python $ops --server-dir $PSScriptRoot offsite }
