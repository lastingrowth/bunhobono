import fs from 'node:fs/promises';
import path from 'node:path';
import { FileBlob, SpreadsheetFile } from '@oai/artifact-tool';
const base=path.resolve('outputs/api-docs-20260831/compare');
const colors=['#DCE6F1','#E2F0D9','#FCE4D6','#E4DFEC','#FFF2CC','#DDEBF7','#F4CCCC','#D9EAD3','#CFE2F3','#EADCF8','#FCE5CD','#D0E0E3','#E6E6E6','#F9CB9C','#B6D7A8','#C9DAF8','#D9D2E9','#FCE8B2','#CFE8E8','#E8D5C4','#D6E4F0','#EAD1DC','#D9E2F3','#E2EFDA','#FBE5D6','#EDEDED','#E4EAF2','#F3E5F5','#E0F2F1'];
const border={preset:'all',style:'thin',color:'#A6A6A6'};
const jobs=[
 ['백엔드_API_목록_v2_예제반영.xlsx','번호보노-백엔드 API정의서',183,'A1:O185','backend-v2-pastel.png'],
 ['프론트_API_매핑_목록_v2_예제반영.xlsx','번호보노-프론트 API매핑',146,'A1:O148','frontend-v2-pastel.png']
];
await fs.mkdir(path.join(base,'previews'),{recursive:true});
for(const[file,sheetName,count,renderRange,png]of jobs){
 const p=path.join(base,file);const wb=await SpreadsheetFile.importXlsx(await FileBlob.load(p));const s=wb.worksheets.getItem(sheetName);
 let row=2,colorIdx=0;
 while(row<=count+1){const value=s.getRange(`A${row}`).values?.[0]?.[0];let next=row+1;while(next<=count+1&&!s.getRange(`A${next}`).values?.[0]?.[0])next++;
  const end=next-1;s.getRange(`A${row}:A${end}`).format={fill:colors[colorIdx++%colors.length],font:{name:'맑은 고딕',size:9,bold:true,color:'#263238'},horizontalAlignment:'center',verticalAlignment:'center',wrapText:true,borders:border};row=next;
 }
 const err=await wb.inspect({kind:'match',searchTerm:'#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A',options:{useRegex:true,maxResults:100}});console.log(err.ndjson);
 const img=await wb.render({sheetName,range:renderRange,scale:.65,format:'png'});await fs.writeFile(path.join(base,'previews',png),new Uint8Array(await img.arrayBuffer()));
 const out=await SpreadsheetFile.exportXlsx(wb);await out.save(p);
}
