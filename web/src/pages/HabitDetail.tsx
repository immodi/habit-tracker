import { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useHabits } from '../hooks/useHabits';
import { useToday } from '../hooks/useToday';
import { completionRate, currentStreak, habitGrid, periodProgress } from '../lib/streaks';
import {
  formatSchedule,
  isFlexible,
  periodName,
  rateWindowLabel,
  scheduleFromHabit,
  streakUnit,
} from '../lib/schedule';
import { Heatmap, HeatmapLegend } from '../components/Heatmap';
import { EmptyState } from '../components/EmptyState';
import { LoadingBlock } from '../components/AppShell';
import { ChevronLeftIcon, FlameIcon } from '../components/icons';

export function HabitDetail() {
  const { habitId } = useParams<{ habitId: string }>();
  const { habits, logs, loading, toggleLog } = useHabits();
  const today = useToday();

  const habit = habits.find((entry) => entry.id === habitId);
  const habitLogs = habitId ? (logs.get(habitId) ?? EMPTY) : EMPTY;

  useEffect(() => {
    document.title = habit ? `${habit.name} · Habits` : 'Habits';
  }, [habit]);

  if (loading) {
    return (
      <div className="page">
        <LoadingBlock label="Loading habit" />
      </div>
    );
  }

  if (!habit) {
    return (
      <div className="page">
        <EmptyState
          title="Habit not found"
          body="It may have been removed or belongs to another account."
          action={
            <Link to="/" className="btn">
              Back to Today
            </Link>
          }
        />
      </div>
    );
  }

  const schedule = scheduleFromHabit(habit);
  const flexible = isFlexible(schedule);
  const streak = currentStreak(schedule, habitLogs, today);
  const completion = completionRate(schedule, habitLogs, today);
  const progress = flexible ? periodProgress(schedule, habitLogs, today) : null;
  const cells = habitGrid(schedule, habitLogs, today, 90);
  const progressPercent = progress
    ? Math.min(100, Math.round((progress.count / progress.target) * 100))
    : 0;

  const handleToggle = (date: string, done: boolean) => {
    void toggleLog(habit.id, date, done);
  };

  return (
    <div className="page">
      <Link to="/" className="back-link">
        <ChevronLeftIcon size={18} />
        Today
      </Link>

      <section className="page-head detail-head">
        <div>
          <p className="eyebrow">
            {formatSchedule(schedule)}
            {habit.archived ? ' · archived' : ''}
          </p>
          <h1 className="page-title">{habit.name}</h1>
        </div>
        <Link to={`/habits/${habit.id}/edit`} className="btn btn-ghost">
          Edit
        </Link>
      </section>

      <div className="stat-grid">
        <div className="stat">
          <p className="stat-label">
            <FlameIcon size={13} />
            Current streak
          </p>
          <p className="stat-value">
            {streak.capped ? `${streak.value}+` : streak.value}
            <span className="stat-unit">{streakUnit(schedule.type, streak.value)}</span>
          </p>
        </div>
        <div className="stat">
          <p className="stat-label">{rateWindowLabel(schedule.type)}</p>
          <p className="stat-value">
            {completion.rate === null ? '—' : `${Math.round(completion.rate * 100)}%`}
            <span className="stat-unit">
              {completion.scheduled > 0
                ? `${completion.done}/${completion.scheduled}`
                : 'nothing yet'}
            </span>
          </p>
        </div>
      </div>

      {progress ? (
        <section className="card card-pad period-card">
          <div className="period-top">
            <span className="period-label">This {periodName(schedule.type)}</span>
            <span className="period-count">
              {Math.min(progress.count, progress.target)} of {progress.target}
            </span>
          </div>
          <div className="progress-track" aria-hidden="true">
            <div className="progress-fill" style={{ width: `${progressPercent}%` }} />
          </div>
          <p className="period-sub">
            {progress.met
              ? 'Target met. Extra check-ins are fine.'
              : `Any day can count; one check-in per day.`}
          </p>
        </section>
      ) : null}

      <section className="card card-pad heatmap-card">
        <div className="card-head">
          <h2>Last 90 days</h2>
          <span className="card-sub">
            {flexible ? 'Tap any day to add or remove a check-in' : 'Tap a day to fill in a missed check-in'}
          </span>
        </div>
        <Heatmap cells={cells} today={today} onToggle={handleToggle} />
        <HeatmapLegend flexible={flexible} />
        <p className="card-footnote">
          {flexible
            ? 'Every day can count toward this target. A check-in before the habit was created is ignored.'
            : 'Days outside the schedule are muted and can’t be toggled. A check-in that already exists on such a day still shows, but doesn’t count toward the streak or rate.'}
        </p>
      </section>
    </div>
  );
}

const EMPTY: ReadonlySet<string> = new Set<string>();
