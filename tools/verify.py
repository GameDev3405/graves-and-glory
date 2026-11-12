"""Reproducible local verification. Logs and exact input identity live in work/verify/."""
import argparse, datetime, hashlib, json, os, pathlib, shutil, subprocess, sys, threading
from concurrent.futures import ThreadPoolExecutor
from release_config import source_identity
ROOT=pathlib.Path(__file__).resolve().parents[1]
def require_game_evidence(path):
    if not path.exists(): raise RuntimeError('Game exited without check evidence')
    lines=path.read_text().splitlines()
    if any(line.startswith('fail ') for line in lines) or 'pass p1-combat-suite' not in lines:
        raise RuntimeError('Game check evidence failed/incomplete: '+str(path))
def java_home():
    candidates=[pathlib.Path(os.environ['JAVA_HOME'])] if os.environ.get('JAVA_HOME') else []
    candidates+=list(pathlib.Path('C:/Program Files/Eclipse Adoptium').glob('jdk-17*'))
    if shutil.which('javac'): candidates.append(pathlib.Path(shutil.which('javac')).resolve().parent.parent)
    for p in candidates:
        java=p/'bin'/('java.exe' if os.name=='nt' else 'java')
        if java.exists() and 'version "17.' in subprocess.run([str(java),'-version'],capture_output=True,text=True).stderr: return p
    raise RuntimeError('Java 17 JDK required; set JAVA_HOME')
def client_in_checkout(cmd,root):
    root=str(root).replace(chr(92),'/').lower().rstrip('/')
    cmd=cmd.replace(chr(92),'/').lower(); i=cmd.find(root)
    while i>=0:
        # <root>/.claude/worktrees/<lane> is another checkout, not this one.
        if (i+len(root)==len(cmd) or cmd[i+len(root)] in '/;" ') and not cmd.startswith('/.claude/worktrees/',i+len(root)): return True
        i=cmd.find(root,i+1)
    return False
def game_guard():
    if os.name!='nt': raise RuntimeError('Game process guard currently supports Windows only')
    # Windows PowerShell 5.1 ships with Windows; pwsh is an optional install. Both run the same CIM query.
    shell=shutil.which('pwsh') or shutil.which('powershell')
    if not shell: raise RuntimeError('The game process guard needs PowerShell (pwsh or Windows PowerShell)')
    data=subprocess.check_output([shell,'-NoProfile','-Command',"@(Get-CimInstance Win32_Process | Where-Object { $_.Name -match '^java(w)?\\.exe$' } | Select-Object ProcessId,CommandLine) | ConvertTo-Json -Compress"])
    rows=json.loads(data or b'[]'); rows=rows if isinstance(rows,list) else [rows]
    clients=[p for p in rows if any(s in (p.get('CommandLine') or '') for s in ['devlaunchinjector','net.minecraft.client','--gameDir'])]
    # WORKFLOW.md forbids compiling in a checkout with a running client; clients of other checkouts don't block this one.
    clients=[p for p in clients if client_in_checkout(p.get('CommandLine') or '',ROOT)]
    if clients: raise RuntimeError('Minecraft client already running in this checkout: defer compilation/game work; do not terminate it')
