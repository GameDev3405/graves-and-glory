<#
Start the Graves and Glory dedicated server (port 25580). Stays in the foreground and supervises the java process.
  .\start.ps1 [-MemoryGB N] [-DryRun] [-Root DIR]
Order: Java check, version gate, config, EULA check, world backup, launch with Aikar flags.
Console input is passed to the server (type stop to shut down). .\stop.ps1 asks this script to stop it cleanly.
-DryRun runs every check and prints the java command; it writes nothing and starts nothing.
#>
param([ValidateRange(2, 64)][int]$MemoryGB = 6, [string]$Root, [switch]$DryRun)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib.ps1')
try {
    Initialize-Kit $PSScriptRoot $Root ([bool]$DryRun)
    Set-Location -LiteralPath $script:Root

    if (!(Test-Path -LiteralPath 'fabric-server-launch.jar')) { throw 'fabric-server-launch.jar is missing. See README.md, step 3.' }
    $java = Get-Command java -ErrorAction SilentlyContinue
    if ($null -eq $java) { throw 'java not found. Install Java 17 or newer (README.md, step 1).' }
    $versionLine = (cmd /c 'java -version 2>&1') | Select-Object -First 1
    if ("$versionLine" -notmatch '"(\d+)[."]') { throw 'Could not read the Java version.' }
    if ([int]$Matches[1] -lt 17) { throw "Java $($Matches[1]) is too old; need 17 or newer." }
    if (Get-ServerProcess) { throw "Server already running (pid $((Get-Content -LiteralPath 'server.pid' -Raw).Trim()))." }

    [void](Test-Gate)

    if (!(Test-Path -LiteralPath 'server.properties')) {
        if (!(Test-Path -LiteralPath 'server.properties.default')) { throw 'server.properties.default is missing from the kit.' }
        if ($DryRun) { Write-Kit '[dry-run] would create server.properties from server.properties.default' }
        else { Copy-Item -LiteralPath 'server.properties.default' -Destination 'server.properties'; Write-Kit 'Created server.properties from the kit default.' }
    }
    $port = Get-Prop 'server-port' ''
    if ($port -eq '' -and $DryRun) { $port = "$($script:KitPort)" }
    if ($port -ne "$($script:KitPort)") { Write-KitWarn "server-port is '$port', not $($script:KitPort)." }

    if (!(Test-Path -LiteralPath 'whitelist.json')) {
        if ($DryRun) { Write-Kit '[dry-run] would create whitelist.json from whitelist.template.json' }
        else { Copy-Item -LiteralPath 'whitelist.template.json' -Destination 'whitelist.json'; Write-Kit 'Created empty whitelist.json. Add players: whitelist add <name> in the server console.' }
    }

    # EULA: a human accepts it by editing eula.txt. This script never writes eula=true.
    if (!(Test-Path -LiteralPath 'eula.txt')) {
        if ($DryRun) { Write-Kit '[dry-run] would create eula.txt with eula=false' }
        else {
            Set-Content -LiteralPath 'eula.txt' -Encoding ascii -Value @('# Read https://aka.ms/MinecraftEULA. If you agree, change the next line to eula=true yourself.', 'eula=false')
            Write-Kit 'Created eula.txt with eula=false.'
        }
    }
    $accepted = (Test-Path -LiteralPath 'eula.txt') -and (Select-String -LiteralPath 'eula.txt' -Pattern '^eula=true\s*$' -Quiet)
    if (!$accepted) {
        if ($DryRun) { Write-Kit '[dry-run] EULA not accepted; a real start would stop here until a person sets eula=true in eula.txt.' }
        else { throw 'EULA not accepted. Read https://aka.ms/MinecraftEULA and, if you agree, set eula=true in eula.txt yourself. This kit never does it for you.' }
    }

    [void](New-Backup 'start')

    $javaArgs = @("-Xms${MemoryGB}G", "-Xmx${MemoryGB}G") + (Get-AikarFlags $MemoryGB) + @('-jar', 'fabric-server-launch.jar', '--nogui')
    if ($DryRun) {
        Write-Kit "[dry-run] command: java $($javaArgs -join ' ')"
        Write-Kit '[dry-run] checks complete; nothing was started.'
        exit 0
    }

    if (Test-Path -LiteralPath $script:StopFile) { Remove-Item -LiteralPath $script:StopFile -Force }
    $info = New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName = $java.Source
    $info.Arguments = $javaArgs -join ' '
    $info.WorkingDirectory = $script:Root
    $info.UseShellExecute = $false
    $info.RedirectStandardInput = $true   # stdout and stderr stay on this console
    $proc = [System.Diagnostics.Process]::Start($info)
    Set-Content -LiteralPath $script:PidFile -Value $proc.Id -Encoding ascii
    Write-Kit "Started on port $port with ${MemoryGB}G heap. java pid $($proc.Id). Type stop here, or run .\stop.ps1, to shut down."

    $reader = $null
    try { $reader = [Console]::In.ReadLineAsync() } catch { $reader = $null }
    $stopSent = $false
    while (!$proc.HasExited) {
        if ($null -ne $reader -and $reader.IsCompleted) {
            $line = $null
            try { $line = $reader.Result } catch { $line = $null }
            if ($null -eq $line) { $reader = $null }
            else {
                try { $proc.StandardInput.WriteLine($line); $proc.StandardInput.Flush() } catch { }
                try { $reader = [Console]::In.ReadLineAsync() } catch { $reader = $null }
            }
        }
        if (!$stopSent -and (Test-Path -LiteralPath $script:StopFile)) {
            Remove-Item -LiteralPath $script:StopFile -Force -ErrorAction SilentlyContinue
            Write-Kit 'Stop requested; sending "stop" to the server.'
            try { $proc.StandardInput.WriteLine('stop'); $proc.StandardInput.Flush(); $stopSent = $true } catch { }
        }
        Start-Sleep -Milliseconds 300
    }
    Remove-Item -LiteralPath $script:PidFile -Force -ErrorAction SilentlyContinue
    Write-Kit "Server exited with code $($proc.ExitCode)."
    exit $proc.ExitCode
} catch { Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red; exit 1 }
