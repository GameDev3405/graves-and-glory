"""PUT one authorized artifact using the exact URL/headers returned by Melty.
Never prints signed URLs, keys, headers or tokens.
"""
import json,pathlib,sys,urllib.parse,urllib.request
plan=json.loads(pathlib.Path(sys.argv[1]).read_text(encoding='utf-8-sig'))
artifact=pathlib.Path(sys.argv[2]).resolve()
url=plan.get('uploadUrl') or plan.get('url') or plan.get('signedUrl')
headers=plan.get('headers') or plan.get('uploadHeaders') or {}
if not url:raise SystemExit('Upload plan has no single PUT URL')
parsed=urllib.parse.urlparse(url)
if parsed.scheme!='https' or parsed.hostname!='ekvzctwehmwidsengayc.storage.supabase.co':raise SystemExit('Unexpected storage destination; no bytes sent')
data=artifact.read_bytes()
request=urllib.request.Request(url,data=data,headers=headers,method='PUT')
with urllib.request.urlopen(request,timeout=120) as response:
    print('Artifact upload HTTP',response.status,'bytes',len(data))
