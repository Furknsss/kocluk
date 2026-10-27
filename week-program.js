export const INITIAL_CAMP_WEEK_KEY = "2026-10-12";
export const PROVIDED_PROGRAM_WEEK_KEY = "2026-10-19";
export const FOLLOWING_WEEK_PROGRAM_KEY = "2026-10-26";

const CANONICAL_DAYS = ["pazartesi", "salı", "çarşamba", "perşembe", "cuma", "cumartesi", "pazar"];

const DAY_ALIASES = new Map([
  ["pazartesi", "pazartesi"],
  ["pzt", "pazartesi"],
  ["monday", "pazartesi"],
  ["mon", "pazartesi"],
  ["salı", "salı"],
  ["sali", "salı"],
  ["sal", "salı"],
  ["tuesday", "salı"],
  ["tue", "salı"],
  ["çarşamba", "çarşamba"],
  ["carsamba", "çarşamba"],
  ["çar", "çarşamba"],
  ["car", "çarşamba"],
  ["wednesday", "çarşamba"],
  ["wed", "çarşamba"],
  ["perşembe", "perşembe"],
  ["persembe", "perşembe"],
  ["per", "perşembe"],
  ["thursday", "perşembe"],
  ["thu", "perşembe"],
  ["cuma", "cuma"],
  ["cum", "cuma"],
  ["friday", "cuma"],
  ["fri", "cuma"],
  ["cumartesi", "cumartesi"],
  ["cmt", "cumartesi"],
  ["saturday", "cumartesi"],
  ["sat", "cumartesi"],
  ["pazar", "pazar"],
  ["paz", "pazar"],
  ["sunday", "pazar"],
  ["sun", "pazar"]
]);

export function resolveCanonicalDayKey(rawDay) {
  if (!rawDay) return null;
  const normalized = String(rawDay).trim().toLocaleLowerCase("tr-TR");
  if (DAY_ALIASES.has(normalized)) return DAY_ALIASES.get(normalized);
  for (const [alias, canonical] of DAY_ALIASES.entries()) {
    if (alias.length >= 4 && normalized.includes(alias)) return canonical;
  }
  return null;
}

