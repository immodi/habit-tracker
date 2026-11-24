import { describe, expect, it } from 'vitest';
import { lastDays } from './dates';
import {
  completionRate,
  countInPeriod,
  currentStreak,
  dayCell,
  habitGrid,
  historyStartFor,
  isDoneForToday,
  isPeriodMet,
  isScheduled,
  isVisibleToday,
  periodProgress,
  remainingToday,
} from './streaks';
import type { HabitSchedule } from './types';

const DAILY: HabitSchedule = {
  type: 'weekdays',
  weekdays: [1, 2, 3, 4, 5, 6, 7],
  targetCount: 1,
  createdDate: null,
};
const WEEKDAYS: HabitSchedule = {
  type: 'weekdays',
  weekdays: [1, 2, 3, 4, 5],
  targetCount: 1,
  createdDate: null,
};
const WEEKLY_THREE: HabitSchedule = {
  type: 'timesPerWeek',
  weekdays: [],
  targetCount: 3,
  createdDate: null,
};
const MONTHLY_TEN: HabitSchedule = {
  type: 'timesPerMonth',
  weekdays: [],
  targetCount: 10,
  createdDate: null,
};

const logs = (...dates: string[]) => new Set(dates);

describe('currentStreak', () => {
  it('counts consecutive days for a daily habit', () => {
    // Monday 23 Nov 2026.
    const today = '2026-11-23';
    expect(currentStreak(DAILY, logs('2026-11-21', '2026-11-22', '2026-11-23'), today)).toEqual({
      value: 3,
      capped: false,
    });
  });

  it('keeps a Monday-to-Friday streak alive across the weekend', () => {
    // Sunday 22 Nov 2026; the last scheduled day is Friday the 20th.
    const today = '2026-11-22';
    const done = logs(
      '2026-11-16',
      '2026-11-17',
      '2026-11-18',
      '2026-11-19',
      '2026-11-20',
    );
    expect(currentStreak(WEEKDAYS, done, today).value).toBe(5);
  });

  it('does not break the streak when today is scheduled but not yet done', () => {
    const today = '2026-11-23';
    expect(currentStreak(DAILY, logs('2026-11-21', '2026-11-22'), today)).toEqual({
      value: 2,
      capped: false,
    });
  });

  it('stops at a missed day in the middle', () => {
    const today = '2026-11-23';
    const done = logs('2026-11-23', '2026-11-22', '2026-11-20', '2026-11-19');
    expect(currentStreak(DAILY, done, today).value).toBe(2);
  });

  it('ignores days before the habit was created', () => {
    const today = '2026-11-23';
    const created = { ...DAILY, createdDate: '2026-11-21' };
    const done = logs('2026-11-20', '2026-11-21', '2026-11-22', '2026-11-23');
    expect(currentStreak(created, done, today)).toEqual({ value: 3, capped: false });
  });

  it('reports 90+ when the streak fills the whole window', () => {
    const today = '2026-11-23';
    const all = lastDays(today, 90);
    expect(currentStreak(DAILY, logs(...all), today)).toEqual({ value: 90, capped: true });
  });

  it('does not count off-schedule days as done', () => {
    const today = '2026-11-23';
    // Saturday 21 and Sunday 22 are off for a weekday habit.
    expect(currentStreak(WEEKDAYS, logs('2026-11-21', '2026-11-22'), today).value).toBe(0);
  });

  it('counts consecutive weeks that met a weekly target', () => {
    const today = '2026-11-25'; // Wednesday
    const done = logs(
      // This week (Mon 23 – Sun 29) met: 3 check-ins.
      '2026-11-23',
      '2026-11-24',
      '2026-11-25',
      // Previous week (16–22) met.
      '2026-11-16',
      '2026-11-17',
      '2026-11-18',
    );
    expect(currentStreak(WEEKLY_THREE, done, today)).toEqual({ value: 2, capped: false });
  });

  it('skips the current week until its target is met', () => {
    const today = '2026-11-25';
    const done = logs(
      // Current week has 2 of 3.
      '2026-11-23',
      '2026-11-24',
      // Previous two weeks were met.
      '2026-11-16',
      '2026-11-17',
      '2026-11-18',
      '2026-11-09',
      '2026-11-10',
      '2026-11-11',
    );
    expect(currentStreak(WEEKLY_THREE, done, today)).toEqual({ value: 2, capped: false });
  });

  it('stops at an unfinished earlier week', () => {
    const today = '2026-11-25';
    const done = logs(
      '2026-11-23',
      '2026-11-24',
      '2026-11-25',
      // Previous week only 2 of 3.
      '2026-11-16',
      '2026-11-17',
      // Older week met, but the streak already broke.
      '2026-11-09',
      '2026-11-10',
      '2026-11-11',
    );
    expect(currentStreak(WEEKLY_THREE, done, today).value).toBe(1);
  });

  it('counts a met creation week but not periods before creation', () => {
    const created = { ...WEEKLY_THREE, createdDate: '2026-11-23' };
    const done = logs('2026-11-23', '2026-11-24', '2026-11-25');
    expect(currentStreak(created, done, '2026-11-25')).toEqual({ value: 1, capped: false });
  });

  it('counts consecutive months that met a monthly target', () => {
    const created = { ...MONTHLY_TEN, createdDate: '2026-10-01' };
    const done = logs(
      '2026-11-01', '2026-11-02', '2026-11-03',
      '2026-10-01', '2026-10-02', '2026-10-03', '2026-10-04', '2026-10-05',
      '2026-10-06', '2026-10-07', '2026-10-08', '2026-10-09', '2026-10-10',
    );
    // November has only 3 of 10, so the streak comes from October.
    expect(currentStreak(created, done, '2026-11-15')).toEqual({ value: 1, capped: false });
  });
});

