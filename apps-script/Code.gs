const WEEKLY_PROGRAM_SECTION = "3. HAFTALIK PROGRAM";
const WEEKLY_PROGRAM_COLUMN_COUNT = 7;

function doGet(e) {
  const sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  const data = sheet.getDataRange().getValues();
  return ContentService.createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}

function doPost(e) {
  const sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  const body = JSON.parse(e.postData.contents);

  if (body.action === "replaceWeeklyProgram") {
    const lock = LockService.getScriptLock();
    lock.waitLock(10000);
    try {
      replaceWeeklyProgram(sheet, body.rows);
    } finally {
      lock.releaseLock();
    }
  } else if (body.action === "updateStatus") {
    updateTaskStatus(sheet, body);
  } else {
    throw new Error("Desteklenmeyen işlem: " + body.action);
  }

  return ContentService.createTextOutput(JSON.stringify({ ok: true }))
    .setMimeType(ContentService.MimeType.JSON);
}

function replaceWeeklyProgram(sheet, rows) {
  if (!Array.isArray(rows) || rows.some(row =>
    !Array.isArray(row) ||
    row.length !== WEEKLY_PROGRAM_COLUMN_COUNT ||
    String(row[0]).trim() !== WEEKLY_PROGRAM_SECTION
  )) {
    throw new Error("Haftalık program satırları beklenen 7 sütunlu biçimde değil.");
  }

  const data = sheet.getDataRange().getValues();
  let startIndex = data.findIndex((row, index) =>
    index > 0 && String(row[0]).trim() === WEEKLY_PROGRAM_SECTION
  );
  let existingCount = 0;

  if (startIndex === -1) {
    startIndex = data.length;
  } else {
    while (
      startIndex + existingCount < data.length &&
      String(data[startIndex + existingCount][0]).trim() === WEEKLY_PROGRAM_SECTION
    ) {
      existingCount += 1;
    }
  }

  const startRow = startIndex + 1;
  if (rows.length > existingCount) {
    const extraRows = rows.length - existingCount;
    const insertBeforeRow = startRow + existingCount;
    if (insertBeforeRow <= sheet.getMaxRows()) {
      sheet.insertRowsBefore(insertBeforeRow, extraRows);
    } else {
      sheet.insertRowsAfter(sheet.getMaxRows(), extraRows);
    }
  }

  if (rows.length > 0) {
    sheet.getRange(startRow, 1, rows.length, WEEKLY_PROGRAM_COLUMN_COUNT).setValues(rows);
  }

  if (existingCount > rows.length) {
    sheet.deleteRows(startRow + rows.length, existingCount - rows.length);
  }
}

function updateTaskStatus(sheet, body) {
  const data = sheet.getDataRange().getValues();
  const normalizedDay = String(body.day || "").trim().toLocaleLowerCase();
  const normalizedTask = String(body.task || "").trim().toLocaleLowerCase();

  for (let i = 1; i < data.length; i += 1) {
    if (
      String(data[i][0]).trim() === WEEKLY_PROGRAM_SECTION &&
      String(data[i][1]).trim().toLocaleLowerCase() === normalizedDay &&
      String(data[i][2]).trim().toLocaleLowerCase() === normalizedTask
    ) {
      sheet.getRange(i + 1, 5).setValue(body.status);
      return;
    }
  }
}
