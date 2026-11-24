import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useHabits } from '../hooks/useHabits';
import { formatSchedule, scheduleFromHabit } from '../lib/schedule';
import { EmptyState } from '../components/EmptyState';
import { LoadingBlock } from '../components/AppShell';
import { ArchiveIcon } from '../components/icons';

export function Archived() {
  const { habits, loading, restoreHabit } = useHabits();
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    document.title = 'Archived · Habits';
  }, []);

  const archived = habits.filter((habit) => habit.archived);

  async function restore(habitId: string) {
    setBusyId(habitId);
    setError(null);
    try {
      await restoreHabit(habitId);
    } catch (restoreError) {
      setError(restoreError instanceof Error ? restoreError.message : 'Could not restore.');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="page">
      <section className="page-head">
        <p className="eyebrow">Kept, not gone</p>
        <h1 className="page-title">Archived</h1>
      </section>

      {error ? <div className="notice notice-error">{error}</div> : null}

      {loading ? (
        <LoadingBlock label="Loading archived habits" />
      ) : archived.length === 0 ? (
        <EmptyState
          icon={<ArchiveIcon size={26} />}
          title="Nothing archived"
          body="Archiving a habit hides it from Today but keeps every check-in. You can restore it here."
          action={
            <Link to="/" className="btn">
              Back to Today
            </Link>
          }
        />
      ) : (
        <div className="list">
          {archived.map((habit) => (
            <div className="list-row" key={habit.id}>
              <div>
                <Link to={`/habits/${habit.id}`} className="row-title row-link">
                  {habit.name}
                </Link>
                <p className="row-sub">{formatSchedule(scheduleFromHabit(habit))}</p>
              </div>
              <button
                type="button"
                className="btn btn-ghost btn-small"
                onClick={() => void restore(habit.id)}
                disabled={busyId === habit.id}
              >
                {busyId === habit.id ? 'Restoring…' : 'Restore'}
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