describe('completionRate', () => {
  it('divides done scheduled days by scheduled days in 30 days', () => {
    const today = '2026-11-23';
    const created = { ...DAILY, createdDate: '2026-11-21' };
    const done = logs('2026-11-21', '2026-11-23');
    expect(completionRate(created, done, today)).toEqual({
      done: 2,
      scheduled: 3,
      rate: 2 / 3,
    });
  });

  it('returns null when nothing was scheduled', () => {
    // Sunday 22 Nov; a weekday-only habit created that day has no scheduled days yet.
    const created = { ...WEEKDAYS, createdDate: '2026-11-22' };
    expect(completionRate(created, logs(), '2026-11-22').rate).toBeNull();
  });

  it('skips weekend days for a weekday habit', () => {
    const today = '2026-11-23';
    const created = { ...WEEKDAYS, createdDate: '2026-11-16' };
    const done = logs('2026-11-16', '2026-11-17', '2026-11-18');
    expect(completionRate(created, done, today)).toEqual({
      done: 3,
      scheduled: 6,
      rate: 0.5,
    });
  });

  it('measures met weeks out of elapsed weeks', () => {
    const created = { ...WEEKLY_THREE, createdDate: '2026-11-16' };
    const done = logs(
      '2026-11-16', '2026-11-17', '2026-11-18',
      '2026-11-23', '2026-11-24', '2026-11-25',
    );
    expect(completionRate(created, done, '2026-11-25')).toEqual({
      done: 2,
      scheduled: 2,
      rate: 1,
    });
  });

  it('counts the current month even before it is met', () => {
    const created = { ...MONTHLY_TEN, createdDate: '2026-10-01' };
    const done = logs(
      '2026-10-01', '2026-10-02', '2026-10-03', '2026-10-04', '2026-10-05',
      '2026-10-06', '2026-10-07', '2026-10-08', '2026-10-09', '2026-10-10',
    );
    expect(completionRate(created, done, '2026-11-15')).toEqual({
      done: 1,
      scheduled: 2,
      rate: 0.5,
    });
  });
});

describe('day status', () => {
  it('marks an off-schedule day with an existing check-in as done but unscheduled', () => {
    const cell = dayCell(WEEKDAYS, logs('2026-11-21'), '2026-11-21', '2026-11-23');
    expect(cell).toMatchObject({ status: 'done', scheduled: false, done: true });
  });

  it('marks future days as future and disabled', () => {
    expect(dayCell(DAILY, logs(), '2026-11-24', '2026-11-23').status).toBe('future');
  });

  it('marks an unchecked flexible day as empty, not missed', () => {
    const cell = dayCell(WEEKLY_THREE, logs(), '2026-11-24', '2026-11-25');
    expect(cell).toMatchObject({ status: 'empty', scheduled: true, done: false });
  });

  it('still marks a missed weekday habit as missed', () => {
    expect(dayCell(WEEKDAYS, logs(), '2026-11-20', '2026-11-23').status).toBe('missed');
  });

  it('knows which days are scheduled', () => {
    expect(isScheduled(WEEKDAYS, '2026-11-21')).toBe(false);
    expect(isScheduled(WEEKDAYS, '2026-11-20')).toBe(true);
    expect(isScheduled(WEEKLY_THREE, '2026-11-20')).toBe(false);
  });
});