export function getMondayWeekKeyFromDateString(rawText) {
  const match = String(rawText || "").match(/(\d{4})-(\d{2})-(\d{2})/);
  if (!match) return null;
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  if (!year || month < 1 || month > 12 || day < 1 || day > 31) return null;
  const date = new Date(year, month - 1, day);
  const mondayOffset = (date.getDay() + 6) % 7;
  date.setDate(date.getDate() - mondayOffset);
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${m}-${d}`;
}

export function resolveDayKeyFromDateString(rawText) {
  const match = String(rawText || "").match(/(\d{4})-(\d{2})-(\d{2})/);
  if (!match) return null;
  const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
  const mondayIndex = (date.getDay() + 6) % 7;
  return CANONICAL_DAYS[mondayIndex] || null;
}

export function formatMinutesTr(totalMinutes) {
  const rounded = Math.round(Number(totalMinutes) || 0);
  if (rounded <= 0) return "";
  const hours = Math.floor(rounded / 60);
  const minutes = rounded % 60;
  if (hours === 0) return `${minutes} dakika`;
  if (minutes === 0) return `${hours} saat`;
  return `${hours} saat ${minutes} dakika`;
}

export function extractDurationText(obj) {
  if (!obj || typeof obj !== "object") return "";
  const hourVal =
    obj.hours ?? obj.duration_hours ?? obj.durationHours ?? obj.total_hours ?? obj.totalHours ?? obj.saat ?? obj.toplam_saat ?? obj.toplamSaat;
  if (typeof hourVal === "number" && Number.isFinite(hourVal) && hourVal > 0) {
    return formatMinutesTr(hourVal * 60);
  }
  if (typeof hourVal === "string" && hourVal.trim()) {
    const num = Number(hourVal.replace(",", "."));
    if (Number.isFinite(num) && num > 0) return formatMinutesTr(num * 60);
    return hourVal.trim();
  }

  const minVal =
    obj.minutes ?? obj.duration_minutes ?? obj.durationMinutes ?? obj.total_minutes ?? obj.totalMinutes ?? obj.dk ?? obj.dakika ?? obj.sure_dk;
  if (typeof minVal === "number" && Number.isFinite(minVal) && minVal > 0) {
    return formatMinutesTr(minVal);
  }
  if (typeof minVal === "string" && minVal.trim()) {
    const num = Number(minVal.replace(",", "."));
    if (Number.isFinite(num) && num > 0) return formatMinutesTr(num);
    return minVal.trim();
  }

  const rawDur =
    obj.duration ?? obj.sure ?? obj.süre ?? obj.hedefSure ?? obj.hedef_sure ?? obj.estimated_time ?? obj.estimatedTime ?? obj.totalDuration ?? obj.toplamSure ?? obj.toplam_sure;
  if (typeof rawDur === "number" && Number.isFinite(rawDur) && rawDur > 0) {
    return formatMinutesTr(rawDur <= 12 ? rawDur * 60 : rawDur);
  }
  if (typeof rawDur === "string" && rawDur.trim()) {
    const trimmed = rawDur.trim();
    const num = Number(trimmed.replace(",", "."));
    if (Number.isFinite(num) && num > 0) {
      return formatMinutesTr(num <= 12 ? num * 60 : num);
    }
    return trimmed;
  }

  if (typeof obj.time === "string" && obj.time.trim() && !/^\d{1,2}:\d{2}\s*-\s*\d{1,2}:\d{2}$/.test(obj.time.trim())) {
    return obj.time.trim();
  }
  return "";
}

export function normalizeTaskItem(item) {
  if (!item) return null;
  if (typeof item === "string") {
    const title = item.trim();
    return title ? { title, sub: "", duration: "", status: "Bekliyor", done: false } : null;
  }
  if (Array.isArray(item)) {
    const [rawTitle, rawSub = "", rawDuration = "", rawDone = false] = item;
    const title = String(rawTitle || "").trim();
    const sub = String(rawSub || "").trim();
    if (!title && !sub) return null;
    const duration = String(rawDuration || "").trim();
    const done = typeof rawDone === "boolean"
      ? rawDone
      : /tamam|bitti|completed|done|true/i.test(String(rawDone || ""));
    return {
      title: title || sub,
      sub: title && sub ? sub : "",
      duration,
      status: done ? "Tamamlandı" : "Bekliyor",
      done
    };
  }
  if (typeof item === "object") {
    const primaryTitle = String(
      item.title ?? item.task ?? item.topic ?? item.konu ?? item.gorev ?? item.görev ?? item.baslik ?? item.başlık ??
      item.content ?? item.icerik ?? item.içerik ?? item.activity ?? item.etkinlik ?? item.description ??
      item.aciklama ?? item.açıklama ?? item.name ?? item.test ?? item.deneme ?? item.exam ?? item.study ?? ""
    ).trim();
    const subjectName = String(
      item.subject ?? item.lesson ?? item.course ?? item.ders ?? item.branch ?? item.brans ?? item.branş ?? item.category ?? item.kategori ?? ""
    ).trim();
    const sourceName = String(
      item.sub ?? item.source ?? item.resource ?? item.book ?? item.books ?? item.kaynak ?? item.kaynaklar ??
      item.soruBankasi ?? item.soru_bankasi ?? item.publisher ?? item.yayin ?? item.yayın ?? item.material ??
      item.materyal ?? item.subtitle ?? item.altBaslik ?? item.alt_baslik ?? item.details ?? item.detay ?? ""
    ).trim();

    let title = primaryTitle || subjectName;
    let sub = "";
    if (primaryTitle) {
      if (sourceName) {
        sub = sourceName;
      } else if (subjectName && subjectName !== primaryTitle) {
        sub = subjectName;
      }
    } else if (subjectName && sourceName) {
      sub = sourceName;
    }

    const timeSlot = String(item.time_slot ?? item.timeSlot ?? item.saat_araligi ?? "").trim() ||
      (typeof item.time === "string" && /^\d{1,2}:\d{2}\s*-\s*\d{1,2}:\d{2}$/.test(item.time.trim()) ? item.time.trim() : "");
    if (timeSlot && title && !title.includes(timeSlot)) {
      title = `[${timeSlot}] ${title}`;
    }

    if (!title && !sub) {
      const ignoredKeys = new Set([
        "id", "day", "gun", "gün", "date", "tarih", "status", "durum", "done", "completed", "tamamlandi",
        "duration", "sure", "süre", "hours", "minutes", "total_hours", "totalHours", "time", "time_slot",
        "goal", "hedef", "note", "not"
      ]);
      const stringValues = Object.entries(item)
        .filter(([k, v]) => !ignoredKeys.has(k) && typeof v === "string" && v.trim())
        .map(([, v]) => v.trim());
      if (stringValues.length > 0) {
        title = stringValues[0];
        sub = stringValues[1] || "";
      }
    }

    if (!title && !sub) return null;

    const duration = extractDurationText(item);
    const goal = String(item.goal ?? item.hedef ?? item.soru ?? item.questions ?? item.target_questions ?? "").trim();
    const note = String(item.note ?? item.not ?? item.notes ?? item.notlar ?? "").trim();
    const rawStatus = String(item.status ?? item.durum ?? item.state ?? "").trim();
    const done = typeof item.done === "boolean"
      ? item.done
      : typeof item.tamamlandi === "boolean"
        ? item.tamamlandi
        : typeof item.tamamlandı === "boolean"
          ? item.tamamlandı
          : typeof item.completed === "boolean"
            ? item.completed
            : typeof item.is_completed === "boolean"
              ? item.is_completed
              : typeof item.isCompleted === "boolean"
                ? item.isCompleted
                : /tamam|bitti|completed|done/i.test(rawStatus);
    const status = done ? "Tamamlandı" : (rawStatus || "Bekliyor");
    const normalized = {
      title: title || sub,
      sub: title && sub ? sub : "",
      duration,
      status,
      done
    };
    if (goal) normalized.goal = goal;
    if (note) normalized.note = note;
    return normalized;
  }
  return null;
}

export function cleanRawJsonText(rawInput) {
  if (typeof rawInput !== "string") return rawInput;
  let cleaned = rawInput.trim();
  cleaned = cleaned.replace(/^```(?:json)?\s*/i, "").replace(/\s*```$/i, "").trim();
  const firstBrace = cleaned.indexOf("{");
  const firstBracket = cleaned.indexOf("[");
  let start = -1;
  if (firstBrace !== -1 && firstBracket !== -1) start = Math.min(firstBrace, firstBracket);
  else start = Math.max(firstBrace, firstBracket);
  if (start > 0) {
    const lastBrace = cleaned.lastIndexOf("}");
    const lastBracket = cleaned.lastIndexOf("]");
    const end = Math.max(lastBrace, lastBracket);
    if (end > start) cleaned = cleaned.slice(start, end + 1);
  }
  return cleaned;
}

export function normalizeImportedJsonProgram(rawJson, fallbackWeekKey) {
  const data = typeof rawJson === "string" ? JSON.parse(cleanRawJsonText(rawJson)) : rawJson;
  if (!data || typeof data !== "object") {
    throw new Error("Geçerli bir JSON nesnesi veya dizisi girilmelidir.");
  }

  const emptyProgram = () => Object.fromEntries(CANONICAL_DAYS.map(day => [day, []]));

  // Multi-week object map like { "2026-10-26": { ... }, "2026-11-02": { ... } }
  const rootTarget = data.weeks && typeof data.weeks === "object" && !Array.isArray(data.weeks)
    ? data.weeks
    : data;
  const weekEntryKeys = Object.keys(rootTarget).filter(key => /^\d{4}-\d{2}-\d{2}$/.test(key));
  if (!Array.isArray(rootTarget) && weekEntryKeys.length > 0) {
    const results = {};
    for (const wk of weekEntryKeys) {
      const single = normalizeImportedJsonProgram(rootTarget[wk], wk);
      Object.assign(results, single.weeks);
    }
    return { weeks: results };
  }

  const program = emptyProgram();
  const scheduleMeta = {};
  const providedDays = new Set();

  if (data.scheduleMeta && typeof data.scheduleMeta === "object") {
    for (const [k, v] of Object.entries(data.scheduleMeta)) {
      const dayKey = resolveCanonicalDayKey(k);
      if (dayKey && v && typeof v === "object" && v.duration) {
        scheduleMeta[dayKey] = {
          duration: String(v.duration).trim(),
          durationLabel: String(v.durationLabel || "Toplam süre").trim()
        };
      } else if (dayKey && typeof v === "string" && v.trim()) {
        scheduleMeta[dayKey] = {
          duration: v.trim(),
          durationLabel: "Toplam süre"
        };
      }
    }
  }

  const sourceContainer =
    data.schedule ?? data.program ?? data.weekly_schedule ?? data.weeklySchedule ?? data.weekly_program ??
    data.weeklyProgram ?? data.days ?? data.gunler ?? data.günler ?? data.haftalikProgram ??
    data.haftalıkProgram ?? data.haftalik_program ?? data.plan ?? data.tasks ?? data.gorevler ?? data.görevler ?? data;

  let firstDetectedDateWeekKey = null;

  if (Array.isArray(sourceContainer)) {
    sourceContainer.forEach(entry => {
      if (Array.isArray(entry)) {
        const firstDay = resolveCanonicalDayKey(entry[0]);
        const secondDay = resolveCanonicalDayKey(entry[1]);
        if (secondDay && entry.length >= 3) {
          providedDays.add(secondDay);
          const task = normalizeTaskItem({
            title: entry[2],
            sub: entry[3],
            status: entry[4],
            duration: entry[5],
            note: entry[6]
          });
          if (task) program[secondDay].push(task);
        } else if (firstDay && entry.length >= 2) {
          providedDays.add(firstDay);
          const task = normalizeTaskItem(entry.slice(1));
          if (task) program[firstDay].push(task);
        }
        return;
      }
      if (entry && typeof entry === "object") {
        const entryDateStr = entry.date ?? entry.tarih ?? "";
        if (!firstDetectedDateWeekKey && entryDateStr) {
          firstDetectedDateWeekKey = getMondayWeekKeyFromDateString(entryDateStr);
        }
        const dayKey =
          resolveCanonicalDayKey(entry.day ?? entry.gun ?? entry.gün ?? entry.dayName ?? entry.day_name) ||
          resolveDayKeyFromDateString(entryDateStr);
        if (!dayKey) return;
        providedDays.add(dayKey);

        const dayDuration = extractDurationText(entry);
        const dayTasks =
          entry.tasks ?? entry.gorevler ?? entry.görevler ?? entry.items ?? entry.activities ??
          entry.etkinlikler ?? entry.lessons ?? entry.dersler ?? entry.plan ?? entry.list ??
          Object.values(entry).find(v => Array.isArray(v));

        if (Array.isArray(dayTasks)) {
          if (dayDuration && !scheduleMeta[dayKey]) {
            scheduleMeta[dayKey] = { duration: dayDuration, durationLabel: "Toplam süre" };
          }
          dayTasks.forEach(t => {
            const normalized = normalizeTaskItem(t);
            if (normalized) program[dayKey].push(normalized);
          });
        } else {
          const normalized = normalizeTaskItem(entry);
          if (normalized) program[dayKey].push(normalized);
        }
      }
    });
  } else if (sourceContainer && typeof sourceContainer === "object") {
    for (const [rawKey, value] of Object.entries(sourceContainer)) {
      const dayKey = resolveCanonicalDayKey(rawKey) || resolveDayKeyFromDateString(rawKey);
      if (!dayKey) continue;
      providedDays.add(dayKey);
      if (!firstDetectedDateWeekKey) {
        firstDetectedDateWeekKey = getMondayWeekKeyFromDateString(rawKey);
      }
      if (Array.isArray(value)) {
        value.forEach(item => {
          const normalized = normalizeTaskItem(item);
          if (normalized) program[dayKey].push(normalized);
        });
      } else if (value && typeof value === "object") {
        if (!firstDetectedDateWeekKey && (value.date || value.tarih)) {
          firstDetectedDateWeekKey = getMondayWeekKeyFromDateString(value.date || value.tarih);
        }
        const dayDuration = extractDurationText(value);
        if (dayDuration && !scheduleMeta[dayKey]) {
          scheduleMeta[dayKey] = {
            duration: dayDuration,
            durationLabel: String(value.durationLabel || "Toplam süre").trim()
          };
        }
        const dayTasks =
          value.tasks ?? value.gorevler ?? value.görevler ?? value.items ?? value.activities ??
          value.etkinlikler ?? value.lessons ?? value.dersler ?? value.plan ?? value.list ??
          Object.values(value).find(v => Array.isArray(v)) ?? [];
        if (Array.isArray(dayTasks)) {
          dayTasks.forEach(item => {
            const normalized = normalizeTaskItem(item);
            if (normalized) program[dayKey].push(normalized);
          });
        }
      }
    }
  }

  const totalTasks = CANONICAL_DAYS.reduce((sum, day) => sum + program[day].length, 0);
  if (totalTasks === 0) {
    throw new Error("JSON içinde geçerli gün ve görev verisi bulunamadı.");
  }

  const rawWeekField = String(data.weekKey ?? data.week ?? data.hafta ?? data.date_range ?? data.dateRange ?? "").trim();
  const targetWeekKey =
    getMondayWeekKeyFromDateString(rawWeekField) ||
    firstDetectedDateWeekKey ||
    (/^\d{4}-\d{2}-\d{2}$/.test(rawWeekField) ? rawWeekField : "") ||
    fallbackWeekKey;

  return {
    weeks: {
      [targetWeekKey]: {
        program,
        scheduleMeta,
        providedDays: Array.from(providedDays)
      }
    }
  };
}

export function mergeImportedWeekIntoExisting(existingWeek, incomingWeek) {
  if (!existingWeek || !existingWeek.program) {
    return {
      program: JSON.parse(JSON.stringify(incomingWeek.program)),
      scheduleMeta: JSON.parse(JSON.stringify(incomingWeek.scheduleMeta || {}))
    };
  }
  const providedSet = new Set(
    Array.isArray(incomingWeek.providedDays) && incomingWeek.providedDays.length > 0
      ? incomingWeek.providedDays
      : CANONICAL_DAYS.filter(d => (incomingWeek.program?.[d] || []).length > 0)
  );
  const mergedProgram = Object.fromEntries(CANONICAL_DAYS.map(day => [day, []]));
  const mergedMeta = { ...(existingWeek.scheduleMeta || {}) };

  CANONICAL_DAYS.forEach(dayKey => {
    const existingDayTasks = Array.isArray(existingWeek.program[dayKey]) ? existingWeek.program[dayKey] : [];
    if (!providedSet.has(dayKey)) {
      mergedProgram[dayKey] = JSON.parse(JSON.stringify(existingDayTasks));
      return;
    }
    const incomingDayTasks = Array.isArray(incomingWeek.program[dayKey]) ? incomingWeek.program[dayKey] : [];
    mergedProgram[dayKey] = incomingDayTasks.map(task => {
      const copy = { ...task };
      if (!copy.done) {
        const normalizedTitle = (copy.title || "").trim().toLocaleLowerCase("tr-TR");
        const matchedExisting = existingDayTasks.find(
          candidate =>
            candidate &&
            (candidate.title || "").trim().toLocaleLowerCase("tr-TR") === normalizedTitle
        );
        if (matchedExisting && matchedExisting.done) {
          copy.done = true;
          copy.status = "Tamamlandı";
          if (!copy.note && matchedExisting.note) copy.note = matchedExisting.note;
        }
      }
      return copy;
    });
    if (incomingWeek.scheduleMeta?.[dayKey]) {
      mergedMeta[dayKey] = { ...incomingWeek.scheduleMeta[dayKey] };
    }
  });

  return {
    program: mergedProgram,
    scheduleMeta: mergedMeta
  };
}

export function getTaskSignatureList(programByDay) {
  if (!programByDay || typeof programByDay !== "object") return [];
  return CANONICAL_DAYS.flatMap(day =>
    (programByDay[day] || []).map(task =>
      JSON.stringify([day, (task.title || "").trim(), (task.sub || "").trim()])
    )
  );
}

export function doesProgramMatchExpected(candidateProgram, expectedProgram) {
  const expectedSignatures = getTaskSignatureList(expectedProgram);
  const candidateSignatures = getTaskSignatureList(candidateProgram);
  if (expectedSignatures.length === 0 || expectedSignatures.length !== candidateSignatures.length) {
    return false;
  }
  const candidateSet = new Set(candidateSignatures);
  return expectedSignatures.every(sig => candidateSet.has(sig));
}

export function mergeStatusIntoExpectedProgram(expectedProgram, existingProgram) {
  const cloned = JSON.parse(JSON.stringify(expectedProgram));
  if (!existingProgram || typeof existingProgram !== "object") return cloned;

  for (const [day, tasks] of Object.entries(cloned)) {
    const existingTasks = Array.isArray(existingProgram[day]) ? existingProgram[day] : [];
    tasks.forEach(task => {
      const match = existingTasks.find(
        candidate =>
          candidate &&
          (candidate.title || "").trim() === (task.title || "").trim() &&
          (candidate.sub || "").trim() === (task.sub || "").trim()
      );
      if (match && typeof match.done === "boolean") {
        task.done = match.done;
        task.status = match.done ? "Tamamlandı" : (match.status || "Bekliyor");
        if (match.note) task.note = match.note;
      }
    });
  }
  return cloned;
}
