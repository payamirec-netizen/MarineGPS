import sys,zipfile,hashlib,struct
def sha(b): return hashlib.sha256(b).hexdigest()
def signing_block(path):
    b=open(path,'rb').read()
    e=b.rfind(b'PK\x05\x06')
    cd=struct.unpack_from('<I',b,e+16)[0]
    if b[cd-16:cd]!=b'APK Sig Block 42': return None
    size=struct.unpack_from('<Q',b,cd-24)[0]
    return sha(b[cd-size-8:cd])
a,b=sys.argv[1:3]
with zipfile.ZipFile(a) as x,zipfile.ZipFile(b) as y:
    print('Entry order identical:',x.namelist()==y.namelist())
    print('Signing block hashes:',signing_block(a),signing_block(b))
    for n in sorted(set(x.namelist())&set(y.namelist())):
        p,q=x.getinfo(n),y.getinfo(n)
        for k in ('date_time','compress_type','compress_size','flag_bits','extra','header_offset','external_attr','create_system'):
            if getattr(p,k)!=getattr(q,k): print(n,k,repr(getattr(p,k)),repr(getattr(q,k)))
