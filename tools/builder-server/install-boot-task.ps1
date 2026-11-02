# Registers the GGBuilderServer scheduled task: runs start-build-server.ps1 at machine boot as SYSTEM, hidden, and
# restarts the supervisor itself if it ever dies. Needs an elevated PowerShell. Use -WhatIf to see what it would do.
# This changes system scheduling, so the owner runs it; nothing else in the repo calls it.
[CmdletBinding(SupportsShouldProcess)]
param(
    [string]$ServerDir = $PSScriptRoot,
    [string]$TaskName = 'GGBuilderServer',
    [int]$MemoryGB = 6
)
$script = Join-Path $ServerDir 'start-build-server.ps1'
if (!(Test-Path -LiteralPath $script)) { throw "Missing $script" }
$action = New-ScheduledTaskAction -Execute 'powershell.exe' -WorkingDirectory $ServerDir `
    -Argument "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$script`" -MemoryGB $MemoryGB"
$trigger = New-ScheduledTaskTrigger -AtStartup
$settings = New-ScheduledTaskSettingsSet -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 5) `
    -ExecutionTimeLimit ([TimeSpan]::Zero) -StartWhenAvailable -MultipleInstances IgnoreNew
$principal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest
if ($PSCmdlet.ShouldProcess($TaskName, "Register boot task for $script")) {
    Register-ScheduledTask -TaskName $TaskName -Action $action -Trigger $trigger -Settings $settings -Principal $principal -Force | Out-Null
    Write-Output "Registered $TaskName. Start it now with: Start-ScheduledTask -TaskName $TaskName"
}
