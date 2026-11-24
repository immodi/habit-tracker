import { addDays, fromDateKey, isFuture, toDateKey, weekdayOf } from './dates';
import { isFlexible } from './schedule';
import type { HabitSchedule, ScheduleType } from './types';

export type DayStatus = 'done' | 'missed' | 'empty' | 'off' | 'future';

export interface DayCell {
  date: string;
  status: DayStatus;
  scheduled: boolean;
  done: boolean;
}

export interface StreakResult {
  value: number;
  /** True when the streak touches the edge of the calculation window. */
  capped: boolean;
}

export interface CompletionResult {
  done: number;
  /** Scheduled days (weekday habits) or elapsed periods (flexible habits). */
  scheduled: number;
  /** 0..1, or null when there was nothing to measure. */
  rate: number | null;
}

export interface PeriodProgress {
  count: number;
  target: number;
  met: boolean;
  start: string;
  end: string;
}

type RepeatType = Exclude<ScheduleType, 'weekdays'>;

const PERIOD_BUDGET: Record<RepeatType, number> = {
  timesPerWeek: 12,
  timesPerMonth: 6,
  timesPerYear: 3,
};

/** Monday, first of month, or first of year — depending on the schedule type. */
export function periodStart(type: ScheduleType, date: string): string {
  switch (type) {
    case 'timesPerWeek':
      return addDays(date, -(weekdayOf(date) - 1));
    case 'timesPerMonth':
      return `${date.slice(0, 7)}-01`;
    case 'timesPerYear':
      return `${date.slice(0, 4)}-01-01`;
    default:
      return date;
  }
}

export function periodEnd(type: ScheduleType, date: string): string {
  switch (type) {
    case 'timesPerWeek':
      return addDays(periodStart(type, date), 6);
    case 'timesPerMonth': {
      const next = fromDateKey(periodStart(type, date));
      next.setMonth(next.getMonth() + 1);
      next.setDate(0);
      return toDateKey(next);
    }
    case 'timesPerYear':
      return `${date.slice(0, 4)}-12-31`;
    default:
      return date;
  }
}

export function shiftPeriod(type: ScheduleType, start: string, delta: number): string {
  switch (type) {
    case 'timesPerWeek':
      return addDays(start, delta * 7);
    case 'timesPerMonth': {
      const [year, month] = start.split('-').map(Number);
      return toDateKey(new Date(year, month - 1 + delta, 1, 12));
    }
    case 'timesPerYear':
      return `${Number(start.slice(0, 4)) + delta}${start.slice(4)}`;
    default:
      return start;
  }
}

/** Number of periods used for flexible streak/rate windows. */
export function periodBudget(type: ScheduleType): number {
  return isFlexibleType(type) ? PERIOD_BUDGET[type] : 0;
}

function isFlexibleType(type: ScheduleType): type is RepeatType {
  return type !== 'weekdays';
}

function targetOf(schedule: HabitSchedule): number {
  return Math.max(1, schedule.targetCount);
}

/** A day that can count toward the habit: on the schedule and after creation. */
export function isEligible(schedule: HabitSchedule, date: string): boolean {
  if (schedule.createdDate && date < schedule.createdDate) return false;
  if (schedule.type === 'weekdays') return schedule.weekdays.includes(weekdayOf(date));
  return true;
}

/** Fixed weekday schedules only; flexible habits have no specific scheduled days. */
export function isScheduled(schedule: HabitSchedule, date: string): boolean {
  return schedule.type === 'weekdays' && isEligible(schedule, date);
}

export function dayCell(
  schedule: HabitSchedule,
  logs: ReadonlySet<string>,
  date: string,
  today: string,
): DayCell {
  const eligible = isEligible(schedule, date);
  const done = logs.has(date);

  if (isFuture(date, today)) {
    return { date, status: 'future', scheduled: eligible, done };
  }
  if (done) {
    // Done wins even on an off-schedule day (for example after the schedule changed).
    return { date, status: 'done', scheduled: eligible, done };
  }
  if (!eligible) {
    return { date, status: 'off', scheduled: false, done };
  }
  if (isFlexible(schedule)) {
    return { date, status: 'empty', scheduled: true, done };
  }
  return { date, status: 'missed', scheduled: true, done };
}

/** Oldest-first status cells for the `count` days ending at `today`. */
export function dayCells(
  schedule: HabitSchedule,
  logs: ReadonlySet<string>,
  today: string,
  count: number,
): DayCell[] {
  const cells: DayCell[] = [];
  let cursor = addDays(today, -(count - 1));
  while (cursor <= today) {
    cells.push(dayCell(schedule, logs, cursor, today));
    cursor = addDays(cursor, 1);
  }
  return cells;
}

export function countInPeriod(
  schedule: HabitSchedule,
  logs: ReadonlySet<string>,
  date: string,
): number {
  const start = periodStart(schedule.type, date);
  const end = periodEnd(schedule.type, date);
  let count = 0;
  logs.forEach((logDate) => {
    if (logDate >= start && logDate <= end) count += 1;
  });
  return count;
}

export function periodProgress(
  schedule: HabitSchedule,
  logs: ReadonlySet<string>,
  date: string,
): PeriodProgress {
  const target = targetOf(schedule);
  const count = countInPeriod(schedule, logs, date);
  return {
    count,
    target,
    met: count >= target,
    start: periodStart(schedule.type, date),
    end: periodEnd(schedule.type, date),
  };
}

