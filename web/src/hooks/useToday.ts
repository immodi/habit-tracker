import { useEffect, useState } from 'react';
import { todayKey } from '../lib/dates';

const REFRESH_AFTER_MIDNIGHT_MS = 2_000;

function msUntilTomorrow(now: Date): number {
  const tomorrow = new Date(
    now.getFullYear(),
    now.getMonth(),
    now.getDate() + 1,
    0,
    0,
    0,
    0,
  );
  return tomorrow.getTime() - now.getTime() + REFRESH_AFTER_MIDNIGHT_MS;
}

/** The user's local calendar date, refreshed just after local midnight. */
export function useToday(): string {
  const [today, setToday] = useState(() => todayKey());

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setToday(todayKey());
    }, msUntilTomorrow(new Date()));
    return () => window.clearTimeout(timer);
  }, [today]);

  return today;
}
