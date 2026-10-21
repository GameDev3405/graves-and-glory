param([string]$Run="visual")
$ErrorActionPreference="Stop"
$balanceRepo=Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $balanceRepo
$balanceRoot=Join-Path $balanceRepo ("work/"+$Run)
New-Item -ItemType Directory -Force ($balanceRoot+"/run"),($balanceRoot+"/evidence"),($balanceRoot+"/runtime") | Out-Null
if(!(Test-Path -LiteralPath ($balanceRoot+"/run/saves"))){Copy-Item -LiteralPath "C:/Users/kortt/Documents/Codex/gg-banner05-20261020/work/banner-acceptance/run/saves" -Destination ($balanceRoot+"/run/saves") -Recurse}
Set-Content -LiteralPath ($balanceRoot+"/run/options.txt") "pauseOnLostFocus:false`nonboardAccessibility:false"
$env:JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot"
$env:GRAVES_ROADMAP_TEST_ROOT=$balanceRoot.Replace('\','/')
$env:GRAVES_PHASE1_CHECKS="bots-visual"
$env:GRAVES_PHASE2_CHECKS=""
& "C:/Users/kortt/.gradle/wrapper/dists/gradle-8.8-bin/dl7vupf4psengwqhwktix4v1/gradle-8.8/bin/gradle.bat" compileLocalTestTools --offline --console=plain *> ($balanceRoot+"/compile.log")
if($LASTEXITCODE -ne 0){Get-Content ($balanceRoot+"/compile.log") -Tail 25;exit $LASTEXITCODE}
& "C:/Users/kortt/.gradle/wrapper/dists/gradle-8.8-bin/dl7vupf4psengwqhwktix4v1/gradle-8.8/bin/gradle.bat" runClient --offline -PcampaignChecks --init-script test-tools/isolated-checks.gradle --args="--gameDir $($balanceRoot.Replace('\','/'))/run" --console=plain *> ($balanceRoot+"/client.log")
Get-Content ($balanceRoot+"/client.log") -Tail 5