describe('flexible progress', () => {
  it('counts check-ins inside the calendar week', () => {
    const done = logs('2026-11-23', '2026-11-24', '2026-11-29');
    expect(countInPeriod(WEEKLY_THREE, done, '2026-11-25')).toBe(3);
    expect(periodProgress(WEEKLY_THREE, done, '2026-11-25')).toMatchObject({
      count: 3,
      target: 3,
      met: true,
      start: '2026-11-23',
      end: '2026-11-29',
    });
  });

  it('counts check-ins inside the calendar month', () => {
    const done = logs('2026-11-01', '2026-10-31');
    expect(countInPeriod(MONTHLY_TEN, done, '2026-11-15')).toBe(1);
  });

  it('treats surplus check-ins as met', () => {
    const two = { ...WEEKLY_THREE, targetCount: 2 };
    expect(isPeriodMet(two, logs('2026-11-23', '2026-11-24', '2026-11-25'), '2026-11-25')).toBe(true);
  });

  it('keeps flexible habits visible every day', () => {
    expect(isVisibleToday(WEEKLY_THREE, '2026-11-25')).toBe(true);
    expect(isVisibleToday(WEEKDAYS, '2026-11-22')).toBe(false);
  });

  it('uses the period target for Today done state', () => {
    const done = logs('2026-11-23', '2026-11-24', '2026-11-25');
    expect(isDoneForToday(WEEKLY_THREE, done, '2026-11-25')).toBe(true);
    expect(isDoneForToday(WEEKLY_THREE, logs('2026-11-23'), '2026-11-25')).toBe(false);
  });
});

describe('habitGrid', () => {
  it('lays the 90-day window out in complete Monday-to-Sunday weeks', () => {
    const cells = habitGrid(DAILY, logs(), '2026-11-23', 90);
    expect(cells.length % 7).toBe(0);
    const real = cells.filter((cell) => cell !== null);
    const past = real.filter((cell) => cell.status !== 'future');
    expect(past).toHaveLength(90);
    expect(real).toHaveLength(96); // 6 future days complete the current week.
    // Real days form one contiguous run, padded with nulls only at the start.
    const first = cells.findIndex((cell) => cell !== null);
    expect(cells.slice(0, first).every((cell) => cell === null)).toBe(true);
    expect(cells.slice(first).every((cell) => cell !== null)).toBe(true);
  });

  it('starts on Monday and ends on today when today is Sunday', () => {
    const cells = habitGrid(DAILY, logs(), '2026-11-22', 7);
    expect(cells).toHaveLength(7);
    expect(cells.every(Boolean)).toBe(true);
    expect(new Date(`${cells[0]!.date}T12:00:00`).getDay()).toBe(1);
    expect(cells[6]!.date).toBe('2026-11-22');
  });
});

describe('remainingToday', () => {
  it('counts scheduled weekday habits and unmet flexible habits', () => {
    const entries = [
      { schedule: DAILY, logs: logs('2026-11-23') },
      { schedule: DAILY, logs: logs() },
      { schedule: WEEKDAYS, logs: logs('2026-11-23') },
      { schedule: WEEKLY_THREE, logs: logs('2026-11-23', '2026-11-24') },
    ];
    expect(remainingToday(entries, '2026-11-23')).toBe(2);
  });

  it('ignores met flexible habits and unscheduled weekday habits', () => {
    const entries = [
      { schedule: WEEKDAYS, logs: logs() },
      { schedule: { ...WEEKLY_THREE, targetCount: 1 }, logs: logs('2026-11-16') },
    ];
    expect(remainingToday(entries, '2026-11-22')).toBe(0);
  });
});

describe('historyStartFor', () => {
  const today = '2026-11-23';
  const ninetyDaysBack = lastDays(today, 90)[0];

  it('keeps the 90-day default for weekday and weekly habits', () => {
    expect(historyStartFor([DAILY, WEEKLY_THREE], today)).toBe(ninetyDaysBack);
  });

  it('loads six months for monthly habits', () => {
    expect(historyStartFor([MONTHLY_TEN], today)).toBe('2026-06-01');
  });

  it('loads three years for yearly habits', () => {
    const yearly: HabitSchedule = {
      type: 'timesPerYear',
      weekdays: [],
      targetCount: 12,
      createdDate: null,
    };
    expect(historyStartFor([yearly], today)).toBe('2024-01-01');
  });
});
