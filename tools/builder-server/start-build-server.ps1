# Builder server for Graves and Glory: one saved world per map (and the camp). /map <name> in game switches worlds:
# the mod writes next-world.txt and stops; this loop relaunches the server on that world. Run it in a window or as a scheduled task.
param([ValidateRange(2,32)][int]$MemoryGB = 6)
Set-Location -LiteralPath $PSScriptRoot
$worlds = 'camp','rivergate','ironhold','frostmarch','hollowwood','kingsfield'
if (!(Test-Path -LiteralPath 'fabric-server-launch.jar')) { throw 'fabric-server-launch.jar is missing.' }
$world = 'camp'
if (Test-Path -LiteralPath 'current-world.txt') { $world = (Get-Content -LiteralPath 'current-world.txt' -Raw).Trim() }
while ($true) {
    if ($worlds -notcontains $world) { $world = 'camp' }
    Set-Content -LiteralPath 'current-world.txt' -Value $world -NoNewline
    $lines = Get-Content -LiteralPath 'server.properties' | ForEach-Object { if ($_ -like 'level-name=*') { "level-name=gg-build-$world" } else { $_ } }
    Set-Content -LiteralPath 'server.properties' -Value $lines -Encoding ascii
    Write-Output "[builder] starting world: $world"
    & java '-Xms1G' "-Xmx${MemoryGB}G" "-Dgg.build=$world" -jar 'fabric-server-launch.jar' nogui
    if (Test-Path -LiteralPath 'next-world.txt') {
        $world = (Get-Content -LiteralPath 'next-world.txt' -Raw).Trim()
        Remove-Item -LiteralPath 'next-world.txt'
        continue
    }
    if ($LASTEXITCODE -ne 0) { Write-Output "[builder] server exited with $LASTEXITCODE; restarting in 15 seconds"; Start-Sleep 15; continue }
    break
}
