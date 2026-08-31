import fs from 'node:fs/promises';
import { FileBlob, SpreadsheetFile } from '@oai/artifact-tool';

const inputPath = 'C:/Users/503-30/AppData/Local/Temp/종로2팀_API정의서.xlsx';
const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(inputPath));
console.log((await wb.inspect({kind:'workbook,sheet,table,region,computedStyle',maxChars:24000,tableMaxRows:20,tableMaxCols:20,tableMaxCellChars:200})).ndjson);
const sheets = JSON.parse('[]');
const sheetInfo = await wb.inspect({kind:'sheet',include:'id,name',maxChars:12000});
console.log('SHEETS\n'+sheetInfo.ndjson);
for (const line of sheetInfo.ndjson.split('\n')) {
  try { const o=JSON.parse(line); if(o.kind==='sheet'&&o.name) sheets.push(o.name); } catch {}
}
await fs.mkdir('.codex-work/reference-previews',{recursive:true});
for (let i=0;i<sheets.length;i++) {
  const blob=await wb.render({sheetName:sheets[i],autoCrop:'all',scale:1,format:'png'});
  const file=`.codex-work/reference-previews/sheet-${i+1}.png`;
  await fs.writeFile(file,new Uint8Array(await blob.arrayBuffer()));
  console.log(`${i+1}\t${sheets[i]}\t${file}`);
}
