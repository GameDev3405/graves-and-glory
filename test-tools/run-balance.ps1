param([string]$Run,[string]$Team0="hard",[string]$Team1="normal",[int]$Port=25931,[int]$Matches=5,[string]$Maps="0,1,2,3,4",[int]$Seconds=300,[int]$TickMs=5,[string]$Cadence="")
$ErrorActionPreference="Stop"
$balanceRepo=Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $balanceRepo
$balanceRoot=Join-Path $balanceRepo ("work/sims/"+$Run)
New-Item -ItemType Directory -Force $balanceRoot | Out-Null
$balanceWorld=Join-Path $balanceRoot "Graves and Glory"
if(!(Test-Path -LiteralPath $balanceWorld)){Copy-Item -LiteralPath "C:/Users/kortt/Documents/Codex/gg-banner05-20261020/work/banner-acceptance/run/saves/Graves and Glory" -Destination $balanceWorld -Recurse}
Set-Content -LiteralPath (Join-Path $balanceRoot "eula.txt") "eula=true"
Set-Content -LiteralPath (Join-Path $balanceRoot "server.properties") @"
server-ip=127.0.0.1
server-port=$Port
online-mode=false
level-name=Graves and Glory
view-distance=6
simulation-distance=10
max-tick-time=180000
enable-rcon=false
spawn-protection=0
"@
@{run=$Run;team0=$Team0;team1=$Team1;maps=$Maps;matches=$Matches;roundSeconds=$Seconds;tickMs=$TickMs;easyCadence=$Cadence;banner0="gilded_normal";banner1="gilded_normal";size=3} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $balanceRoot "RUN.json")
$env:JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot"
$env:GRAVES_ROADMAP_SERVER_ROOT=$balanceRoot.Replace('\','/')
$env:GRAVES_BOTSIM_TICK_MS="$TickMs"
$env:GRAVES_BALANCE_LABEL=$Run
$env:GRAVES_BALANCE_CADENCE=$Cadence
$env:GRAVES_BOTSIM_PROBE="1"
$balanceArgs=@("runServer","--offline","--init-script","test-tools/isolated-server.gradle","-PbotSim=$Matches","-PsimDawn=$Team0","-PsimDusk=$Team1","-PsimMaps=$Maps","-PsimSize=3","-PsimRoundSeconds=$Seconds","-PsimBanner0=gilded_normal","-PsimBanner1=gilded_normal","--console=plain")
& "C:/Users/kortt/.gradle/wrapper/dists/gradle-8.8-bin/dl7vupf4psengwqhwktix4v1/gradle-8.8/bin/gradle.bat" @balanceArgs *> (Join-Path $balanceRoot "server.log")
Write-Output "Run $Run exit $LASTEXITCODE"
Get-Content -LiteralPath (Join-Path $balanceRoot "server.log") -Tail 6
