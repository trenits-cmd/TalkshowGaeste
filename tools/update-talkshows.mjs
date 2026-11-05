#!/usr/bin/env node
/**
 * Holt echte Gäste- und Themenangaben aus offiziellen Senderquellen und
 * schreibt sie in data/talkshows.json.
 *
 * Quellen:
 *  - ZDF-Presseportal (Markus Lanz, maybrit illner)
 *  - Das Erste / WDR (maischberger, Kölner Treff)
 *  - NDR (NDR Talk Show, Caren Miosga)
 *  - MDR (Riverboat)
 *  - Radio Bremen (3nach9)
 *
 * Das Skript hat keine externen Abhängigkeiten und läuft mit Node 18+.
 */

import { readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const DATA_FILE = path.join(ROOT, "data", "talkshows.json");
const ASSET_FILE = path.join(ROOT, "app", "src", "main", "assets", "talkshows.json");

const USER_AGENT =
  "TalkshowGaeste/1.1 (+https://github.com/trenits-cmd/TalkshowGaeste)";

const MONTHS = {
  januar: 1,
  februar: 2,
  maerz: 3,
  "märz": 3,
  april: 4,
  mai: 5,
  juni: 6,
  juli: 7,
  august: 8,
  september: 9,
  oktober: 10,
  november: 11,
  dezember: 12,
};

const SOURCES = {
  markusLanz: "https://presseportal.zdf.de/pressemappe/markus-lanz-gaeste-und-themen",
  maybritIllner:
    "https://presseportal.zdf.de/pressemappe/maybrit-illner-gaeste-themen",
  maischberger:
    "https://www.daserste.de/information/talk/maischberger/sendung/maischberger-1084.html",
  koelnerTreff: "https://www1.wdr.de/fernsehen/koelner-treff/index.html",
  ndrTalkShow: "https://www.ndr.de/fernsehen/sendungen/ndr_talk_show/index.html",
  miosgaRueckschau:
    "https://www.ndr.de/fernsehen/sendungen/caren-miosga/rueckschau",
  riverboat: "https://www.mdr.de/riverboat/",
  dreiNachNeun: "https://www.radiobremen.de/programm/3nach9/",
};

const SHOW_TIMES = {
  "markus-lanz": "23:15",
  "maybrit-illner": "22:15",
  "caren-miosga": "21:45",
  maischberger: "22:50",
  "koelner-treff": "22:00",
  "ndr-talk-show": "22:00",
  riverboat: "22:00",
  "3nach9": "22:00",
};

function log(message) {
  console.log(message);
}

function warning(message) {
  console.log(`::warning::${message}`);
}

async function fetchText(url, timeoutMs = 25000) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, {
      redirect: "follow",
      signal: controller.signal,
      headers: {
        "user-agent": USER_AGENT,
        accept: "text/html,application/xhtml+xml",
        "accept-language": "de-DE,de;q=0.9",
      },
    });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status} für ${url}`);
    }
    return await response.text();
  } finally {
    clearTimeout(timer);
  }
}

function decodeEntities(value) {
  return value
    .replace(/&nbsp;/gi, " ")
    .replace(/&amp;/gi, "&")
    .replace(/&quot;/gi, '"')
    .replace(/&#39;|&apos;/gi, "'")
    .replace(/&lt;/gi, "<")
    .replace(/&gt;/gi, ">")
    .replace(/&uuml;/gi, "ü")
    .replace(/&ouml;/gi, "ö")
    .replace(/&auml;/gi, "ä")
    .replace(/&Uuml;/gi, "Ü")
    .replace(/&Ouml;/gi, "Ö")
    .replace(/&Auml;/gi, "Ä")
    .replace(/&szlig;/gi, "ß")
    .replace(/&#x([0-9a-f]+);/gi, (_, hex) =>
      String.fromCodePoint(parseInt(hex, 16)),
    )
    .replace(/&#(\d+);/g, (_, dec) => String.fromCodePoint(Number(dec)));
}

function stripTags(value) {
  return decodeEntities(value.replace(/<[^>]*>/g, " "));
}

/** HTML in einzelne, nicht-leere Textzeilen zerlegen. */
function htmlToLines(html) {
  const withoutNoise = html
    .replace(/<script[\s\S]*?<\/script>/gi, " ")
    .replace(/<style[\s\S]*?<\/style>/gi, " ")
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<\/(p|div|li|h[1-6]|tr|section|article)>/gi, "\n");
  return stripTags(withoutNoise)
    .split(/\r?\n/)
    .map((line) => line.replace(/\s+/g, " ").trim())
    .filter(Boolean);
}

function extractHeadings(html, level = 2, onlyWithId = false) {
  const regex = onlyWithId
    ? new RegExp(`<h${level}[^>]*\\bid="[^"]*"[^>]*>([\\s\\S]*?)<\\/h${level}>`, "gi")
    : new RegExp(`<h${level}[^>]*>([\\s\\S]*?)<\\/h${level}>`, "gi");
  const result = [];
  let match;
  while ((match = regex.exec(html)) !== null) {
    const inner = match[1];
    if (onlyWithId && /<a\b/i.test(inner)) continue;
    const text = stripTags(inner).replace(/\s+/g, " ").trim();
    if (text) result.push(text);
  }
  return result;
}

