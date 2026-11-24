import { Link } from 'react-router-dom';
import type { Habit } from '../lib/types';
import { currentStreak, dayCells, periodProgress } from '../lib/streaks';
import {
  isFlexible,
  scheduleFromHabit,
  streakSuffix,
  thisPeriodLabel,
} from '../lib/schedule';
import { WeekStrip } from './WeekStrip';
import { CheckIcon, FlameIcon } from './icons';

interface HabitRowProps {
  habit: Habit;
  logs: ReadonlySet<string>;
  today: string;
  onToggle: (date: string, done: boolean) => void;
}

export function HabitRow({ habit, logs, today, onToggle }: HabitRowProps) {
  const schedule = scheduleFromHabit(habit);
  const done = logs.has(today);
  const streak = currentStreak(schedule, logs, today);
  const week = dayCells(schedule, logs, today, 7);
  const flexible = isFlexible(schedule);
  const progress = flexible ? periodProgress(schedule, logs, today) : null;

  const statusText = progress
    ? `${Math.min(progress.count, progress.target)} of ${progress.target} ${thisPeriodLabel(schedule.type)}`
    : done
      ? 'Done today'
      : 'Not done yet';

  return (
    <article className={`habit-card${done ? ' is-done' : ''}`}>
      <div className="habit-head">
        <Link to={`/habits/${habit.id}`} className="habit-name">
          {habit.name}
        </Link>
        {streak.value > 0 ? (
          <span className="streak-pill">
            <FlameIcon size={12} />
            {streak.value}
            {streak.capped ? '+' : ''}
            {streakSuffix(schedule.type)}
          </span>
        ) : null}
      </div>

      <div className="habit-controls">
        <button
          type="button"
          className={`toggle${done ? ' is-done' : ''}`}
          aria-pressed={done}
          aria-label={done ? `Mark ${habit.name} not done` : `Mark ${habit.name} done`}
          onClick={() => onToggle(today, !done)}
        >
          <CheckIcon size={22} />
        </button>
        <span className="meta-text">{statusText}</span>
        <WeekStrip cells={week} today={today} />
      </div>
    </article>
  );
}
