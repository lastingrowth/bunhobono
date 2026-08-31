import fs from 'node:fs/promises';
import path from 'node:path';

const root = path.resolve('bunhobono');
const javaRoot = path.join(root, 'backend/src/main/java');
const frontRoot = path.join(root, 'frontend/src');
const fastRoot = path.join(root, 'fast-api');

async function filesUnder(dir, exts) {
  const out = [];
  for (const ent of await fs.readdir(dir, { withFileTypes: true })) {
    const p = path.join(dir, ent.name);
    if (ent.isDirectory()) out.push(...await filesUnder(p, exts));
    else if (exts.some(e => ent.name.endsWith(e))) out.push(p);
  }
  return out;
}
const rel = p => path.relative(root, p).replaceAll('\\', '/');
const lineAt = (s, i) => s.slice(0, i).split('\n').length;

function closePair(s, start, open='(', close=')') {
  let d = 0, quote = '', esc = false;
  for (let i=start; i<s.length; i++) {
    const c=s[i];
    if (quote) { if (esc) esc=false; else if(c==='\\') esc=true; else if(c===quote) quote=''; continue; }
    if (c==='"' || c==="'" || c==='`') { quote=c; continue; }
    if (c===open) d++;
    else if (c===close && --d===0) return i;
  }
  return -1;
}

const javaFiles = await filesUnder(javaRoot, ['.java']);
const dtoFields = new Map();
for (const f of javaFiles) {
  const s = await fs.readFile(f, 'utf8');
  const cls = path.basename(f, '.java');
  const fields = [];
  for (const m of s.matchAll(/\bprivate\s+(?!static\b)(?:final\s+)?([\w<>?,.\[\] ]+)\s+(\w+)\s*(?:[;=])/g)) fields.push(`${m[2]}: ${m[1].trim()}`);
  const rec = s.match(/\brecord\s+\w+\s*\(([^)]*)\)/s);
  if (rec) for (const p of rec[1].split(',')) { const m=p.trim().match(/([\w<>?,.\[\] ]+)\s+(\w+)$/); if(m) fields.push(`${m[2]}: ${m[1].trim()}`); }
  if (fields.length) dtoFields.set(cls, fields);
}

const catKo = {
  CameraPdm:'카메라 예지보전', CameraData:'카메라 데이터', Bill:'요금 정산', FeeRule:'요금 정책', Camera:'카메라',
  Faq:'FAQ', CarLog:'차량 입출차 이력', Board:'게시판', Login:'인증', Inquiry:'문의', GatePdm:'게이트 예지보전',
  Gate:'게이트', MemPurchase:'정기권 구매', Kiosk:'키오스크', MemberArchive:'회원 보관', Weather:'날씨', Member:'회원',
  Notice:'공지 알림', MemNotice:'회원 알림', ParkingSpace:'주차 공간', AiChat:'AI 채팅', Parking:'주차장',
  RobotPdm:'로봇 예지보전', Vehicle:'차량', RobotLog:'로봇 이력', Robot:'주차 로봇', Reset:'데모 초기화',
  RobotTask:'로봇 작업', Trash:'휴지통'
};

function categoryFromController(name) { return catKo[name.replace(/Controller$/, '')] || name.replace(/Controller$/, ''); }
function joinUrl(a,b) { const x=`${a||''}/${b||''}`.replace(/\/+/, '/').replace(/\/{2,}/g,'/'); return x.length>1?x.replace(/\/$/,''):x; }
function titleFrom(methodName, http, url, category) {
  const exact = {
    login:'로그인', home:'FastAPI 상태 조회', resetDemo:'시연 상태 초기화', detectTest:'번호판 검출 테스트', ocrTest:'번호판 OCR 테스트', ocr:'번호판 OCR 처리',
    cctvStream:'CCTV 영상 스트리밍', cctvStatus:'CCTV 상태 조회', pauseStream:'CCTV 일시정지', resumeStream:'CCTV 재개', completePendingStream:'CCTV 대기 처리 완료', restartStream:'CCTV 영상 재시작',
    sendSignupPhoneCode:'회원가입 휴대전화 인증번호 발송', verifySignupPhoneCode:'회원가입 휴대전화 인증번호 확인', sendSignupEmailCode:'회원가입 이메일 인증번호 발송', verifySignupEmailCode:'회원가입 이메일 인증번호 확인'
  };
  if (exact[methodName]) return exact[methodName];
  const n=methodName.toLowerCase();
  let action = http==='GET'?'조회':http==='POST'?'등록/처리':http==='PUT'?'수정':http==='PATCH'?'부분 수정':'삭제';
  if (/analyze|predict/.test(n)) action='분석/예측 실행'; else if (/complete.*action/.test(n)) action='조치 완료'; else if (/search/.test(n)) action='검색'; else if (/detail|findby|one$/.test(n)) action='상세 조회'; else if (/list|all|find|get/.test(n) && http==='GET') action='목록 조회';
  else if (/restore/.test(n)) action='복원'; else if (/approve/.test(n)) action='승인'; else if (/confirm/.test(n)) action='확인'; else if (/calculate/.test(n)) action='계산';
  else if (/read/.test(n)) action='읽음 처리';
  else if (/withdraw/.test(n)) action='탈퇴 처리'; else if (/archive/.test(n)) action='보관 처리'; else if (/sendcode/.test(n)) action='인증번호 발송';
  else if (/verify/.test(n)) action='검증'; else if (/password/.test(n)) action='비밀번호 변경'; else if (/repark/.test(n)) action='재주차 요청'; else if (/parkout/.test(n)) action='출차 요청';
  return `${category} ${action}`;
}

