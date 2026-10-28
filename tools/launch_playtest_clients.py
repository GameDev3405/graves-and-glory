"""Launch additional disposable local development identities, never launcher accounts."""
import ctypes,json,pathlib,subprocess,os
r=pathlib.Path(__file__).resolve().parents[1]
m=json.loads((r/'evidence/revision-02/host-process.json').read_text(encoding='utf-8-sig'))
assert 'net.fabricmc.devlaunchinjector.Main' in m['CommandLine']
argc=ctypes.c_int();shell=ctypes.windll.shell32;shell.CommandLineToArgvW.restype=ctypes.POINTER(ctypes.c_wchar_p)
argv=shell.CommandLineToArgvW(m['CommandLine'],ctypes.byref(argc));base=[argv[i] for i in range(argc.value)];ctypes.windll.kernel32.LocalFree(argv)
log=(pathlib.Path(os.environ['LOCALAPPDATA'])/'gravesandglory/melty.log').read_text();address=log.split('Melty join address: ')[-1].splitlines()[0].strip();assert ':' in address
processes=[]
for n in range(1,4):
    run=r/f'run-playtest-{n}';run.mkdir(exist_ok=True);runtime=r/f'evidence/revision-02/join-{n}';runtime.mkdir(exist_ok=True)
    (runtime/'melty.properties').write_text('join='+address+'\n')
    args=[a for a in base if not a.startswith('-Dgravesandglory.commandPlaytest=') and not a.startswith('-Xmx')]
    args.insert(1,'-Xmx1600M');args.insert(1,'-Dgravesandglory.runtime='+str(runtime));args.insert(1,'-Dgravesandglory.joinPlaytest=true')
    if '--username' in args:args[args.index('--username')+1]=f'BannerGuest{n}'
    else:args.extend(['--username',f'BannerGuest{n}'])
    if '--uuid' in args:args[args.index('--uuid')+1]=''
    (run/'options.txt').write_text('pauseOnLostFocus:false\nonboardAccessibility:false\nrenderDistance:8\nsimulationDistance:6\nguiScale:3\n')
    output=(runtime/'console.log').open('w');p=subprocess.Popen(args,cwd=run,stdout=output,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
    processes.append({'pid':p.pid,'name':f'BannerGuest{n}'})
(r/'evidence/revision-02/guests.json').write_text(json.dumps(processes,indent=2));print('Started',len(processes),'local test clients:',[p['pid'] for p in processes])
