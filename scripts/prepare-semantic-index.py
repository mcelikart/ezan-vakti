#!/usr/bin/env python3
"""Build E5 vectors locally; Android search never downloads data.
Requires numpy, onnxruntime==1.22.0 and tokenizers==0.21.4.
The pinned model/tokenizer and authentic verse snapshots must be local first.
"""
import argparse, hashlib, json, struct, time
from pathlib import Path
import numpy as np
import onnxruntime as ort
from tokenizers import Tokenizer

parser=argparse.ArgumentParser()
parser.add_argument('--assets',type=Path,default=Path('app/src/main/assets/quran_search'))
parser.add_argument('--tokenizer',type=Path,required=True)
parser.add_argument('--report',type=Path,required=True)
args=parser.parse_args(); a=args.assets
verses=json.loads((a/'verses.json').read_text()); assert len(verses)==6236
reference={f"{v['surahId']}:{v['verseNumber']}":i for i,v in enumerate(verses)}
tokenizer=Tokenizer.from_file(str(args.tokenizer));tokenizer.no_padding();tokenizer.no_truncation()
opts=ort.SessionOptions();opts.intra_op_num_threads=4;opts.inter_op_num_threads=1
session=ort.InferenceSession(str(a/'encoder.onnx'),opts,providers=['CPUExecutionProvider'])

def tokens(text,prefix):
    return [0]+tokenizer.encode(prefix+text,add_special_tokens=False).ids[:126]+[2]

def encode_batch(ids_list):
    n=max(map(len,ids_list)); ids=np.ones((len(ids_list),n),dtype=np.int64);mask=np.zeros_like(ids)
    for i,row in enumerate(ids_list): ids[i,:len(row)]=row;mask[i,:len(row)]=1
    hidden=session.run(None,{'input_ids':ids,'attention_mask':mask,'token_type_ids':np.zeros_like(ids)})[0]
    mean=(hidden*mask[:,:,None]).sum(axis=1)/mask.sum(axis=1)[:,None]
    return (mean/np.linalg.norm(mean,axis=1,keepdims=True)).astype(np.float32)

rows=[]
for i,verse in enumerate(verses):
    passage_prefix=tokenizer.encode('passage:',add_special_tokens=False).ids
    ids=tokenizer.encode('passage: '+verse['translation'],add_special_tokens=False).ids[len(passage_prefix):]
    body_limit=126-len(passage_prefix)
    for start in range(0,max(1,len(ids)),96):
        rows.append((i,1.0,[0]+passage_prefix+ids[start:start+body_limit]+[2]))
        if start+body_limit>=len(ids): break
    if len(ids)<18:
        neighbors=verses[max(0,i-1):min(len(verses),i+2)]
        context=' '.join(v['translation'] for v in neighbors if v['surahId']==verse['surahId'])
        if context!=verse['translation']:rows.append((i,0.97,tokens(context,'passage: ')))
# Keep independent query-space concept views. Averaging a one-word subject
# with long examples erased short Turkish queries such as 'hasetlik'.
# These are model vectors, never runtime word-to-verse lookup rules.
topics=json.loads((a/'semantic-topics.json').read_text())
anchors=[]
for topic_index,topic in enumerate(topics):
    for ref in topic['verses']: assert ref in reference, ref
    texts=list(dict.fromkeys([topic['title']]+topic.get('aliases',[])+[topic['description']]+topic.get('examples',[])))
    for text in texts:
        assert text.strip(), topic['id']
        anchors.append((topic_index,tuple(tokens(text,'query: '))))
start=time.time()
unique=list(dict.fromkeys([tuple(row[2]) for row in rows]+[ids for _,ids in anchors]))
encoded={}
for offset in range(0,len(unique),24):
    batch=unique[offset:offset+24]; values=encode_batch(batch)
    encoded.update(zip(batch,values))
    if offset%480==0: print(f'{offset}/{len(unique)} unique sequences, {time.time()-start:.1f}s',flush=True)
raw_vectors=np.stack([encoded[tuple(row[2])] for row in rows])
centroid=raw_vectors.mean(axis=0).astype(np.float32)
with (a/"centroid.bin").open("wb") as f:
    f.write(struct.pack("<4si",b"QSC1",384));f.write(centroid.astype("<f4").tobytes())