def run(args):
    stamp=datetime.datetime.now().strftime('%Y%m%d-%H%M%S')+'-'+str(os.getpid())
    out=ROOT/'work/verify'/stamp; out.mkdir(parents=True)
    report={'tier':args.tier,'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT).decode().strip(),
            'dirty':bool(subprocess.check_output(['git','status','--porcelain'],cwd=ROOT)),
            'source_before':source_identity(),'checks':[],'status':'incomplete','log_root':str(out)}
    generated=['src/main/java/gg/gravesandglory/Spec.java','sheets/preflight.json']
    def hashes(): return {n:hashlib.sha256((ROOT/n).read_bytes()).hexdigest() for n in generated}
    report['generated_before']=hashes()
    env=dict(os.environ,PYTHONIOENCODING='utf-8')
    lock=threading.Lock()
    def step(name,cmd,timeout=600):
        log=out/(name+'.log')
        with log.open('w',encoding='utf8') as stream:
            child=subprocess.Popen([str(c) for c in cmd],cwd=ROOT,env=env,stdout=stream,stderr=subprocess.STDOUT)
            try: code=child.wait(timeout=timeout)
            except subprocess.TimeoutExpired:
                # Only the process tree this step created, never an unrelated player's game.
                if os.name=='nt': subprocess.run(['taskkill','/PID',str(child.pid),'/T','/F'],stdout=stream,stderr=subprocess.STDOUT)
                else: child.kill()
                child.wait(); code=124
        with lock:
            report['checks'].append(dict(name=name,command=[str(c) for c in cmd],exit_code=code,log=str(log)))
            print(name+': '+('PASS' if code==0 else 'FAIL '+str(code)),flush=True)
            if code: print('\n'.join(log.read_text(encoding='utf8',errors='replace').splitlines()[-18:]),flush=True)
        if code: raise RuntimeError(name+' failed; full log '+str(log))
    def together(canon,*lanes):
        """Runs independent lanes of steps at once; every lane runs to its end, then the first failure (in lane order) is raised.
        Each step keeps its own log and exit code; the report lists the steps in canon's order, as the sequential run did."""
        with ThreadPoolExecutor(len(lanes)) as pool: futures=[pool.submit(lane) for lane in lanes]
        errors=[f.exception() for f in futures if f.exception()]
        report['checks'].sort(key=lambda c: canon.index(c['name']) if c['name'] in canon else -1)
        if errors: raise errors[0]
    try:
        game_guard()
        jdk=java_home(); env['JAVA_HOME']=str(jdk); env['PATH']=str(jdk/'bin')+os.pathsep+env['PATH']
        gradle=[str(ROOT/('gradlew.bat' if os.name=='nt' else 'gradlew')),'--no-daemon']
        if args.offline: gradle+=['--offline']
        step('preflight',[sys.executable,'tools/preflight.py'])
        tested_identity=source_identity()
        gson=list((pathlib.Path.home()/'.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson/2.10.1').glob('*/*.jar'))
        if not gson: raise RuntimeError('Gson 2.10.1 missing; run documented online setup once')
        classes=out/'classes'; classes.mkdir()
        names='Spec Gear GearStats GearEffects RelicSlots Forge GearWire GearCommands GearView BattleMath ModeMath MatchStats Progression Profile ProfileMigration MarchMath RosterMath  WarGrid CounterMath LevelMath CreditLog AdmissionPolicy SocialHub CampTerrain CompatibilityPolicy HouseRoute StepHop Wedge LayoutMarker LegacyDataMigration TroopFaces UiMath HintText FeedLines ModeLines BannerDrop  BannerCommands AudioCues SafeFiles MatchRules ArenaAutosave EngineMap MovementGuard MotionMath Clip MotionInput Pose MotionBody MotionFeet MotionStack MotionEngine MatchEnding EndingAwards PodiumLayout PropDef PropGeometry PropEffects PropPackFile HandbookText'.split()
        checks=sorted((ROOT/'test-tools/standalone').glob('*Check.java'))
        sources=[ROOT/'src/main/java/gg/gravesandglory'/(n+'.java') for n in names]
        sources+=[ROOT/'test-tools/java/gg/gravesandglory/SoleSlide.java']  # pure geometry the motion grade shares with SoleSlideCheck
        sources+=[ROOT/'test-tools/java/gg/gravesandglory/StepWindows.java']  # the grade's event windows and authored ratios, pinned by StepWindowsCheck
        # The steps below share no outputs (each Python check paints into its own temp dir, javac into work/verify, Gradle into
        # build/), so they run at once. The guard runs first because java-compile and build start here.
        game_guard()
        cpus=os.cpu_count() or 2
        def java_lane():
            step('java-compile',[jdk/'bin/javac','--release','17','-cp',gson[0],'-d',classes,*sources,*checks])
            run_check=lambda test: step(test.stem,[jdk/'bin/java','-cp',str(classes)+os.pathsep+str(gson[0]),'gg.gravesandglory.'+test.stem])
            with ThreadPoolExecutor(max(1,min(6,cpus//4))) as pool: futures=[pool.submit(run_check,t) for t in checks]
            errors=[f.exception() for f in futures if f.exception()]
            if errors: raise errors[0]
        canon=['preflight','python-tests','ui-standard','ui-assets','skin-assets','clips','java-compile',*(t.stem for t in checks),'build']
        together(canon,
            # One process per test module (slow modules split by recorded times); the same discovery, every id accounted for.
            lambda: step('python-tests',[sys.executable,'tools/parallel_tests.py','test-tools/standalone','--pattern','test_*.py','--times',ROOT/'work/verify/python-test-times.json']),
            lambda: step('ui-standard',[sys.executable,'test-tools/standalone/test_ui_standard.py']),
            # test_build_skins is already included in unittest discovery; do not run it twice.
            lambda: step('ui-assets',[sys.executable,'tools/build_ui_art.py','--verify']),
            lambda: step('skin-assets',[sys.executable,'tools/build_skins.py','--verify']),
            lambda: step('clips',[sys.executable,'tools/clips.py','--verify']),
            java_lane,
            lambda: (game_guard(),step('build',[*gradle,'build','compileLocalTestTools','-x','test'],1200)))  # the guard again right before Gradle, as before
        if args.tier=='release':
            # Strict package gate; no stopgap bypass and no upload.
            step('package',[sys.executable,'tools/package.py'])
            step('package-readback',[sys.executable,'tools/verify_package.py'])
        if args.tier=='game':
            game_guard()
            game=out/'game'; (game/'run').mkdir(parents=True); (game/'evidence').mkdir()
            (game/'run/options.txt').write_text('pauseOnLostFocus:false\nonboardAccessibility:false\nrenderDistance:8\nsimulationDistance:6\nguiScale:3\n')
            env['GRAVES_ROADMAP_TEST_ROOT']=str(game)
            env['GRAVES_PHASE1_CHECKS']='combat'
            step('game',[*gradle,'runClient','-PlocalIntegration','-I','test-tools/isolated-checks.gradle'],1800)
            evidence=game/'evidence/local-checks.txt'
            require_game_evidence(evidence)
            report['game_scope']='Phase 1 combat smoke only; multiplayer, six players, visual review and full reward loop remain separate gates'
        if source_identity()!=tested_identity: raise RuntimeError('Source changed during verification; result cannot certify this source')
        report['status']='pass'
    except Exception as exc:
        report['status']='failed'; report['error']=str(exc); print(str(exc),file=sys.stderr)
    finally:
        report['source_after']=source_identity(); report['generated_after']=hashes()
        (out/'result.json').write_text(json.dumps(report,indent=2),encoding='utf8')
        print('Report: '+str(out/'result.json'))
    return 0 if report['status']=='pass' else 1
if __name__=='__main__':
    p=argparse.ArgumentParser(); p.add_argument('tier',choices=['fast','game','release']); p.add_argument('--offline',action='store_true'); a=p.parse_args(); sys.exit(run(a))
