"""Prepare a headless-only server kit. No installer execution or EULA acceptance."""
import hashlib
import json
import zipfile
from release_config import ROOT, version, source_identity, validate_player_jar

FABRIC_URL='https://meta.fabricmc.net/v2/versions/loader/1.20.1/0.16.14/1.1.2/server/jar'
FABRIC_SHA256='856a5f9fe4809ac1972778ff5bac71f8995b7ae0513caa6d0be94288b92cf113'

def export():
    release=version();jar=ROOT/f'build/libs/graves-and-glory-{release}.jar'
    validate_player_jar(jar,source_identity())
    rows=json.loads((ROOT/'dependencies/platform-lock.json').read_text(encoding='utf8'))
    files={'mods/'+jar.name:jar.read_bytes()}
    for row in rows:
        if row['env']['server']!='required':continue
        data=(ROOT/row['localPath']).read_bytes()
        if hashlib.sha512(data).hexdigest()!=row['hashes']['sha512']:raise RuntimeError('Stale server dependency')
        files['mods/'+row['fileName']]=data
    manifest=json.loads((ROOT/'dist/package-manifest.json').read_text())
    player=ROOT/'dist'/manifest['fileName']
    if hashlib.sha256(player.read_bytes()).hexdigest()!=manifest['sha256']:raise RuntimeError('Player archive changed')
    with zipfile.ZipFile(player) as pack:
        for name in pack.namelist():
            if name.startswith('docs/'):files[name]=pack.read(name)
    with zipfile.ZipFile(ROOT/f'dist/graves-and-glory-{release}.mrpack') as pack:
        files['server.properties']=pack.read('server-overrides/server.properties')
    files['setup-server.ps1']=(f'''$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$destination = Join-Path $PSScriptRoot 'fabric-server-launch.jar'
if (Test-Path -LiteralPath $destination) {{ throw 'Launcher already exists; preserve it or move it aside before setup.' }}
$staging = Join-Path $PSScriptRoot 'fabric-server-launch.jar.download'
Invoke-WebRequest -Uri '{FABRIC_URL}' -OutFile $staging
if ((Get-FileHash -LiteralPath $staging -Algorithm SHA256).Hash.ToLowerInvariant() -ne '{FABRIC_SHA256}') {{ throw 'Fabric download hash mismatch. Do not run it.' }}
Move-Item -LiteralPath $staging -Destination $destination
Write-Output 'Fabric launcher downloaded and verified. Review SERVER-README.txt before starting.'
''').encode()
    files['start-server.ps1']=b'''param([ValidateRange(2,32)][int]$MemoryGB = 8)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (!(Test-Path -LiteralPath 'fabric-server-launch.jar')) { throw 'Run setup-server.ps1 first.' }
if (!(Test-Path -LiteralPath 'eula.txt') -or !(Select-String -LiteralPath 'eula.txt' -Pattern '^eula=true$' -Quiet)) { throw 'Review the Minecraft server EULA yourself, then supply eula.txt if you agree. See SERVER-README.txt.' }
& java '-Xms2G' "-Xmx${MemoryGB}G" -jar 'fabric-server-launch.jar' nogui
exit $LASTEXITCODE
'''
    files['SERVER-README.txt']=f'''Graves and Glory {release} - local dedicated-server candidate

Requires Minecraft Java Edition, Java 17+ (Java 21 used in verification), and
Fabric Loader 0.16.14 for Minecraft 1.20.1. This kit contains the original game
mod and four server dependencies. Rendering, shader and animation mods are absent.
Third-party notices/sources are in docs/. Minecraft/Java/Fabric launcher binaries,
personal saves, profiles, credentials and EULA acceptance are not included.

WINDOWS SETUP
1. Extract into a new dedicated folder. Keep your existing server/saves backed up.
2. Run setup-server.ps1. It downloads the official pinned Fabric launcher and
   checks SHA-256. It does not start the server or accept any terms.
3. Read https://aka.ms/MinecraftEULA yourself. If you agree, supply eula.txt
   containing eula=true. The project never supplies your agreement for you.
4. Run start-server.ps1 (default maximum heap 8 GB, initial heap 2 GB).
   Do not allocate all 96 GB to one process. Measure tick time before scaling.
   The first start downloads Minecraft and Fabric libraries from their providers.
5. Wait for world preparation. The first battlefield is built after Minecraft's
   initial Done message; it can take time and cause startup tick-lag warnings.
6. Join 127.0.0.1:25565 from this PC using the exact same client release/build.
   Use stop in the server console for a normal save and shutdown.

WORLD AND NETWORK
Keep level-name=Graves and Glory. Activation currently checks the saved world's
internal level name (Graves and Glory or legacy Bannerbound), not just its folder.
A random existing world is not automatically converted into this game.
Default configuration binds localhost, uses online-mode=true, allows eight
players and disables RCON. It is not an Internet/home deployment configuration.
Starlink public reachability still needs an outbound relay/public-edge assessment.
No router, firewall, scheduled startup or public endpoint is configured here.

CURRENT LIMITS
One shared battlefield per server process. Local-profile imports are still
trusted; centrally secured official progression and public matchmaking are
unfinished. Do not treat this candidate as a finished public competitive service.
The production headless startup/lobby were checked locally; an authenticated
remote client match and the actual home-server PC still need verification.
'''.encode()
    output=ROOT/f'dist/graves-and-glory-{release}-server.zip';temp=output.with_suffix('.zip.tmp')
    with zipfile.ZipFile(temp,'w',zipfile.ZIP_DEFLATED) as pack:
        for name,data in sorted(files.items()):pack.writestr(name,data)
    # Read back every allowed entry; no generated runtime/EULA file is allowlisted.
    with zipfile.ZipFile(temp) as pack:
        if pack.testzip() is not None or set(pack.namelist())!=set(files):raise RuntimeError('Invalid server archive')
        for name,data in files.items():
            if pack.read(name)!=data:raise RuntimeError('Server archive readback differs: '+name)
    temp.replace(output)
    report={'fileName':output.name,'bytes':output.stat().st_size,'sha256':hashlib.sha256(output.read_bytes()).hexdigest(),
            'build':source_identity(),'mods':sorted(n for n in files if n.startswith('mods/')),
            'entriesVerified':len(files),'scope':'Kit integrity; runtime results are recorded separately in the work log'}
    (ROOT/'dist/server-manifest.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report,indent=2))

if __name__=='__main__':export()
