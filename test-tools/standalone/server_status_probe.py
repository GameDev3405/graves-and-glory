"""Read-only local Minecraft status probe; never included in player/server kits."""
import json
import os
import re
import socket
import struct
import sys

def varint(n):
    data=bytearray()
    while True:
        b=n&127;n>>=7;data.append(b|(128 if n else 0))
        if not n:return bytes(data)

def exact(sock,n):
    data=b''
    while len(data)<n:
        chunk=sock.recv(n-len(data))
        if not chunk:raise RuntimeError('Server closed status response')
        data+=chunk
    return data

def readint(sock):
    result=0
    for i in range(5):
        b=exact(sock,1)[0];result|=(b&127)<<(i*7)
        if not b&128:return result
    raise ValueError('Invalid status packet length')

def expected_max_players():
    """Max players the server kit configures (server.properties.default); 16 if unreadable."""
    path=os.path.join(os.path.dirname(os.path.abspath(__file__)),'..','..','tools','server-kit','server.properties.default')
    try:
        with open(path) as f:m=re.search(r'^max-players=(\d+)',f.read(),re.M)
        return int(m.group(1)) if m else 16
    except OSError:
        return 16

def log_hint(server_dir):
    """Last crash/error evidence from <server_dir>/logs/latest.log and crash-reports/, or None."""
    if not server_dir:return None
    hint={}
    try:
        with open(os.path.join(server_dir,'logs','latest.log'),errors='replace') as f:lines=f.read().splitlines()
        marks=[l for l in lines if re.search(r'Exception|Crash|FATAL|/ERROR\]|Failed to start|Address already in use',l)]
        hint['latestLog']=marks[-1][:300] if marks else (lines[-1][:300] if lines else 'latest.log is empty')
    except OSError:
        hint['latestLog']='no logs/latest.log'
    try:
        reports=sorted(n for n in os.listdir(os.path.join(server_dir,'crash-reports')) if n.endswith('.txt'))
        if reports:hint['crashReport']=reports[-1]
    except OSError:
        pass
    return hint

def probe(port,server_dir=None,timeout=10):
    """Return (exit_code, result dict). Never raises for an unreachable or misbehaving server."""
    try:
        with socket.create_connection(('127.0.0.1',port),timeout=timeout) as sock:
            sock.settimeout(timeout)
            host=b'localhost';hello=varint(0)+varint(767)+varint(len(host))+host+struct.pack('>H',port)+varint(1)
            sock.sendall(varint(len(hello))+hello+b'\x01\x00')
            packet_length=readint(sock)
            if packet_length>1048576:raise ValueError('Status too large')
            if readint(sock)!=0:raise ValueError('Unexpected status packet')
            size=readint(sock)
            if size>packet_length:raise ValueError('Invalid status payload length')
            data=json.loads(exact(sock,size))
            version,players=data['version'],data['players']
            want=expected_max_players()
            if version['protocol']!=767 or players['max']!=want:
                return 1,{'result':'FAIL','reason':'Unexpected status: protocol %s (want 767), max players %s (want %s)'%(version['protocol'],players['max'],want),
                          'port':port}
            return 0,{'result':'PASS','scope':'Local production server status; no authenticated player joined',
                      'version':version,'players':{'max':players['max'],'online':players['online']}}
    except (OSError,RuntimeError,ValueError,KeyError,TypeError) as e:
        out={'result':'DOWN' if isinstance(e,ConnectionRefusedError) else 'FAIL','port':port,'reason':'%s: %s'%(type(e).__name__,e)}
        hint=log_hint(server_dir)
        if hint:out['logs']=hint
        return 1,out

def main(argv):
    args=list(argv[1:]);server_dir=None
    if '--dir' in args:
        i=args.index('--dir')
        try:server_dir=args[i+1]
        except IndexError:pass
        del args[i:i+2]
    try:port=int(args[0])
    except (IndexError,ValueError):
        print(json.dumps({'result':'FAIL','reason':'usage: server_status_probe.py <port> [--dir <server folder>]'},indent=2))
        return 2
    code,out=probe(port,server_dir)
    print(json.dumps(out,indent=2))
    return code

if __name__=='__main__':
    sys.exit(main(sys.argv))
