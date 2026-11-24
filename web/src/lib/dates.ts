const WEEKDAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'] as const;
const MONTHS = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
] as const;

/** Local calendar date -> yyyy-MM-dd. */
export function toDateKey(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/** yyyy-MM-dd -> local Date at noon, which is DST-safe for day arithmetic. */
export function fromDateKey(key: string): Date {
  const [year, month, day] = key.split('-').map(Number);
  return new Date(year, month - 1, day, 12, 0, 0, 0);
}

export function todayKey(now: Date = new Date()): string {
  return toDateKey(now);
}

export function addDays(key: string, days: number): string {
  const date = fromDateKey(key);
  date.setDate(date.getDate() + days);
  return toDateKey(date);
}

/** ISO weekday: 1 = Monday … 7 = Sunday. */
export function weekdayOf(key: string): number {
  const day = fromDateKey(key).getDay();
  return day === 0 ? 7 : day;
}

export function isWeekend(key: string): boolean {
  return weekdayOf(key) >= 6;
}

export function daysAgo(today: string, days: number): string {
  return addDays(today, -days);
}

/** Inclusive range, oldest first. */
export function dateRange(start: string, end: string): string[] {
  const result: string[] = [];
  let cursor = start;
  while (cursor <= end) {
    result.push(cursor);
    cursor = addDays(cursor, 1);
  }
  return result;
}

/** Oldest-first list of `count` days ending today. */
export function lastDays(today: string, count: number): string[] {
  return dateRange(addDays(today, -(count - 1)), today);
}

export function weekdayLabel(weekday: number): string {
  return WEEKDAYS[weekday - 1] ?? '';
}

export function weekdayInitial(weekday: number): string {
  return weekdayLabel(weekday).charAt(0);
}

export function formatFullDate(key: string): string {
  const date = fromDateKey(key);
  return `${weekdayLabel(weekdayOf(key))}, ${date.getDate()} ${MONTHS[date.getMonth()]} ${date.getFullYear()}`;
}

export function formatShortDate(key: string): string {
  const date = fromDateKey(key);
  return `${date.getDate()} ${MONTHS[date.getMonth()]}`;
}

export function monthLabel(key: string): string {
  return MONTHS[fromDateKey(key).getMonth()] ?? '';
}

export function isFuture(key: string, today: string): boolean {
  return key > today;
}

export const ALL_WEEKDAYS = [1, 2, 3, 4, 5, 6, 7] as const;

export function formatWeekdays(weekdays: number[]): string {
  const sorted = [...weekdays].filter((day) => day >= 1 && day <= 7).sort((a, b) => a - b);
  if (sorted.length === 7) return 'Every day';
  if (sorted.length === 5 && sorted.every((day, i) => day === i + 1)) return 'Weekdays';
  if (sorted.length === 2 && sorted[0] === 6 && sorted[1] === 7) return 'Weekends';
  if (sorted.length === 0) return 'Not scheduled';
  return sorted.map((day) => WEEKDAYS[day - 1]).join(', ');
}
