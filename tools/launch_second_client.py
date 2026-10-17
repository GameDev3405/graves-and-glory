"""Clone only our Fabric development client's launch, in a separate game directory.

No account credentials are read from the user's launchers. This uses the dev
client's development identity solely for local integration testing.
"""
import ctypes,json,pathlib,subprocess,os
root=pathlib.Path(__file__).resolve().parents[1]
metadata=json.loads((root/'evidence/host-process.json').read_text(encoding='utf-8-sig'))
if isinstance(metadata,list):
    metadata=next(v for v in metadata if '--username BannerHost ' in v['CommandLine'])
command=metadata['CommandLine']
argc=ctypes.c_int()
shell=ctypes.windll.shell32
shell.CommandLineToArgvW.restype=ctypes.POINTER(ctypes.c_wchar_p)
argv=shell.CommandLineToArgvW(command,ctypes.byref(argc))
args=[argv[i] for i in range(argc.value)]
ctypes.windll.kernel32.LocalFree(argv)
assert 'net.fabricmc.devlaunchinjector.Main' in args, 'Only our dev launch is supported'
args[args.index('--username')+1]='BannerJoin'
run=root/'run-join';run.mkdir(exist_ok=True)
runtime=root/'evidence/join-runtime';runtime.mkdir(exist_ok=True)
hostlog=(pathlib.Path(os.environ['LOCALAPPDATA'])/'gravesandglory/melty.log').read_text()
address=hostlog.split('Melty join address: ')[-1].splitlines()[0].strip()
assert ':' in address
(root/'evidence/host-melty.log').write_text(hostlog)
(runtime/'melty.properties').write_text('join='+address+'\n')
args.insert(1,'-Dgravesandglory.runtime='+str(runtime))
args.insert(1,'-Xmx2G')
log=(root/'evidence/join-console.log').open('w')
process=subprocess.Popen(args,cwd=run,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
(root/'evidence/join-process.json').write_text(json.dumps({'pid':process.pid,'hostPid':metadata['ProcessId'],'address':address}))
print('Started separate development join client:',process.pid)
