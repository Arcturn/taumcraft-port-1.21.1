import gzip,struct,io,json
from pathlib import Path
class Reader:
 def __init__(self,b):self.f=io.BytesIO(b)
 def unpack(self,fmt):return struct.unpack('>'+fmt,self.f.read(struct.calcsize('>'+fmt)))[0]
 def string(self):return self.f.read(self.unpack('H')).decode('utf-8')
 def data(self,t):
  if t in {1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}:return self.unpack({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
  if t==7:return list(self.f.read(self.unpack('i')))
  if t==8:return self.string()
  if t==9:
   subtype=self.unpack('B');n=self.unpack('i');return [self.data(subtype) for _ in range(n)]
  if t==10:
   out={}
   while True:
    subtype=self.unpack('B')
    if subtype==0:return out
    name=self.string();out[name]=self.data(subtype)
  if t in (11,12):return [self.unpack('i' if t==11 else 'q') for _ in range(self.unpack('i'))]
  raise ValueError(t)
def read(path):
 b=Path(path).read_bytes()
 if b.startswith(b'\x1f\x8b'):b=gzip.decompress(b)
 r=Reader(b);t=r.unpack('B');name=r.string();d=r.data(t);assert r.f.read()==b'';return d
if __name__=='__main__':
 import sys
 print(json.dumps(read(sys.argv[1]),ensure_ascii=False,indent=2))
