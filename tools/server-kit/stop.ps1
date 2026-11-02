<#
Stop the server cleanly: asks start.ps1 to send "stop" to the server, which saves the world and exits.
Never kills the process.
  .\stop.ps1 [-TimeoutSeconds N] [-DryRun] [-Root DIR]
#>
param([int]$TimeoutSeconds = 120, [string]$Root, [switch]$DryRun)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib.ps1')
try {
    Initialize-Kit $PSScriptRoot $Root ([bool]$DryRun)
    $proc = Get-ServerProcess
    if ($null -eq $proc) {
        Write-Kit 'Server is not running.'
        if ((Test-Path -LiteralPath $script:PidFile) -and !$DryRun) { Remove-Item -LiteralPath $script:PidFile -Force }
        exit 0
    }
    if ($DryRun) { Write-Kit "[dry-run] would write stop.request for java pid $($proc.Id) and wait up to ${TimeoutSeconds}s"; exit 0 }
    Write-Kit "Stopping java pid $($proc.Id) (saving the world)..."
    Set-Content -LiteralPath $script:StopFile -Value (Get-Stamp) -Encoding ascii
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $warned = $false
    while (!$proc.HasExited) {
        if ((Get-Date) -gt $deadline) {
            Remove-Item -LiteralPath $script:StopFile -Force -ErrorAction SilentlyContinue
            throw "Still running after ${TimeoutSeconds}s. Not forcing it. Look at the server console and type stop there; as a last resort end it yourself, then restore from backups\ if the world is damaged."
        }
        if (!$warned -and (Test-Path -LiteralPath $script:StopFile) -and ((Get-Date) -gt $deadline.AddSeconds(-$TimeoutSeconds + 15))) {
            Write-KitWarn 'Nothing has picked up the request. Was the server started with start.ps1? If not, type stop in its console.'
            $warned = $true
        }
        Start-Sleep -Milliseconds 500
    }
    Remove-Item -LiteralPath $script:PidFile -Force -ErrorAction SilentlyContinue
    Write-Kit 'Stopped.'
} catch { Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red; exit 1 }
