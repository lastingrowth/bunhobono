import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const input = await FileBlob.load("bunhobono/backend/outputs/api_summary/API_summary.xlsx");
const workbook = await SpreadsheetFile.importXlsx(input);
const summary = await workbook.inspect({
  kind: "workbook,sheet,table,region,computedStyle",
  maxChars: 12000,
  tableMaxRows: 12,
  tableMaxCols: 16,
  tableMaxCellChars: 160,
});
console.log(summary.ndjson);
