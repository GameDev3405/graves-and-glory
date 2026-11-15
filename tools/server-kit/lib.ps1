# Shared helpers for the Graves and Glory server kit. Dot-sourced by the other scripts; not run directly.
# Works in Windows PowerShell 5.1 and PowerShell 7. Needs Java 21+ only for start.ps1.

$script:KitMc = '1.21.1'
$script:KitModId = 'gravesandglory'
$script:KitPort = 25580
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Write-Kit([string]$Message) { Write-Host "[server-kit] $Message" }
function Write-KitWarn([string]$Message) { Write-Host "WARNING: $Message" -ForegroundColor Yellow }

function Initialize-Kit([string]$ScriptDir, [string]$RootOverride, [bool]$Dry) {
    $target = $ScriptDir
    if ($RootOverride) { $target = $RootOverride }
    if (!(Test-Path -LiteralPath $target -PathType Container)) { throw "Server directory not found: $target" }
    $script:Root = (Resolve-Path -LiteralPath $target).ProviderPath
    $script:Dry = $Dry
    $script:PropsFile = Join-Path $script:Root 'server.properties'
    $script:LockFile = Join-Path $script:Root 'mod-lock.json'
    $script:PidFile = Join-Path $script:Root 'server.pid'
    $script:StopFile = Join-Path $script:Root 'stop.request'
    $script:BackupDir = Join-Path $script:Root 'backups'
}

function Get-Short([string]$Hash) { return $Hash.Substring(0, [Math]::Min(12, $Hash.Length)) }

function Get-Prop([string]$Key, [string]$Default = '') {
    if (!(Test-Path -LiteralPath $script:PropsFile)) { return $Default }
    $hit = Get-Content -LiteralPath $script:PropsFile | Where-Object { $_ -like "$Key=*" } | Select-Object -Last 1
    if ($null -eq $hit) { return $Default }
    $value = $hit.Substring($Key.Length + 1).Trim()
    if ($value -eq '') { return $Default }
    return $value
}

function Get-WorldName { return (Get-Prop 'level-name' 'Graves and Glory') }

function Get-ServerProcess {
    if (!(Test-Path -LiteralPath $script:PidFile)) { return $null }
    $text = (Get-Content -LiteralPath $script:PidFile -Raw).Trim()
    $id = 0
    if (![int]::TryParse($text, [ref]$id)) { return $null }
    $proc = Get-Process -Id $id -ErrorAction SilentlyContinue
    if ($null -eq $proc -or $proc.ProcessName -notlike 'java*') { return $null }
    return $proc
}

function Read-JarEntry([string]$Jar, [string]$Entry) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($Jar)
    try {
        $item = $zip.GetEntry($Entry)
        if ($null -eq $item) { return $null }
        $reader = New-Object System.IO.StreamReader($item.Open())
        try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
    } finally { $zip.Dispose() }
}

# Check the jar itself (mod id, Minecraft version, build record) and return its identity.
function Get-JarInfo([string]$Jar) {
    if (!(Test-Path -LiteralPath $Jar -PathType Leaf)) { throw "Mod jar not found: $Jar" }
    $Jar = (Resolve-Path -LiteralPath $Jar).ProviderPath   # .NET resolves relative paths against the process directory, not the PowerShell location
    try { $metaText = Read-JarEntry $Jar 'fabric.mod.json' } catch { throw "$Jar is not a readable jar file." }
    if (!$metaText) { throw "$Jar has no fabric.mod.json; it is not a Fabric mod jar." }
    $meta = $metaText | ConvertFrom-Json
    if ($meta.id -ne $script:KitModId) { throw "$Jar is mod id '$($meta.id)', expected '$($script:KitModId)'." }
    if ("$($meta.version)" -notmatch '^\d+\.\d+\.\d+') { throw "$Jar has an unusable version '$($meta.version)'." }
    if ("$($meta.depends.minecraft)" -ne $script:KitMc) { throw "$Jar targets Minecraft '$($meta.depends.minecraft)'; this kit runs $($script:KitMc)." }
    $buildText = Read-JarEntry $Jar 'gravesandglory-build.json'
    if (!$buildText) { throw "$Jar has no gravesandglory-build.json (not a release build); rebuild with gradlew build." }
    $build = $buildText | ConvertFrom-Json
    if ("$($build.sourceSha256)" -notmatch '^[0-9a-f]{64}$') { throw "$Jar has an invalid build identity." }
    if ("$($build.version)" -ne "$($meta.version)") { throw "$Jar build record version differs from its mod version; rebuild." }
    return [pscustomobject]@{
        Path = (Resolve-Path -LiteralPath $Jar).ProviderPath
        Version = "$($meta.version)"
        Build = "$($build.sourceSha256)"
        Sha = (Get-FileHash -LiteralPath $Jar -Algorithm SHA256).Hash.ToLowerInvariant()
    }
}

