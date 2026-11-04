<#
Download the official Fabric server launcher for Minecraft 1.20.1 (Fabric Loader 0.16.14) and check its SHA-256.
Does not start the server and accepts no terms (the Minecraft EULA is yours to read: start.ps1 explains).
  .\setup.ps1 [-DryRun] [-Root DIR]
Refuses to replace a launcher that is already there.
#>
param([string]$Root, [switch]$DryRun)
$ErrorActionPreference = 'Stop'
$kit = if ($Root) { (Resolve-Path -LiteralPath $Root).Path } else { $PSScriptRoot }
$destination = Join-Path $kit 'fabric-server-launch.jar'
if (Test-Path -LiteralPath $destination) { throw 'Launcher already exists; preserve it or move it aside before setup.' }
if ($DryRun) { Write-Output "[dry-run] would download the Fabric 0.16.14 launcher to $destination and check its SHA-256"; exit 0 }
$staging = Join-Path $kit 'fabric-server-launch.jar.download'
Invoke-WebRequest -Uri 'https://meta.fabricmc.net/v2/versions/loader/1.20.1/0.16.14/1.1.2/server/jar' -OutFile $staging
if ((Get-FileHash -LiteralPath $staging -Algorithm SHA256).Hash.ToLowerInvariant() -ne '856a5f9fe4809ac1972778ff5bac71f8995b7ae0513caa6d0be94288b92cf113') { Remove-Item -LiteralPath $staging -Force; throw 'Fabric download hash mismatch. Do not run it.' }
Move-Item -LiteralPath $staging -Destination $destination
Write-Output 'Fabric launcher downloaded and verified. Next: .\gate.ps1 -Pin, then .\start.ps1 (see README.md).'