def centered(values):
    values=values-centroid
    return values/np.linalg.norm(values,axis=-1,keepdims=True)
vectors=list(centered(raw_vectors))
vectors=np.stack(vectors).astype(np.float32)
with (a/'vectors.bin.tmp').open('wb') as f:
    f.write(struct.pack('<4sii',b'QSV2',384,len(rows)))
    for (verse,weight,_),vector in zip(rows,vectors):
        f.write(struct.pack('<if',verse,weight));f.write(vector.astype('<f4').tobytes())
(a/'vectors.bin.tmp').replace(a/'vectors.bin')
with (a/'concept-anchors.bin.tmp').open('wb') as f:
    f.write(struct.pack('<4siii',b'QCA1',384,len(anchors),len(topics)))
    for topic_index,ids in anchors:
        f.write(struct.pack('<i',topic_index))
        f.write(encoded[ids].astype('<f4').tobytes())
(a/'concept-anchors.bin.tmp').replace(a/'concept-anchors.bin')
meta={'format':'QSV2','dimensions':384,'rows':len(rows),'verses':6236,
 'conceptFormat':'QCA1','concepts':len(topics),'anchors':len(anchors),
 'model':'intfloat/multilingual-e5-small','revision':'ccc66d3',
 'encoder':'onnx/model_qint8_avx512_vnni.onnx','maxTokens':128,
 'pooling':'attention-mask mean; L2 normalization; corpus mean removal; L2 normalization','prefixes':['query: ','passage: '],
 'ranking':'centered passage cosine plus gated query-space concept anchors; per-verse passage evidence ranks concept candidates',
 'sha256':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in [a/'encoder.onnx',a/'verses.json',a/'vectors.bin',a/'centroid.bin',a/'semantic-topics.json',a/'concept-anchors.bin',a/'unigram-vocab.bin',a/'unigram-normalizer.bin']}}
(a/'semantic-index.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2)+'\n')
queries=['İnsanların arkamdan konuşması ve kusurlarımı araştırması','Borç verdiğim kişi ödeme yapamıyor, ona nasıl davranmalıyım?','Anne ve babam yaşlanınca onlara karşı sorumluluklarım','Ümidimi kaybettim, yaptığım hatalardan geri dönmek istiyorum','İşyerinde hak etmediği halde yakınına görev vermek','Öfkeliyken kendimi tutmak ve birini bağışlamak','Kendimi yapayalnız hissediyorum','Aldığım bir haberin doğru olup olmadığını araştırmak','Ticarette ölçüyü eksik tutmak','Başka inançtan birini zorla dine sokmak','Kadın ve erkeğin yaptığı iyiliklerin karşılığı','İhtiyacı olan insanlara gösteriş yapmadan yardım etmek','Kötü davranan birine de adil olmak','Namaz','zxqwvzzz xqzzvwp']
report=[]
for query in queries:
    ids=tokens(query,'query: ');raw_vector=encode_batch([ids])[0]
    vector=raw_vector-centroid;vector=vector/np.linalg.norm(vector);scores=vectors@vector
    best=np.full(len(verses),-1.0,dtype=np.float32);direct=best.copy()
    for row,score in zip(rows,scores):
        value=float(score)*row[1];best[row[0]]=max(best[row[0]],value)
        if row[1]!=0.995:direct[row[0]]=max(direct[row[0]],value)
    order=sorted(range(len(verses)),key=lambda i:(-best[i],-direct[i],i))[:30]
    # The report supplies independent encoder parity fixtures. Passage diagnostics
    # deliberately exclude the runtime concept gates and are not search-quality results.
    report.append({'query':query,'tokenIds':ids,'queryEmbedding':raw_vector.tolist(),'passageDiagnosticResults':[{'reference':f"{verses[i]['surahId']}:{verses[i]['verseNumber']}",'score':round(float(best[i]),5),'translation':verses[i]['translation']} for i in order]})
args.report.parent.mkdir(parents=True,exist_ok=True);args.report.write_text(json.dumps(report,ensure_ascii=False,indent=2))
print(json.dumps({'vectors':len(rows),'seconds':round(time.time()-start,2),'output':str(a/'vectors.bin')}),flush=True)