export function isPeriodMet(
  schedule: HabitSchedule,
  logs: ReadonlySet<string>,
  date: string,
): boolean {
  return countInPeriod(schedule, logs, date) >= targetOf(schedule);
}

/**
 * Consecutive scheduled days (weekday habits) or consecutive periods meeting the
 * target (flexible habits), counting back from today. The current period only
 * counts once met; until then it neither extends nor breaks the streak.
 */
export function currentStreak(
  schedule: HabitSchedule,
  logs: ReadonlySet<string>,
  today: string,
  windowDays = 90,
): StreakResult {
  if (schedule.type !== 'weekdays') {
    const budget = periodBudget(schedule.type);
    const currentStart = periodStart(schedule.type, today);
    let start = currentStart;
    let value = 0;
    let counted = 0;

    while (counted < budget) {
      const isCurrent = start === currentStart;
      if (!isCurrent && schedule.createdDate && start < schedule.createdDate) break;
      const met = countInPeriod(schedule, logs, start) >= targetOf(schedule);
      if (met) value += 1;
      else if (!isCurrent) break;
      start = shiftPeriod(schedule.type, start, -1);
      counted += 1;
    }

    return { value, capped: value >= budget && value > 0 };
  }

  let value = 0;
  for (let offset = 0; offset < windowDays; offset += 1) {
    const date = addDays(today, -offset);
    if (schedule.createdDate && date < schedule.createdDate) break;
    if (!isScheduled(schedule, date)) continue;
    if (logs.has(date)) {
      value += 1;
      continue;
    }
    if (date === today) continue;
    break;
  }
  return { value, capped: value >= windowDays && value > 0 };
}

/**
 * Weekday habits: scheduled days marked done divided by scheduled days in the
 * last 30 days. Flexible habits: periods whose target was met divided by the
 * elapsed periods in their window (12 weeks, 6 months, or 3 years).
 */
export function completionRate(
  schedule: HabitSchedule,
  logs: ReadonlySet<string>,
  today: string,
  windowDays = 30,
): CompletionResult {
  if (schedule.type !== 'weekdays') {
    const budget = periodBudget(schedule.type);
    let start = periodStart(schedule.type, today);
    let done = 0;
    let elapsed = 0;

    for (let index = 0; index < budget; index += 1) {
      // Only the current period may have begun before the habit was created.
      if (index > 0 && schedule.createdDate && start < schedule.createdDate) break;
      elapsed += 1;
      if (countInPeriod(schedule, logs, start) >= targetOf(schedule)) done += 1;
      start = shiftPeriod(schedule.type, start, -1);
    }

    return { done, scheduled: elapsed, rate: elapsed === 0 ? null : done / elapsed };
  }

  let done = 0;
  let scheduled = 0;
  for (let offset = 0; offset < windowDays; offset += 1) {
    const date = addDays(today, -offset);
    if (schedule.createdDate && date < schedule.createdDate) break;
    if (!isScheduled(schedule, date)) continue;
    scheduled += 1;
    if (logs.has(date)) done += 1;
  }
  return { done, scheduled, rate: scheduled === 0 ? null : done / scheduled };
}

/**
 * The habit detail grid: `days` days laid out in Monday-to-Sunday columns.
 * Leading blanks complete the first week; the current week is completed with
 * future (disabled) days.
 */
export function habitGrid(
  schedule: HabitSchedule,
  logs: ReadonlySet<string>,
  today: string,
  days = 90,
): Array<DayCell | null> {
  const first = addDays(today, -(days - 1));
  const start = addDays(first, -(weekdayOf(first) - 1));
  const last = addDays(today, 7 - weekdayOf(today));
  const cells: Array<DayCell | null> = [];
  let cursor = start;
  while (cursor <= last) {
    cells.push(cursor < first ? null : dayCell(schedule, logs, cursor, today));
    cursor = addDays(cursor, 1);
  }
  return cells;
}

/** Weekday habits only appear on their weekdays; flexible habits appear every day. */
export function isVisibleToday(schedule: HabitSchedule, today: string): boolean {
  return schedule.type === 'weekdays' ? isScheduled(schedule, today) : true;
}

/** Weekday habits are done on the day; flexible habits once the period target is met. */
export function isDoneForToday(
  schedule: HabitSchedule,
  logs: ReadonlySet<string>,
  today: string,
): boolean {
  if (schedule.type === 'weekdays') return logs.has(today);
  return isPeriodMet(schedule, logs, today);
}

export function needsAttention(
  schedule: HabitSchedule,
  logs: ReadonlySet<string>,
  today: string,
): boolean {
  return isVisibleToday(schedule, today) && !isDoneForToday(schedule, logs, today);
}

/** Number of visible habits that still need a check-in. */
export function remainingToday(
  entries: Array<{ schedule: HabitSchedule; logs: ReadonlySet<string> }>,
  today: string,
): number {
  return entries.reduce((count, entry) => {
    return needsAttention(entry.schedule, entry.logs, today) ? count + 1 : count;
  }, 0);
}

/**
 * Oldest log date worth loading. 90 days by default (daily rates), 12 weeks for
 * weekly habits, 6 months for monthly, and 3 years for yearly habits.
 */
export function historyStartFor(schedules: HabitSchedule[], today: string): string {
  let start = addDays(today, -89);
  for (const schedule of schedules) {
    if (!isFlexible(schedule)) continue;
    const budget = periodBudget(schedule.type);
    const candidate = shiftPeriod(
      schedule.type,
      periodStart(schedule.type, today),
      -(budget - 1),
    );
    if (candidate < start) start = candidate;
  }
  return start;
}
