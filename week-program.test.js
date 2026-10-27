import test from "node:test";
import assert from "node:assert/strict";
import {
  INITIAL_CAMP_WEEK_KEY,
  PROVIDED_PROGRAM_WEEK_KEY,
  FOLLOWING_WEEK_PROGRAM_KEY,
  doesProgramMatchExpected,
  mergeStatusIntoExpectedProgram,
  normalizeImportedJsonProgram
} from "./week-program.js";

test("week keys correspond to Monday start dates of each October 2026 week", () => {
  assert.equal(INITIAL_CAMP_WEEK_KEY, "2026-10-12");
  assert.equal(PROVIDED_PROGRAM_WEEK_KEY, "2026-10-19");
  assert.equal(FOLLOWING_WEEK_PROGRAM_KEY, "2026-10-26");
});

test("doesProgramMatchExpected rejects 19-25 Ekim sheet tasks when viewing 26 Ekim - 1 Kasım week", () => {
  const oct19SheetProgram = {
    pazartesi: [
      { title: "Trigonometri 1-5. Bölüm", sub: "📖 345 Allstar AYT Mat" }
    ],
    salı: [],
    çarşamba: [],
    perşembe: [],
    cuma: [],
    cumartesi: [],
    pazar: []
  };
  const oct26ExpectedProgram = {
    pazartesi: [
      { title: "3D Türkiye Geneli Deneme Sınavı (1 Genel Deneme) + Analiz", sub: "📖 3D Yayınları" }
    ],
    salı: [],
    çarşamba: [],
    perşembe: [],
    cuma: [],
    cumartesi: [],
    pazar: []
  };

  assert.equal(doesProgramMatchExpected(oct19SheetProgram, oct26ExpectedProgram), false);
  assert.equal(doesProgramMatchExpected(oct26ExpectedProgram, oct26ExpectedProgram), true);
});

test("mergeStatusIntoExpectedProgram preserves task completion for matching week tasks only", () => {
  const expected = {
    pazartesi: [
      { title: "3D Türkiye Geneli Deneme Sınavı", sub: "📖 3D Yayınları", done: false, status: "Bekliyor" }
    ]
  };
  const existingSameWeek = {
    pazartesi: [
      { title: "3D Türkiye Geneli Deneme Sınavı", sub: "📖 3D Yayınları", done: true, status: "Tamamlandı" }
    ]
  };
  const merged = mergeStatusIntoExpectedProgram(expected, existingSameWeek);
  assert.equal(merged.pazartesi[0].done, true);
  assert.equal(merged.pazartesi[0].status, "Tamamlandı");
});

test("normalizeImportedJsonProgram parses Gemini schedule array format with week range and total_hours", () => {
  const geminiJson = `\`\`\`json
{
  "student": "Furkan",
  "week": "2026-10-27_to_2026-11-01",
  "schedule": [
    {
      "date": "2026-10-27",
      "day": "Salı",
      "total_hours": 6.5,
      "tasks": [
        {
          "subject": "TYT Matematik",
          "topic": "Problemler Deneme 1",
          "source": "Acil Problemler Denemesi",
          "duration_minutes": 30
        },
        {
          "lesson": "AYT Matematik",
          "task": "Trigonometri 36-40. Bölüm",
          "book": "345 Allstar AYT Mat",
          "hours": 2
        }
      ]
    }
  ]
}
\`\`\``;

  const parsed = normalizeImportedJsonProgram(geminiJson, "2026-10-26");
  assert.ok(parsed.weeks["2026-10-26"]);
  assert.equal(parsed.weeks["2026-10-26"].scheduleMeta.salı.duration, "6 saat 30 dakika");
  assert.equal(parsed.weeks["2026-10-26"].program.salı.length, 2);
  assert.equal(parsed.weeks["2026-10-26"].program.salı[0].title, "Problemler Deneme 1");
  assert.equal(parsed.weeks["2026-10-26"].program.salı[0].sub, "Acil Problemler Denemesi");
  assert.equal(parsed.weeks["2026-10-26"].program.salı[0].duration, "30 dakika");
  assert.equal(parsed.weeks["2026-10-26"].program.salı[1].duration, "2 saat");
});