function pad(value) {
  return String(value).padStart(2, "0");
}

function isoDate(year, month, day) {
  if (!year || !month || !day) return null;
  const date = new Date(Date.UTC(year, month - 1, day));
  if (
    date.getUTCFullYear() !== year ||
    date.getUTCMonth() !== month - 1 ||
    date.getUTCDate() !== day
  ) {
    return null;
  }
  return `${year}-${pad(month)}-${pad(day)}`;
}

function addDays(iso, days) {
  const [year, month, day] = iso.split("-").map(Number);
  const date = new Date(Date.UTC(year, month - 1, day + days));
  return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(
    date.getUTCDate(),
  )}`;
}

function monthNumber(name) {
  return MONTHS[name.trim().toLowerCase()] ?? null;
}

function cleanGuest(value) {
  return value
    .replace(/^[-•*·]\s*/, "")
    .replace(/\s+/g, " ")
    .replace(/[;,.]+$/, "")
    .trim();
}

function looksLikeGuest(value) {
  if (!value || value.length < 2 || value.length > 160) return false;
  if (/^(gäste|gaeste|thema|sendedatum|mehr|zurück|quelle|bild|video)$/i.test(value)) {
    return false;
  }
  return /\p{L}/u.test(value);
}

function splitGuestList(value) {
  const parts = value
    .replace(/\s+(und|sowie|&)\s+/gi, ", ")
    .split(/\s*,\s*/)
    .map(cleanGuest)
    .filter(looksLikeGuest);

  // "Reinhold Messner und Ehefrau Diane" soll ein Eintrag bleiben.
  const result = [];
  for (const part of parts) {
    if (/^(Ehefrau|Ehemann|Partnerin|Partner)\b/i.test(part) && result.length > 0) {
      result[result.length - 1] += ` und ${part}`;
    } else {
      result.push(part);
    }
  }
  return result;
}

function guestWithRole(value) {
  const line = cleanGuest(value);
  const comma = line.indexOf(",");
  if (comma > 0 && !line.slice(0, comma).includes("(")) {
    const name = line.slice(0, comma).trim();
    const role = line.slice(comma + 1).trim();
    const words = name.split(/\s+/);
    if (words.length <= 5 && role.length > 0) {
      return `${name} (${role})`;
    }
  }
  return line;
}

function normalizeGuests(guests) {
  const seen = new Set();
  const result = [];
  for (const guest of guests) {
    const value = cleanGuest(guest);
    if (!looksLikeGuest(value)) continue;
    const key = value.toLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);
    result.push(value);
  }
  return result;
}

function makeEpisode(showId, date, time, topic, guests) {
  if (!date) return null;
  const cleaned = normalizeGuests(guests || []);
  if (cleaned.length === 0) return null;
  return {
    showId,
    date,
    time: time || SHOW_TIMES[showId] || "22:00",
    topic: (topic || "").trim(),
    guests: cleaned,
  };
}

function germanDate(day, monthName, year) {
  const month = monthNumber(monthName);
  if (!month) return null;
  return isoDate(year, month, Number(day));
}

function inferYear(day, monthName, todayIso) {
  const month = monthNumber(monthName);
  if (!month) return null;
  const today = new Date(`${todayIso}T00:00:00Z`);
  let year = today.getUTCFullYear();
  let candidate = isoDate(year, month, Number(day));
  if (!candidate) return null;
  const diffDays = (Date.parse(`${candidate}T00:00:00Z`) - today.getTime()) / 86400000;
  if (diffDays < -60) {
    year += 1;
    candidate = isoDate(year, month, Number(day));
  }
  return candidate;
}

// ---------------------------------------------------------------------------
// ZDF: Markus Lanz
// ---------------------------------------------------------------------------
async function scrapeMarkusLanz(todayIso) {
  const html = await fetchText(SOURCES.markusLanz);
  const lines = htmlToLines(html);
  const start = lines.findIndex((line) => /^Nächste Ausgabe von "Markus Lanz"/i.test(line));
  if (start < 0) throw new Error("Markus-Lanz-Ausgabe nicht gefunden");

  const dateLine = lines.slice(start + 1, start + 4).find((line) =>
    /\d{1,2}\.\s*\w+\s*\d{4}/.test(line),
  );
  const dateMatch = dateLine?.match(
    /(\d{1,2})\.\s*([A-Za-zÄÖÜäöüß]+)\s*(\d{4})[^0-9]*?(\d{1,2})(?:[.:](\d{2}))?\s*Uhr/i,
  );
  if (!dateMatch) throw new Error(`Datum nicht lesbar: ${dateLine}`);

  const day = Number(dateMatch[1]);
  const month = dateMatch[2];
  const year = Number(dateMatch[3]);
  let hour = Number(dateMatch[4]);
  const minute = Number(dateMatch[5] || 0);
  let date = germanDate(day, month, year);
  if (hour === 24) {
    hour = 0;
    date = addDays(date, 1);
  }
  const time = `${pad(hour)}:${pad(minute)}`;

  const guestsStart = lines.findIndex(
    (line, index) => index > start && /^Gäste:?$/i.test(line),
  );
  if (guestsStart < 0) throw new Error("Gästeliste nicht gefunden");

  const guests = [];
  for (let i = guestsStart + 1; i < lines.length; i += 1) {
    const line = lines[i];
    if (/^(Sendedatum|Das könnte Sie auch interessieren|ZDF)$/i.test(line)) break;
    if (/^:$/.test(line)) continue;
    guests.push(guestWithRole(line));
  }

  return makeEpisode("markus-lanz", date, time, "", guests);
}

// ---------------------------------------------------------------------------
// ZDF: maybrit illner
// ---------------------------------------------------------------------------
async function scrapeMaybritIllner(todayIso) {
  const html = await fetchText(SOURCES.maybritIllner);
  const lines = htmlToLines(html);
  const start = lines.findIndex((line) =>
    /^Nächste Ausgabe von "maybrit illner"/i.test(line),
  );
  if (start < 0) throw new Error("maybrit-illner-Ausgabe nicht gefunden");

  const dateLine = lines.slice(start + 1, start + 4).find((line) =>
    /\d{1,2}\.\s*\w+\s*\d{4}/.test(line),
  );
  const dateMatch = dateLine?.match(
    /(\d{1,2})\.\s*([A-Za-zÄÖÜäöüß]+)\s*(\d{4})[^0-9]*?(\d{1,2})(?:[.:](\d{2}))?\s*Uhr/i,
  );
  if (!dateMatch) throw new Error(`Datum nicht lesbar: ${dateLine}`);

  const date = germanDate(Number(dateMatch[1]), dateMatch[2], Number(dateMatch[3]));
  const time = `${pad(Number(dateMatch[4]))}:${pad(Number(dateMatch[5] || 0))}`;

  const topicIndex = lines.findIndex(
    (line, index) => index > start && /^Thema\s*:/i.test(line),
  );
  let topic = "";
  let afterTopic = topicIndex;
  if (topicIndex >= 0) {
    const inline = lines[topicIndex].replace(/^Thema\s*:\s*/i, "").trim();
    if (inline && inline !== ":") {
      topic = cleanGuest(inline);
    } else {
      const candidates = lines.slice(topicIndex + 1, topicIndex + 4);
      const topicLine = candidates.find(
        (line) => line !== ":" && !/^Gäste\s*:/i.test(line),
      );
      if (topicLine) topic = cleanGuest(topicLine.replace(/^:\s*/, ""));
      afterTopic = topicLine ? lines.indexOf(topicLine, topicIndex) : topicIndex;
    }
  }

  const guestsStart = lines.findIndex(
    (line, index) =>
      index > (afterTopic < 0 ? start : afterTopic) && /^Gäste\s*:/i.test(line),
  );
  if (guestsStart < 0) throw new Error("Gästeliste nicht gefunden");

  const guests = [];
  const inlineGuests = lines[guestsStart].replace(/^Gäste\s*:\s*/i, "").trim();
  if (inlineGuests && inlineGuests !== ":") guests.push(inlineGuests);
  for (let i = guestsStart + 1; i < lines.length; i += 1) {
    const line = lines[i];
    if (/^(Sendedatum|Das könnte Sie auch interessieren|ZDF)$/i.test(line)) break;
    if (/^:$/.test(line)) continue;
    guests.push(line);
  }

  return makeEpisode("maybrit-illner", date, time, topic, guests);
}

// ---------------------------------------------------------------------------
// ARD / Das Erste: maischberger
// ---------------------------------------------------------------------------
async function scrapeMaischberger(todayIso) {
  const html = await fetchText(SOURCES.maischberger);
  const lines = htmlToLines(html);
  const dateIndex = lines.findIndex((line) =>
    /maischberger am \d{1,2}\.\d{1,2}\.\d{4} um \d{1,2}:\d{2} Uhr/i.test(line),
  );
  if (dateIndex < 0) throw new Error("maischberger-Sendehinweis nicht gefunden");

  const dateMatch = lines[dateIndex].match(
    /maischberger am (\d{1,2})\.(\d{1,2})\.(\d{4}) um (\d{1,2}):(\d{2}) Uhr/i,
  );
  if (!dateMatch) throw new Error("Datum nicht lesbar");

  const date = isoDate(
    Number(dateMatch[3]),
    Number(dateMatch[2]),
    Number(dateMatch[1]),
  );
  const time = `${pad(Number(dateMatch[4]))}:${dateMatch[5]}`;

  const guestLine = lines
    .slice(dateIndex, dateIndex + 4)
    .find((line) => /^Zu Gast bei maischberger:/i.test(line));
  if (!guestLine) throw new Error("Gästeliste nicht gefunden");

  const guests = splitGuestList(guestLine.replace(/^Zu Gast bei maischberger:\s*/i, ""));
  return makeEpisode("maischberger", date, time, "", guests);
}

// ---------------------------------------------------------------------------
// WDR: Kölner Treff
// ---------------------------------------------------------------------------
async function scrapeKoelnerTreff(todayIso) {
  const html = await fetchText(SOURCES.koelnerTreff);
  const lines = htmlToLines(html);

  const dateIndex = lines.findIndex((line) =>
    /^\|\s*\d{1,2}\.\s*[A-Za-zÄÖÜäöüß]+\s*\d{4},\s*\d{1,2}\.\d{2}/.test(line),
  );
  if (dateIndex < 0) throw new Error("Kölner-Treff-Sendehinweis nicht gefunden");

  const dateMatch = lines[dateIndex].match(
    /\|\s*(\d{1,2})\.\s*([A-Za-zÄÖÜäöüß]+)\s*(\d{4}),\s*(\d{1,2})\.(\d{2})/,
  );
  if (!dateMatch) throw new Error("Datum nicht lesbar");

  const date = germanDate(
    Number(dateMatch[1]),
    dateMatch[2],
    Number(dateMatch[3]),
  );
  const time = `${pad(Number(dateMatch[4]))}:${dateMatch[5]}`;

  const candidates = lines
    .slice(Math.max(0, dateIndex - 12), dateIndex + 26)
    .filter((line) => /Zu Gast.*\bsind\b/i.test(line));
  if (candidates.length === 0) throw new Error("Gästeliste nicht gefunden");

  // Auf der Seite stehen mehrere Teaservarianten. Die vollständigste nehmen.
  let guests = [];
  for (const line of candidates) {
    const match = line.match(/Zu Gast.*?\bsind\s+(?:u\.a\.\s+)?(.+)$/i);
    const parsed = match ? splitGuestList(match[1]) : [];
    if (parsed.length > guests.length) guests = parsed;
  }
  return makeEpisode("koelner-treff", date, time, "", guests);
}

// ---------------------------------------------------------------------------
// NDR: NDR Talk Show
// ---------------------------------------------------------------------------
async function scrapeNdrTalkShow(todayIso) {
  const html = await fetchText(SOURCES.ndrTalkShow);
  const linkMatch = html.match(
    /href="([^"]*ndr-talk-show-die-gaeste-am-[^"]+,sendung-\d+\.html)"/i,
  );
  if (!linkMatch) throw new Error("Gäste-Artikel nicht gefunden");

  const articleUrl = new URL(decodeEntities(linkMatch[1]), SOURCES.ndrTalkShow).href;
  const article = await fetchText(articleUrl);
  const lines = htmlToLines(article);

  const dateLine = lines.find((line) =>
    /(Montag|Dienstag|Mittwoch|Donnerstag|Freitag|Samstag|Sonntag),\s*\d{1,2}\.\s*\w+\s*\d{4}/.test(
      line,
    ),
  );
  const dateMatch = dateLine?.match(
    /(Montag|Dienstag|Mittwoch|Donnerstag|Freitag|Samstag|Sonntag),\s*(\d{1,2})\.\s*([A-Za-zÄÖÜäöüß]+)\s*(\d{4})(?:,\s*(\d{1,2})[:.](\d{2}))?/i,
  );
  if (!dateMatch) throw new Error("Sendedatum nicht lesbar");

  const date = germanDate(
    Number(dateMatch[2]),
    dateMatch[3],
    Number(dateMatch[4]),
  );
  const time =
    dateMatch[5] !== undefined
      ? `${pad(Number(dateMatch[5]))}:${dateMatch[6]}`
      : SHOW_TIMES["ndr-talk-show"];

  const bodyStart = article.search(/folgende Gäste:/i);
  const bodyEnd = article.search(/Redaktionsleiter/i);
  const relevant = article.slice(
    bodyStart >= 0 ? bodyStart : 0,
    bodyEnd > bodyStart ? bodyEnd : undefined,
  );
  const guests = extractHeadings(relevant, 2, true);

  return makeEpisode("ndr-talk-show", date, time, "", guests);
}

// ---------------------------------------------------------------------------
// NDR: Caren Miosga (Rückschau)
// ---------------------------------------------------------------------------
async function scrapeCarenMiosga(todayIso) {
  const index = await fetchText(SOURCES.miosgaRueckschau);
  const linkMatch = index.match(/href="([^"]*miosga-sendung-\d+\.html)"/i);
  if (!linkMatch) throw new Error("Miosga-Rückschau nicht gefunden");

  const articleUrl = new URL(decodeEntities(linkMatch[1]), SOURCES.miosgaRueckschau).href;
  const article = await fetchText(articleUrl);
  const lines = htmlToLines(article);

  const dateLine = lines.find((line) =>
    /(Sonntag),\s*\d{1,2}\.\s*\w+\s*\d{4}/.test(line),
  );
  const dateMatch = dateLine?.match(
    /(Sonntag),\s*(\d{1,2})\.\s*([A-Za-zÄÖÜäöüß]+)\s*(\d{4})(?:,\s*(\d{1,2}):(\d{2}))?/i,
  );
  if (!dateMatch) throw new Error("Sendedatum nicht lesbar");

  const date = germanDate(
    Number(dateMatch[2]),
    dateMatch[3],
    Number(dateMatch[4]),
  );
  const time =
    dateMatch[5] !== undefined
      ? `${pad(Number(dateMatch[5]))}:${dateMatch[6]}`
      : SHOW_TIMES["caren-miosga"];

  const h1Match = article.match(/<h1[^>]*>([\s\S]*?)<\/h1>/i);
  const topic = h1Match
    ? stripTags(h1Match[1]).replace(/\s+/g, " ").replace(/\s*\|\s*ndr\.de\s*$/i, "").trim()
    : "";

  const bodyEnd = article.indexOf("Beitrag teilen");
  const relevant = article.slice(0, bodyEnd > 0 ? bodyEnd : undefined);
  const ignored = /^(Hallo Niedersachsen|Der NDR|Service|Themen|NDR Fernsehen)$/i;
  const guests = extractHeadings(relevant, 2, true).filter(
    (name) => !ignored.test(name),
  );

  return makeEpisode("caren-miosga", date, time, topic, guests);
}

// ---------------------------------------------------------------------------
// MDR: Riverboat
// ---------------------------------------------------------------------------
async function scrapeRiverboat(todayIso) {
  const html = await fetchText(SOURCES.riverboat);
  const lines = htmlToLines(html);

  const dateLine = lines.find((line) => /^RIVERBOAT-Gäste vom\s+/i.test(line));
  const dateMatch = dateLine?.match(
    /^RIVERBOAT-Gäste vom\s+(\d{1,2})\.\s*([A-Za-zÄÖÜäöüß]+)/i,
  );
  if (!dateMatch) throw new Error("Riverboat-Sendedatum nicht gefunden");

  const date = inferYear(Number(dateMatch[1]), dateMatch[2], todayIso);

  const guestLine = lines.find((line) => /erwarten[^:]*:\s*/i.test(line));
  if (!guestLine) throw new Error("Riverboat-Gästeliste nicht gefunden");

  const match = guestLine.match(/erwarten[^:]*:\s*(.+)$/i);
  const guests = match ? splitGuestList(match[1]) : [];
  return makeEpisode("riverboat", date, SHOW_TIMES.riverboat, "", guests);
}

// ---------------------------------------------------------------------------
// Radio Bremen: 3nach9
// ---------------------------------------------------------------------------
async function scrapeDreiNachNeun(todayIso) {
  const html = await fetchText(SOURCES.dreiNachNeun);
  const lines = htmlToLines(html);

  const dateLine = lines.find((line) =>
    /^3nach9 am\s+\d{1,2}\.\s*[A-Za-zÄÖÜäöüß]+\s+\d{4}/i.test(line),
  );
  const dateMatch = dateLine?.match(
    /^3nach9 am\s+(\d{1,2})\.\s*([A-Za-zÄÖÜäöüß]+)\s+(\d{4})/i,
  );
  if (!dateMatch) throw new Error("3nach9-Sendedatum nicht gefunden");

  const date = germanDate(
    Number(dateMatch[1]),
    dateMatch[2],
    Number(dateMatch[3]),
  );

  const guestLine = lines.find((line) => /mit folgenden Gästen:\s*/i.test(line));
  if (!guestLine) throw new Error("3nach9-Gästeliste nicht gefunden");

  const match = guestLine.match(/mit folgenden Gästen:\s*(.+)$/i);
  const guests = match ? splitGuestList(match[1]) : [];
  return makeEpisode("3nach9", date, SHOW_TIMES["3nach9"], "", guests);
}

// ---------------------------------------------------------------------------

function todayInBerlin() {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "Europe/Berlin",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date());
}

function episodeKey(episode) {
  return `${episode.showId}\u0000${episode.date}`;
}

async function main() {
  const todayIso = todayInBerlin();
  const document = JSON.parse(readFileSync(DATA_FILE, "utf8"));
  const shows = Array.isArray(document.shows) ? document.shows : [];
  if (shows.length === 0) throw new Error("data/talkshows.json enthält keine Shows");

  const previousSource = String(document.source || "");
  const previousEpisodes = Array.isArray(document.episodes) ? document.episodes : [];
  const previousWasDemo = /beispiel/i.test(previousSource);

  const episodes = new Map();
  if (!previousWasDemo) {
    const cutoff = addDays(todayIso, -21);
    for (const episode of previousEpisodes) {
      if (!episode?.showId || !episode?.date) continue;
      if (episode.date < cutoff) continue;
      episodes.set(episodeKey(episode), episode);
    }
  }

  const scrapers = [
    ["Markus Lanz (ZDF)", scrapeMarkusLanz],
    ["maybrit illner (ZDF)", scrapeMaybritIllner],
    ["maischberger (ARD)", scrapeMaischberger],
    ["Kölner Treff (WDR)", scrapeKoelnerTreff],
    ["NDR Talk Show (NDR)", scrapeNdrTalkShow],
    ["Caren Miosga (NDR)", scrapeCarenMiosga],
    ["Riverboat (MDR)", scrapeRiverboat],
    ["3nach9 (Radio Bremen)", scrapeDreiNachNeun],
  ];

  let successCount = 0;
  const results = await Promise.allSettled(
    scrapers.map(async ([name, scraper]) => {
      const episode = await scraper(todayIso);
      return { name, episode };
    }),
  );

  for (const result of results) {
    if (result.status === "rejected") {
      warning(`${result.reason?.message || result.reason}`);
      continue;
    }
    const { name, episode } = result.value;
    if (!episode) {
      warning(`${name}: keine verwertbaren Gäste gefunden`);
      continue;
    }
    successCount += 1;
    episodes.set(episodeKey(episode), episode);
    log(
      `[ok] ${name}: ${episode.date} ${episode.time} – ${episode.guests.length} Gäste`,
    );
  }

  if (successCount === 0) {
    warning("Keine Quelle lieferte Gäste – data/talkshows.json bleibt unverändert.");
    return;
  }

  const sortedEpisodes = [...episodes.values()].sort((a, b) =>
    `${a.date}\u0000${a.time}\u0000${a.showId}`.localeCompare(
      `${b.date}\u0000${b.time}\u0000${b.showId}`,
    ),
  );

  const changed =
    JSON.stringify(sortedEpisodes) !== JSON.stringify(previousEpisodes);
  if (!changed) {
    log("Keine Änderungen – Datei bleibt unverändert.");
    return;
  }

  const updated = {
    updated: new Date().toISOString(),
    source: "Automatisch aus offiziellen Senderangaben (ZDF, ARD/WDR, NDR, MDR, Radio Bremen)",
    shows,
    episodes: sortedEpisodes,
  };

  const json = `${JSON.stringify(updated, null, 2)}\n`;
  writeFileSync(DATA_FILE, json, "utf8");
  writeFileSync(ASSET_FILE, json, "utf8");
  log(
    `data/talkshows.json aktualisiert: ${sortedEpisodes.length} Episoden, ${successCount} Quellen.`,
  );
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
