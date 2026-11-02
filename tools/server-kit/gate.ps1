<#
Version gate. Refuses a missing, duplicate, wrong or unpinned Graves and Glory jar.
  .\gate.ps1          check mods\ against mod-lock.json (exit 1 on mismatch)
  .\gate.ps1 -Pin     record the jar now in mods\ as the approved build (a human decision)
  -Root DIR           use DIR instead of this script's folder
  -DryRun             with -Pin: report, write nothing
#>
param([switch]$Pin, [string]$Root, [switch]$DryRun)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib.ps1')
try {
    Initialize-Kit $PSScriptRoot $Root ([bool]$DryRun)
    if ($Pin) {
        $info = Get-JarInfo (Find-ModJar)
        Write-Kit "Jar $(Split-Path -Leaf $info.Path): version $($info.Version), build $($info.Build)"
        Set-Pin $info
    } else {
        [void](Test-Gate)
    }
} catch { Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red; exit 1 }
