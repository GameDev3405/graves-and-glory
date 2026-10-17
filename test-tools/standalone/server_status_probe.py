"""Read-only local Minecraft status probe; never included in player/server kits."""
import json
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

if __name__=='__main__':
    port=int(sys.argv[1])
    with socket.create_connection(('127.0.0.1',port),timeout=10) as sock:
        host=b'localhost';hello=varint(0)+varint(763)+varint(len(host))+host+struct.pack('>H',port)+varint(1)
        sock.sendall(varint(len(hello))+hello+b'\x01\x00')
        packet_length=readint(sock)
        if packet_length>1048576:raise ValueError('Status too large')
        if readint(sock)!=0:raise ValueError('Unexpected status packet')
        size=readint(sock)
        if size>packet_length:raise ValueError('Invalid status payload length')
        data=json.loads(exact(sock,size))
        assert data['version']['protocol']==763 and data['players']['max']==8
        print(json.dumps({'result':'PASS','scope':'Local production server status; no authenticated player joined',
                          'version':data['version'],'players':{'max':data['players']['max'],'online':data['players']['online']}},indent=2))