# The single runtime jar in mods/, or a refusal.
function Find-ModJar {
    $mods = Join-Path $script:Root 'mods'
    if (!(Test-Path -LiteralPath $mods -PathType Container)) { throw "mods/ is missing in $($script:Root)." }
    $found = @()
    foreach ($file in Get-ChildItem -LiteralPath $mods -File) {
        $name = $file.Name
        if ($name -like '*DO-NOT-SHIP*') { throw "Refusing test-tool jar in mods/: $name" }
        if ($name -like 'graves-and-glory*-sources.jar' -or $name -like 'graves-and-glory*-dev.jar') { throw "Refusing non-runtime jar in mods/: $name" }
        if ($name -like 'graves-and-glory*.jar' -or $name -like 'gravesandglory*.jar' -or $name -like 'bannerbound*.jar') { $found += $file }
    }
    if ($found.Count -lt 1) { throw 'No Graves and Glory jar in mods/. Copy graves-and-glory-<version>.jar there.' }
    if ($found.Count -gt 1) { throw "$($found.Count) Graves and Glory jars in mods/. Keep exactly one; move old ones out." }
    return $found[0].FullName
}

# Record the inspected jar as the approved one. A human decision: only gate.ps1 -Pin and update.ps1 call it.
function Set-Pin($Info) {
    if ($script:Dry) { Write-Kit "[dry-run] would write mod-lock.json for $($Info.Version) build $(Get-Short $Info.Build)"; return }
    if (Test-Path -LiteralPath $script:LockFile) { Copy-Item -LiteralPath $script:LockFile -Destination "$($script:LockFile).prev" -Force }
    $lock = [ordered]@{ modId = $script:KitModId; minecraft = $script:KitMc; version = $Info.Version; sourceSha256 = $Info.Build; jarSha256 = $Info.Sha }
    $temp = "$($script:LockFile).tmp"
    ($lock | ConvertTo-Json) | Set-Content -LiteralPath $temp -Encoding ascii
    Move-Item -LiteralPath $temp -Destination $script:LockFile -Force
    Write-Kit "Pinned mod $($Info.Version) (build $(Get-Short $Info.Build))."
}

# Refuse a missing, duplicate, wrong, or unpinned mod jar.
function Test-Gate {
    $jar = Find-ModJar
    $info = Get-JarInfo $jar
    if (!(Test-Path -LiteralPath $script:LockFile)) { throw "No mod-lock.json. After confirming $(Split-Path -Leaf $jar) is the approved build, run: .\gate.ps1 -Pin" }
    $lock = Get-Content -LiteralPath $script:LockFile -Raw | ConvertFrom-Json
    if ($lock.version -ne $info.Version) { throw "Version gate: jar is $($info.Version) but the lock says $($lock.version). Refusing." }
    if ($lock.sourceSha256 -ne $info.Build) { throw "Version gate: jar build $(Get-Short $info.Build) differs from the locked build $(Get-Short "$($lock.sourceSha256)"). Refusing." }
    if ($lock.jarSha256 -ne $info.Sha) { throw 'Version gate: jar bytes differ from the locked file (rebuilt or altered). Refusing.' }
    Write-Kit "Version gate OK: $(Split-Path -Leaf $jar), version $($info.Version), build $(Get-Short $info.Build), Minecraft $($script:KitMc)."
    return $info
}

