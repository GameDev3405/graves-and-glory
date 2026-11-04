import os, subprocess, time, sys, shutil
R = 'C:/Users/kortt/Melty/bannerbound-qa3-1104'
W = R + '/work/qa3/net'
shutil.rmtree(W, ignore_errors=True)
for d in ('ctl', 'host/evidence', 'join/run', 'join/evidence'):
    os.makedirs(W + '/' + d)
open(W + '/join/run/options.txt', 'w').write('pauseOnLostFocus:false\nonboardAccessibility:false\nrenderDistance:8\nsimulationDistance:6\nguiScale:3\nresourcePacks:["fabric","gravesandglory:armory"]\n')
env = dict(os.environ, JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot',
           GRAVES_ROADMAP_TEST_ROOT=W + '/host', GRAVES_NETWORK_HOST='true',
           GRAVES_NETWORK_ROOT=W + '/ctl', GRAVES_ROADMAP_USER='CampHost')
host = subprocess.Popen([R + '/gradlew.bat', '--offline', 'runClient', '-PcampaignChecks', '-PlocalIntegration', '-I', 'test-tools/isolated-checks.gradle'],
                        cwd=R, env=env, stdout=open(R + '/work/qa3/net-host.log', 'w'), stderr=subprocess.STDOUT)
print('host gradle pid', host.pid, flush=True)
t0 = time.time()
while not os.path.exists(W + '/ctl/address.txt'):
    if host.poll() is not None or time.time() - t0 > 600:
        print('host died/timeout before address.txt', host.poll(), flush=True); sys.exit(1)
    time.sleep(3)
print('address', open(W + '/ctl/address.txt').read(), flush=True)
ps = subprocess.run(['powershell', '-NoProfile', '-Command',
    "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.CommandLine -like '*gravesandglory.networkHost=true*' } | ForEach-Object { $_.ProcessId; $_.CommandLine }"],
    capture_output=True, text=True).stdout.strip().split('\n')
hostpid, cmd = int(ps[0].strip()), ps[1].strip()
print('host java pid', hostpid, flush=True)
wb = W.replace('/', chr(92))
cmd = cmd.replace(W + '/host', W + '/join').replace(wb + chr(92) + 'host', wb + chr(92) + 'join')
cmd = cmd.replace('-Dgravesandglory.networkHost=true', '-Dgravesandglory.networkHost=false -Dgravesandglory.networkJoin=true').replace('CampHost', 'CampFriend')
open(R + '/work/qa3/net-join-cmd.txt', 'w').write(cmd)
join = subprocess.Popen(cmd, cwd=W + '/join/run', stdout=open(R + '/work/qa3/net-join.log', 'w'), stderr=subprocess.STDOUT)
print('joiner pid', join.pid, flush=True)
t0 = time.time()
while join.poll() is None and time.time() - t0 < 1500:
    time.sleep(5)
print('joiner exit', join.poll(), 'after', int(time.time() - t0), flush=True)
time.sleep(5)
if join.poll() is None:
    subprocess.run(['taskkill', '/PID', str(join.pid), '/T', '/F'])
subprocess.run(['taskkill', '/PID', str(hostpid), '/T', '/F'])
time.sleep(3)
print('done', flush=True)
