import type { Timestamp } from 'firebase/firestore';

export const SCHEDULE_TYPES = [
  'weekdays',
  'timesPerWeek',
  'timesPerMonth',
  'timesPerYear',
] as const;

export type ScheduleType = (typeof SCHEDULE_TYPES)[number];

export interface Habit {
  id: string;
  name: string;
  /** ISO weekday numbers, 1 = Monday … 7 = Sunday. Used by `weekdays` schedules. */
  weekdays: number[];
  scheduleType: ScheduleType;
  /** Times per week/month/year for flexible schedules. Ignored for `weekdays`. */
  targetCount: number;
  archived: boolean;
  createdAt: Timestamp | null;
  updatedAt: Timestamp | null;
}

export interface HabitLog {
  id: string;
  habitId: string;
  /** Local calendar date in yyyy-MM-dd form. */
  date: string;
  createdAt: Timestamp | null;
}

export interface HabitInput {
  name: string;
  scheduleType: ScheduleType;
  weekdays: number[];
  targetCount: number;
}

/** Pure schedule shape consumed by the streak logic. */
export interface HabitSchedule {
  type: ScheduleType;
  weekdays: number[];
  targetCount: number;
  /** Local yyyy-MM-dd the habit was created, or null when unknown. */
  createdDate: string | null;
}