function Get-Stamp { return (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssZ') }

# Verified zip of the world to backups\<world>-<UTC stamp>-<label>.zip. Never overwrites an archive. Returns the path or $null.
function New-Backup([string]$Label, [int]$Keep = 20, [bool]$AllowRunning = $false) {
    $world = Get-WorldName
    $worldDir = Join-Path $script:Root $world
    if (!(Test-Path -LiteralPath $worldDir -PathType Container)) { Write-Kit "No world '$world' yet; nothing to back up."; return $null }
    if ((Get-ServerProcess) -and !$AllowRunning) { throw 'Server is running. Stop it first (.\stop.ps1), or pass -AllowRunning to accept a possibly inconsistent copy.' }
    $safe = ($world -replace '[^A-Za-z0-9._-]', '_')
    $out = Join-Path $script:BackupDir "$safe-$(Get-Stamp)-$Label.zip"
    if (Test-Path -LiteralPath $out) { throw "Backup already exists, refusing to overwrite: $out" }
    if ($script:Dry) { Write-Kit "[dry-run] would archive '$world' to $out"; return $out }
    New-Item -ItemType Directory -Force -Path $script:BackupDir | Out-Null
    $partial = "$out.partial"
    try {
        [System.IO.Compression.ZipFile]::CreateFromDirectory($worldDir, $partial, [System.IO.Compression.CompressionLevel]::Optimal, $true)
        $zip = [System.IO.Compression.ZipFile]::OpenRead($partial)
        try { $names = @($zip.Entries | ForEach-Object { $_.FullName.Replace('\', '/') }) } finally { $zip.Dispose() }
        if ($names -notcontains "$world/level.dat") { throw 'Backup has no level.dat; refusing to trust it.' }
    } catch {
        Remove-Item -LiteralPath $partial -Force -ErrorAction SilentlyContinue
        throw "Backup failed, nothing was changed: $($_.Exception.Message)"
    }
    Move-Item -LiteralPath $partial -Destination $out
    Write-Kit "Backup written and verified: $out"
    if ($Label -eq 'start' -or $Label -eq 'auto') {
        # Prune only start/auto archives beyond the newest $Keep. Other labels are kept forever.
        $old = Get-ChildItem -LiteralPath $script:BackupDir -Filter "*-$Label.zip" | Sort-Object Name -Descending | Select-Object -Skip $Keep
        foreach ($file in $old) { Remove-Item -LiteralPath $file.FullName; Write-Kit "Pruned old $Label backup: $($file.Name)" }
    }
    return $out
}

# Aikar's G1 flags; the large-heap variant from 12 GB up.
# Major Java version from the output of `java -version 2>&1`. The first line is not always the version line: a
# JAVA_TOOL_OPTIONS or JDK_JAVA_OPTIONS banner ("Picked up JAVA_TOOL_OPTIONS: ...") comes first. Take the line that
# has `version "N"` (or `"1.8.0"`) in it. Returns 0 when no line matches.
function Get-JavaMajor([object[]]$Lines) {
    foreach ($l in $Lines) {
        if ("$l" -match 'version\s+"(\d+)[."]') { return [int]$Matches[1] }
    }
    return 0
}

# Extra JVM flags for the server: jvm-args.txt in the server folder (one flag per line, # starts a comment) first,
# then the -JvmArgs text of start.ps1 (flags separated by spaces). Every flag must start with "-".
function Get-JvmArgs([string]$Inline) {
    $found = @()
    $file = Join-Path $script:Root 'jvm-args.txt'
    if (Test-Path -LiteralPath $file) {
        foreach ($l in (Get-Content -LiteralPath $file)) {
            $t = ($l -replace '#.*$', '').Trim()
            if ($t -ne '') { $found += $t }
        }
    }
    if ($Inline) { $found += @($Inline -split '\s+' | Where-Object { $_ -ne '' }) }
    foreach ($f in $found) { if (!$f.StartsWith('-')) { throw "JVM flag '$f' must start with '-' (jvm-args.txt or -JvmArgs)." } }
    return @($found | ForEach-Object { if ($_ -match '\s') { '"' + $_ + '"' } else { $_ } })
}

function Get-AikarFlags([int]$MemoryGB) {
    if ($MemoryGB -ge 12) { $new = 40; $max = 50; $region = '16M'; $reserve = 15; $ihop = 20 }
    else { $new = 30; $max = 40; $region = '8M'; $reserve = 20; $ihop = 15 }
    return @(
        '-XX:+UseG1GC', '-XX:+ParallelRefProcEnabled', '-XX:MaxGCPauseMillis=200', '-XX:+UnlockExperimentalVMOptions',
        '-XX:+DisableExplicitGC', '-XX:+AlwaysPreTouch', "-XX:G1NewSizePercent=$new", "-XX:G1MaxNewSizePercent=$max",
        "-XX:G1HeapRegionSize=$region", "-XX:G1ReservePercent=$reserve", '-XX:G1HeapWastePercent=5', '-XX:G1MixedGCCountTarget=4',
        "-XX:InitiatingHeapOccupancyPercent=$ihop", '-XX:G1MixedGCLiveThresholdPercent=90', '-XX:G1RSetUpdatingPauseTimePercent=5',
        '-XX:SurvivorRatio=32', '-XX:+PerfDisableSharedMem', '-XX:MaxTenuringThreshold=1',
        '-Dusing.aikars.flags=https://mcflags.emc.gg', '-Daikars.new.flags=true')
}