function javaParams(signature) {
  const result=[];
  const inside=signature.slice(signature.indexOf('(')+1,-1); const parts=[]; let start=0, par=0, angle=0;
  for(let i=0;i<inside.length;i++){const c=inside[i]; if(c==='(')par++; else if(c===')')par--; else if(c==='<')angle++; else if(c==='>')angle--; else if(c===','&&par===0&&angle===0){parts.push(inside.slice(start,i));start=i+1;}}
  parts.push(inside.slice(start));
  for(const raw of parts){
    const p=raw.trim(); if(!p)continue;
    const ann=[...p.matchAll(/@(PathVariable|RequestParam|RequestPart|RequestBody|AuthenticationPrincipal)(?:\s*\(([^)]*)\))?/g)];
    const cleaned=p.replace(/@\w+(?:\s*\([^)]*\))?\s*/g,'').replace(/\bfinal\s+/g,'').trim();
    const tm=cleaned.match(/^([\w<>?,.\[\] ]+)\s+(\w+)$/); if(!tm)continue;
    const type=tm[1].trim(), variable=tm[2]; const main=ann.find(a=>['PathVariable','RequestParam','RequestPart','RequestBody'].includes(a[1]));
    if(main?.[1]==='RequestBody'){
      const simple=type.match(/<\s*([\w.]+)\s*>/)?.[1]||type.replace(/\[\]/,''); const fields=dtoFields.get(simple.split('.').pop());
      result.push(`Body: ${variable} (${type})${fields?` → ${fields.join(', ')}`:''}`);
    } else if(main){
      const named=main[2]?.match(/(?:value|name)\s*=\s*"([^"]+)"|"([^"]+)"/); const name=named?.[1]||named?.[2]||variable;
      const required=/required\s*=\s*false/.test(main[2]||'')?'N':'Y'; const loc=main[1]==='PathVariable'?'Path':main[1]==='RequestParam'?'Query':'Part';
      result.push(`${loc}: ${name} (${type}, ${required})`);
    } else if(ann.some(a=>a[1]==='AuthenticationPrincipal')||/Authentication|Principal/.test(type)) result.push(`인증 사용자: ${variable} (${type})`);
  }
  return result.join(' / ') || '없음';
}

