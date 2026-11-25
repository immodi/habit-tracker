import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useHabits } from '../hooks/useHabits';
import { ALL_WEEKDAYS, formatWeekdays } from '../lib/dates';
import { maxTarget, periodName } from '../lib/schedule';
import type { HabitInput, ScheduleType } from '../lib/types';
import { WeekdayChips } from '../components/WeekdayChips';
import { LoadingBlock } from '../components/AppShell';
import { EmptyState } from '../components/EmptyState';
import { ChevronLeftIcon } from '../components/icons';

const SCHEDULE_OPTIONS: Array<{ value: ScheduleType; label: string }> = [
  { value: 'weekdays', label: 'Specific days' },
  { value: 'timesPerWeek', label: 'Times per week' },
  { value: 'timesPerMonth', label: 'Times per month' },
  { value: 'timesPerYear', label: 'Times per year' },
];

const DEFAULT_TARGET: Record<Exclude<ScheduleType, 'weekdays'>, number> = {
  timesPerWeek: 3,
  timesPerMonth: 10,
  timesPerYear: 12,
};

export function HabitEditor() {
  const { habitId } = useParams<{ habitId: string }>();
  const isEditing = Boolean(habitId);
  const { habits, loading, addHabit, updateHabit, archiveHabit, restoreHabit } = useHabits();
  const navigate = useNavigate();

  const existing = habitId ? habits.find((habit) => habit.id === habitId) : undefined;

  const [name, setName] = useState('');
  const [scheduleType, setScheduleType] = useState<ScheduleType>('weekdays');
  const [weekdays, setWeekdays] = useState<number[]>([...ALL_WEEKDAYS]);
  const [targetCount, setTargetCount] = useState(3);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmArchive, setConfirmArchive] = useState(false);

  useEffect(() => {
    if (!existing) return;
    setName(existing.name);
    setScheduleType(existing.scheduleType);
    setWeekdays(existing.weekdays.length > 0 ? existing.weekdays : [...ALL_WEEKDAYS]);
    setTargetCount(existing.targetCount);
  }, [existing?.id]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    document.title = isEditing ? 'Edit habit · Habits' : 'New habit · Habits';
  }, [isEditing]);

  const backTo = existing ? `/habits/${existing.id}` : '/';

  function chooseScheduleType(next: ScheduleType) {
    if (next === scheduleType) return;
    setScheduleType(next);
    if (next === 'weekdays') return;
    setTargetCount((current) => {
      if (scheduleType === 'weekdays') return DEFAULT_TARGET[next];
      return Math.min(Math.max(1, current), maxTarget(next));
    });
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) {
      setError('Give the habit a name.');
      return;
    }
    if (scheduleType === 'weekdays' && weekdays.length === 0) {
      setError('Pick at least one day.');
      return;
    }
    const count = Math.min(Math.max(1, targetCount), maxTarget(scheduleType));
    if (scheduleType !== 'weekdays' && count < 1) {
      setError('Pick how many times per period.');
      return;
    }

    const input: HabitInput = {
      name: trimmed,
      scheduleType,
      weekdays: scheduleType === 'weekdays' ? weekdays : [],
      targetCount: scheduleType === 'weekdays' ? 1 : count,
    };

    setBusy(true);
    setError(null);
    try {
      if (existing) {
        await updateHabit(existing.id, input);
        navigate(`/habits/${existing.id}`, { replace: true });
      } else {
        await addHabit(input);
        navigate('/', { replace: true });
      }
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Could not save the habit.');
      setBusy(false);
    }
  }

  async function handleArchive() {
    if (!existing) return;
    if (!confirmArchive) {
      setConfirmArchive(true);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await archiveHabit(existing.id);
      navigate('/archived', { replace: true });
    } catch (archiveError) {
      setError(archiveError instanceof Error ? archiveError.message : 'Could not archive.');
      setBusy(false);
      setConfirmArchive(false);
    }
  }

  async function handleRestore() {
    if (!existing) return;
    setBusy(true);
    setError(null);
    try {
      await restoreHabit(existing.id);
      navigate('/', { replace: true });
    } catch (restoreError) {
      setError(restoreError instanceof Error ? restoreError.message : 'Could not restore.');
      setBusy(false);
    }
  }

  if (isEditing && loading) {
    return (
      <div className="page">
        <LoadingBlock label="Loading habit" />
      </div>
    );
  }

  if (isEditing && !existing) {
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

  const unit = periodName(scheduleType);

  return (
    <div className="page">
      <Link to={backTo} className="back-link">
        <ChevronLeftIcon size={18} />
        {existing ? existing.name : 'Today'}
      </Link>

      <section className="page-head">
        <p className="eyebrow">{isEditing ? 'Edit habit' : 'New habit'}</p>
        <h1 className="page-title">{isEditing ? 'Fine-tune it' : 'What will you do?'}</h1>
      </section>

      <form className="card card-pad form-card" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="habit-name">Name</label>
          <input
            id="habit-name"
            className="text-input"
            type="text"
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Read for 20 minutes"
            autoComplete="off"
            autoFocus={!isEditing}
            maxLength={80}
          />
        </div>

        <div className="field">
          <label>How often</label>
          <div className="chips" role="radiogroup" aria-label="Recurrence">
            {SCHEDULE_OPTIONS.map((option) => (
              <button
                key={option.value}
                type="button"
                className="chip"
                aria-pressed={scheduleType === option.value}
                onClick={() => chooseScheduleType(option.value)}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>

        {scheduleType === 'weekdays' ? (
          <div className="field">
            <label>Days</label>
            <WeekdayChips value={weekdays} onChange={setWeekdays} />
            <p className="field-hint">
              {weekdays.length === 0
                ? 'Pick at least one day.'
                : `Scheduled ${formatWeekdays(weekdays).toLowerCase()}.`}
            </p>
          </div>
        ) : (
          <div className="field">
            <label>Target</label>
            <div className="stepper">
              <button
                type="button"
                className="stepper-btn"
                aria-label="Fewer times"
                disabled={targetCount <= 1}
                onClick={() => setTargetCount((current) => Math.max(1, current - 1))}
              >
                −
              </button>
              <input
                className="stepper-input"
                type="number"
                inputMode="numeric"
                min={1}
                max={maxTarget(scheduleType)}
                value={targetCount}
                aria-label={`Times per ${unit}`}
                onChange={(event) => {
                  const next = Number(event.target.value);
                  setTargetCount(
                    Number.isFinite(next)
                      ? Math.min(Math.max(1, next), maxTarget(scheduleType))
                      : 1,
                  );
                }}
              />
              <button
                type="button"
                className="stepper-btn"
                aria-label="More times"
                disabled={targetCount >= maxTarget(scheduleType)}
                onClick={() =>
                  setTargetCount((current) => Math.min(maxTarget(scheduleType), current + 1))
                }
              >
                +
              </button>
              <span className="stepper-unit">
                times per {unit}
                <span className="stepper-hint">Any day counts.</span>
              </span>
            </div>
            <p className="field-hint">
              One check-in per day. Extra check-ins beyond the target are fine; they do not
              extend the streak further.
            </p>
          </div>
        )}

        {error ? <div className="notice notice-error">{error}</div> : null}

        <div className="form-actions">
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy ? 'Saving…' : isEditing ? 'Save changes' : 'Create habit'}
          </button>
          <Link to={backTo} className="btn btn-ghost">
            Cancel
          </Link>
        </div>
      </form>

      {existing ? (
        <div className={`card card-pad${existing.archived ? '' : ' danger-zone'}`}>
          <div>
            <p className="row-title">
              {existing.archived
                ? 'Restore habit'
                : confirmArchive
                  ? 'Archive this habit?'
                  : 'Archive habit'}
            </p>
            <p className="row-sub">
              {existing.archived
                ? 'This habit is archived. Restore it to bring it back to Today.'
                : 'It disappears from Today, keeps its history, and can be restored later.'}
            </p>
          </div>
          {existing.archived ? (
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => void handleRestore()}
              disabled={busy}
            >
              {busy ? 'Restoring…' : 'Restore'}
            </button>
          ) : (
            <button
              type="button"
              className={`btn ${confirmArchive ? 'btn-danger' : 'btn-ghost'}`}
              onClick={() => void handleArchive()}
              disabled={busy}
            >
              {confirmArchive ? 'Yes, archive' : 'Archive'}
            </button>
          )}
        </div>
      ) : null}
    </div>
  );
}
