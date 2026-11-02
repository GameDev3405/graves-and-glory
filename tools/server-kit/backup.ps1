<#
Verified world backup to backups\<world>-<UTC stamp>-<label>.zip. Never overwrites an archive.
  .\backup.ps1 [-Label NAME] [-Keep N] [-AllowRunning] [-DryRun] [-Root DIR]
Labels start and auto are pruned to the newest N (default 20). Any other label is kept forever.
#>
param([ValidatePattern('^[A-Za-z0-9]+$')][string]$Label = 'manual', [int]$Keep = 20, [switch]$AllowRunning, [string]$Root, [switch]$DryRun)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib.ps1')
try {
    Initialize-Kit $PSScriptRoot $Root ([bool]$DryRun)
    [void](New-Backup $Label $Keep ([bool]$AllowRunning))
} catch { Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red; exit 1 }