const spring=[];
for (const f of javaFiles) {
  const s=await fs.readFile(f,'utf8');
  if(!/@RestController\b/.test(s)) continue;
  const className=path.basename(f,'.java');
  const classPos=s.search(/\bclass\s+/);
  const head=s.slice(0,classPos<0?s.length:classPos);
  const base=(head.match(/@RequestMapping\s*\(\s*(?:value\s*=\s*)?"([^"]*)"/)||[])[1]||'';
  const re=/@(GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)\b/g; let m;
  while((m=re.exec(s))) {
    let end=m.index+m[0].length, args='';
    while(/\s/.test(s[end])) end++;
    if(s[end]==='(') { const c=closePair(s,end); args=s.slice(end+1,c); end=c+1; }
    const next=s.slice(end).search(/\bpublic\s+/); if(next<0) continue;
    const pub=end+next; const par=s.indexOf('(',pub); if(par<0) continue; const pc=closePair(s,par); if(pc<0) continue;
    const signature=s.slice(pub,pc+1).replace(/\s+/g,' ');
    const before=s.slice(pub,par); const mm=before.match(/(\w+)\s*$/); if(!mm) continue;
    const p=(args.match(/"([^"]*)"/)||[])[1]||'';
    const method=m[1].replace('Mapping','').toUpperCase(); const category=categoryFromController(className);
    spring.push({server:'Spring Boot',category,method,url:joinUrl(base,p),feature:titleFrom(mm[1],method,joinUrl(base,p),category),params:javaParams(signature),handler:mm[1],source:rel(f),line:lineAt(s,m.index)});
  }
}

function pyCategory(url) {
  if(url.startsWith('/cctv')) return 'CCTV'; if(url.startsWith('/ocr')||url.startsWith('/detect')) return 'OCR';
  if(url.startsWith('/demo/predictive-maintenance/camera')) return '카메라 예지보전'; if(url.startsWith('/demo/predictive-maintenance/gate')) return '게이트 예지보전'; if(url.startsWith('/demo/predictive-maintenance/robot')) return '로봇 예지보전'; if(url.startsWith('/demo')) return '데모 초기화'; return 'FastAPI 상태';
}
const fast=[];
for(const f of await filesUnder(fastRoot,['.py'])) {
  const s=await fs.readFile(f,'utf8');
  const prefixes=new Map();
  for(const m of s.matchAll(/(\w+)\s*=\s*APIRouter\s*\(\s*prefix\s*=\s*["']([^"']+)["']/g)) prefixes.set(m[1],m[2]);
  const re=/@(app|router)\.(get|post|put|patch|delete)\s*\(\s*["']([^"']*)["'][^)]*\)/g; let m;
  while((m=re.exec(s))) {
    const url=joinUrl(m[1]==='router'?(prefixes.get('router')||''):'',m[3]);
    const after=m.index+m[0].length; const defm=s.slice(after).match(/\s*(?:async\s+)?def\s+(\w+)\s*\(/); if(!defm) continue;
    const par=s.indexOf('(',after+defm.index); const pc=closePair(s,par); const sig=s.slice(par+1,pc);
    const params=[];
    for(const p of sig.split(/,(?![^()]*\))/)) { const pm=p.trim().match(/(\w+)\s*:\s*([^=]+)(?:=\s*(.*))?/s); if(!pm) continue; const loc=url.includes(`{${pm[1]}}`)?'Path':/Form\(/.test(pm[3]||'')?'Form':/File\(/.test(pm[3]||'')?'File':'Query'; params.push(`${loc}: ${pm[1]} (${pm[2].trim()})`); }
    const category=pyCategory(url), method=m[2].toUpperCase();
    fast.push({server:'FastAPI',category,method,url,feature:titleFrom(defm[1],method,url,category),params:params.join(' / ')||'없음',handler:defm[1],source:rel(f),line:lineAt(s,m.index)});
  }
}

const backend=[...spring,...fast].sort((a,b)=>a.server.localeCompare(b.server)||a.category.localeCompare(b.category,'ko')||a.url.localeCompare(b.url)||a.method.localeCompare(b.method));

function maskComments(s) { return s.replace(/\/\*[\s\S]*?\*\//g,x=>' '.repeat(x.length)).replace(/(^|[^:])\/\/.*$/gm,(x,p)=>p+' '.repeat(x.length-p.length)); }
function normTemplate(x) { return x.replace(/^['"`]|['"`]$/g,'').replace(/\$\{([^}]+)\}/g,'{$1}'); }
function structural(u){return u.replace(/\{[^}]+\}/g,'{}').replace(/\?.*$/,'').replace(/\/$/,'')||'/';}
const frontFiles=await filesUnder(frontRoot,['.js','.vue']);
const frontSources=new Map(); for(const f of frontFiles) frontSources.set(f,await fs.readFile(f,'utf8'));
const front=[];
for(const [f,raw] of frontSources) {
  const s=maskComments(raw); const re=/(api|axios)\.(get|post|put|patch|delete)\s*\(/g; let m;
  while((m=re.exec(s))) {
    const pc=closePair(s,s.indexOf('(',m.index)); if(pc<0) continue; const arg=s.slice(s.indexOf('(',m.index)+1,pc).trim();
    let first=arg.match(/^([`'"][\s\S]*?[`'"])/)?.[1];
    if(!first) { const v=arg.match(/^(\w+)/)?.[1]; if(v){ const prior=s.slice(Math.max(0,m.index-800),m.index); const vm=[...prior.matchAll(new RegExp(`(?:const|let)\\s+${v}\\s*=\\s*([\`'\"][^;]+)`,'g'))].pop(); first=vm?.[1]; } }
    if(!first) continue;
    let url=normTemplate(first), server=m[1]==='axios'&&url.startsWith('/fastapi')?'FastAPI':'Spring Boot';
    if(server==='FastAPI') url=url.replace(/^\/fastapi/,'')||'/'; else if(!url.startsWith('/api/')) url=joinUrl('/api',url);
    const prior=s.slice(0,m.index); const candidates=[];
    for(const x of prior.matchAll(/export\s+const\s+(\w+)\s*=/g)) candidates.push({at:x.index,name:x[1]});
    for(const x of prior.matchAll(/(?:export\s+)?(?:async\s+)?function\s+(\w+)\s*\(/g)) candidates.push({at:x.index,name:x[1]});
    for(const x of prior.matchAll(/^\s*(?:async\s+)?(\w+)\s*\([^)]*\)\s*\{/gm)) candidates.push({at:x.index,name:x[1]});
    candidates.sort((a,b)=>a.at-b.at); const funcName=candidates.pop()?.name||'(직접 호출)';
    const usage=[];
    if(funcName!=='(직접 호출)') for(const [uf,us] of frontSources) if(uf!==f && new RegExp(`\\b${funcName}\\b`).test(maskComments(us))) usage.push(rel(uf));
    const match=backend.find(e=>e.server===server&&e.method===m[2].toUpperCase()&&structural(e.url)===structural(url));
    front.push({server,category:match?.category||pyCategory(url),method:m[2].toUpperCase(),url,apiFunction:funcName,usage:usage.join('\n')||rel(f),apiSource:rel(f),line:lineAt(raw,m.index),backendMatch:match?'일치':'불일치',backendHandler:match?.handler||''});
  }
  const fre=/fetch\s*\(\s*(`[^`]+`|'[^']+'|"[^"]+")\s*(?:,\s*\{([\s\S]*?)\})?\s*\)/g; let fm;
  while((fm=fre.exec(s))) {
    let url=normTemplate(fm[1]).replace(/^\$\{FASTAPI_URL\}/,''); if(!url.startsWith('/')) continue;
    const method=((fm[2]||'').match(/method\s*:\s*['"](\w+)['"]/)||[])[1]?.toUpperCase()||'GET';
    const match=backend.find(e=>e.server==='FastAPI'&&e.method===method&&structural(e.url)===structural(url));
    front.push({server:'FastAPI',category:match?.category||pyCategory(url),method,url,apiFunction:'(화면 직접 호출)',usage:rel(f),apiSource:rel(f),line:lineAt(raw,fm.index),backendMatch:match?'일치':'불일치',backendHandler:match?.handler||''});
  }
}

const dedup=[]; const seen=new Set();
for(const x of front) { const k=[x.server,x.method,x.url,x.apiFunction,x.apiSource].join('|'); if(!seen.has(k)){seen.add(k);dedup.push(x);} }
dedup.sort((a,b)=>a.server.localeCompare(b.server)||a.category.localeCompare(b.category,'ko')||a.url.localeCompare(b.url)||a.method.localeCompare(b.method));

const mappedKeys=new Set(dedup.filter(x=>x.backendMatch==='일치').map(x=>`${x.server}|${x.method}|${structural(x.url)}`));
const unused=backend.filter(x=>!mappedKeys.has(`${x.server}|${x.method}|${structural(x.url)}`));
const duplicateBackend=[]; const bk=new Map(); for(const x of backend){const k=`${x.server}|${x.method}|${structural(x.url)}`;bk.set(k,(bk.get(k)||0)+1);} for(const [k,n] of bk)if(n>1)duplicateBackend.push({key:k,count:n});
const report={generatedAt:new Date().toISOString(),backend,front:dedup,unused,validation:{springCount:spring.length,fastApiCount:fast.length,backendCount:backend.length,frontendCallCount:dedup.length,frontendMatched:dedup.filter(x=>x.backendMatch==='일치').length,frontendUnmatched:dedup.filter(x=>x.backendMatch!=='일치').length,backendUnused:unused.length,duplicateBackend}};
await fs.writeFile('.codex-work/api_inventory.json',JSON.stringify(report,null,2));
console.log(JSON.stringify(report.validation,null,2));
console.log('UNMATCHED',dedup.filter(x=>x.backendMatch!=='일치'));
