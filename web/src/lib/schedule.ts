import { toDateKey, formatWeekdays } from './dates';
import type { Habit, HabitSchedule, ScheduleType } from './types';
import { SCHEDULE_TYPES } from './types';

export function toScheduleType(value: unknown): ScheduleType {
  return typeof value === 'string' && (SCHEDULE_TYPES as readonly string[]).includes(value)
    ? (value as ScheduleType)
    : 'weekdays';
}

export function scheduleFromHabit(habit: Habit): HabitSchedule {
  return {
    type: habit.scheduleType,
    weekdays: habit.weekdays,
    targetCount: habit.targetCount,
    createdDate: habit.createdAt ? toDateKey(habit.createdAt.toDate()) : null,
  };
}

export function isFlexible(schedule: HabitSchedule): boolean {
  return schedule.type !== 'weekdays';
}

export function periodName(type: ScheduleType): string {
  switch (type) {
    case 'timesPerWeek':
      return 'week';
    case 'timesPerMonth':
      return 'month';
    case 'timesPerYear':
      return 'year';
    default:
      return 'day';
  }
}

export function maxTarget(type: ScheduleType): number {
  switch (type) {
    case 'timesPerWeek':
      return 7;
    case 'timesPerMonth':
      return 31;
    case 'timesPerYear':
      return 366;
    default:
      return 7;
  }
}

/** "Every day", "Weekdays", "3 times per week", … */
export function formatSchedule(schedule: HabitSchedule): string {
  if (!isFlexible(schedule)) return formatWeekdays(schedule.weekdays);
  const count = Math.max(1, schedule.targetCount);
  return `${count} ${count === 1 ? 'time' : 'times'} per ${periodName(schedule.type)}`;
}

export function streakUnit(type: ScheduleType, value: number): string {
  const name = periodName(type);
  return value === 1 ? name : `${name}s`;
}

export function streakSuffix(type: ScheduleType): string {
  switch (type) {
    case 'timesPerWeek':
      return 'w';
    case 'timesPerMonth':
      return 'mo';
    case 'timesPerYear':
      return 'y';
    default:
      return 'd';
  }
}

export function rateWindowLabel(type: ScheduleType): string {
  switch (type) {
    case 'timesPerWeek':
      return 'Last 12 weeks';
    case 'timesPerMonth':
      return 'Last 6 months';
    case 'timesPerYear':
      return 'Last 3 years';
    default:
      return 'Last 30 days';
  }
}

export function thisPeriodLabel(type: ScheduleType): string {
  return `this ${periodName(type)}`;
}
