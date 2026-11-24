import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useHabits } from '../hooks/useHabits';
import { useToday } from '../hooks/useToday';
import { formatFullDate } from '../lib/dates';
import { isDoneForToday, isVisibleToday } from '../lib/streaks';
import { isFlexible, scheduleFromHabit } from '../lib/schedule';
import { HabitRow } from '../components/HabitRow';
import { EmptyState } from '../components/EmptyState';
import { LoadingBlock } from '../components/AppShell';
import { CheckCircleIcon, PlusIcon } from '../components/icons';

export function Today() {
  const { habits, logs, loading, error, toggleLog } = useHabits();
  const today = useToday();

  const active = habits.filter((habit) => !habit.archived);
  const visible = active.filter((habit) => isVisibleToday(scheduleFromHabit(habit), today));
  const doneCount = visible.filter((habit) =>
    isDoneForToday(scheduleFromHabit(habit), logs.get(habit.id) ?? EMPTY, today),
  ).length;
  const remaining = visible.length - doneCount;
  const percent = visible.length === 0 ? 0 : Math.round((doneCount / visible.length) * 100);
  const hasFlexible = visible.some((habit) => isFlexible(scheduleFromHabit(habit)));

  useEffect(() => {
    document.title = remaining > 0 ? `(${remaining}) Habits` : 'Habits';
  }, [remaining]);

  const toggle = (habitId: string) => (date: string, done: boolean) => {
    void toggleLog(habitId, date, done);
  };

  return (
    <div className="page">
      <section className="page-head">
        <p className="eyebrow">{formatFullDate(today)}</p>
        <h1 className="page-title">Today</h1>
      </section>

      {error ? <div className="notice notice-error">{error}</div> : null}

      {loading ? (
        <LoadingBlock label="Loading habits" />
      ) : active.length === 0 ? (
        <EmptyState
          icon={<CheckCircleIcon size={26} />}
          title="Start with one habit"
          body="Pick something small you can do today. You can add more whenever you like."
          action={
            <Link to="/habits/new" className="btn btn-primary">
              <PlusIcon size={18} />
              Add your first habit
            </Link>
          }
        />
      ) : (
        <>
          <div className="summary-card">
            <div
              className="ring"
              role="img"
              aria-label={`${percent}% of today's habits done`}
            >
              <svg viewBox="0 0 48 48" aria-hidden="true">
                <circle className="ring-track" cx="24" cy="24" r="20" />
                <circle
                  className="ring-progress"
                  cx="24"
                  cy="24"
                  r="20"
                  strokeDasharray={`${(percent / 100) * 125.66} 125.66`}
                  transform="rotate(-90 24 24)"
                />
              </svg>
              <span className="ring-label">{percent}%</span>
            </div>
            <div className="summary-body">
              <p className="summary-headline">
                <strong>
                  {doneCount} of {visible.length}
                </strong>{' '}
                done{hasFlexible ? '' : ' today'}
              </p>
              <div className="progress-track" aria-hidden="true">
                <div className="progress-fill" style={{ width: `${percent}%` }} />
              </div>
              <p className="summary-sub">
                {visible.length === 0
                  ? 'Nothing is scheduled for today.'
                  : remaining === 0
                    ? 'Everything is on track. Nicely done.'
                    : `${remaining} habit${remaining === 1 ? '' : 's'} still ${
                        remaining === 1 ? 'needs' : 'need'
                      } attention.`}
              </p>
            </div>
          </div>

          {visible.length === 0 ? (
            <div className="card card-pad muted-card">
              Today is a rest day for all of your habits. Enjoy it.
            </div>
          ) : (
            <div className="habit-list">
              {visible.map((habit) => (
                <HabitRow
                  key={habit.id}
                  habit={habit}
                  logs={logs.get(habit.id) ?? EMPTY}
                  today={today}
                  onToggle={toggle(habit.id)}
                />
              ))}
            </div>
          )}

          <Link to="/habits/new" className="btn btn-primary btn-block add-button">
            <PlusIcon size={18} />
            Add habit
          </Link>
        </>
      )}
    </div>
  );
}

const EMPTY: ReadonlySet<string> = new Set<string>();
