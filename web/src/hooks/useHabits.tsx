import {
  addDoc,
  collection,
  deleteDoc,
  doc,
  onSnapshot,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
} from 'firebase/firestore';
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { db } from '../firebase';
import { toDateKey } from '../lib/dates';
import { historyStartFor } from '../lib/streaks';
import { maxTarget, scheduleFromHabit, toScheduleType } from '../lib/schedule';
import type { Habit, HabitInput } from '../lib/types';
import { useAuth } from './useAuth';
import { useToday } from './useToday';

interface HabitsState {  habits: Habit[];
  /** habitId -> set of yyyy-MM-dd check-in dates. */
  logs: ReadonlyMap<string, ReadonlySet<string>>;
  loading: boolean;
  error: string | null;
  addHabit: (input: HabitInput) => Promise<void>;
  updateHabit: (habitId: string, input: HabitInput) => Promise<void>;
  archiveHabit: (habitId: string) => Promise<void>;
  restoreHabit: (habitId: string) => Promise<void>;
  toggleLog: (habitId: string, date: string, done: boolean) => Promise<void>;
}

const HabitsContext = createContext<HabitsState | null>(null);

function logDocumentId(habitId: string, date: string): string {
  return `${habitId}_${date}`;
}

export function HabitsProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const uid = user?.uid;
  const today = useToday();

  const [habits, setHabits] = useState<Habit[]>([]);
  const [logs, setLogs] = useState<Map<string, Set<string>>>(() => new Map());
  const [habitsReady, setHabitsReady] = useState(false);
  const [logsReady, setLogsReady] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!uid) return;
    setHabitsReady(false);
    const habitsRef = collection(db, 'users', uid, 'habits');
    const unsubscribe = onSnapshot(
      habitsRef,
      (snapshot) => {
        const next = snapshot.docs.map((document) => {
          const data = document.data();
          const scheduleType = toScheduleType(data.scheduleType);
          const rawTarget = typeof data.targetCount === 'number' ? data.targetCount : 1;
          return {
            id: document.id,
            name: typeof data.name === 'string' ? data.name : '',
            weekdays: Array.isArray(data.weekdays) ? data.weekdays : [1, 2, 3, 4, 5, 6, 7],
            scheduleType,
            targetCount: Math.min(Math.max(1, Math.round(rawTarget)), maxTarget(scheduleType)),
            archived: data.archived === true,
            createdAt: data.createdAt ?? null,
            updatedAt: data.updatedAt ?? null,
          } satisfies Habit;
        });
        next.sort((a, b) => {
          const aTime = a.createdAt?.toMillis() ?? Number.MAX_SAFE_INTEGER;
          const bTime = b.createdAt?.toMillis() ?? Number.MAX_SAFE_INTEGER;
          return aTime - bTime || a.name.localeCompare(b.name);
        });
        setHabits(next);
        setHabitsReady(true);
      },
      (snapshotError) => {
        setError(snapshotError.message);
        setHabitsReady(true);
      },
    );
    return unsubscribe;
  }, [uid]);

  // Load enough history for the longest recurrence in use.
  const historyStart = useMemo(
    () => historyStartFor(habits.map(scheduleFromHabit), today),
    [habits, today],
  );

  useEffect(() => {
    if (!uid) return;
    setLogsReady(false);
    const logsRef = collection(db, 'users', uid, 'habitLogs');
    // Range query on `date`: only the needed history is loaded.
    const logsQuery = query(logsRef, where('date', '>=', historyStart));
    const unsubscribe = onSnapshot(
      logsQuery,
      (snapshot) => {
        const next = new Map<string, Set<string>>();
        snapshot.docs.forEach((document) => {
          const data = document.data();
          const habitId = typeof data.habitId === 'string' ? data.habitId : '';
          const date = typeof data.date === 'string' ? data.date : '';
          if (!habitId || !date) return;
          const existing = next.get(habitId) ?? new Set<string>();
          existing.add(date);
          next.set(habitId, existing);
        });
        setLogs(next);
        setLogsReady(true);
      },
      (snapshotError) => {
        setError(snapshotError.message);
        setLogsReady(true);
      },
    );
    return unsubscribe;
  }, [uid, historyStart]);

  const addHabit = useCallback(
    async (input: HabitInput) => {
      if (!uid) throw new Error('Not signed in');
      await addDoc(collection(db, 'users', uid, 'habits'), {
        name: input.name.trim(),
        weekdays:
          input.scheduleType === 'weekdays'
            ? [...input.weekdays].sort((a, b) => a - b)
            : [],
        scheduleType: input.scheduleType,
        targetCount: Math.min(Math.max(1, input.targetCount), maxTarget(input.scheduleType)),
        archived: false,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
    },
    [uid],
  );

  const updateHabit = useCallback(
    async (habitId: string, input: HabitInput) => {
      if (!uid) throw new Error('Not signed in');
      await updateDoc(doc(db, 'users', uid, 'habits', habitId), {
        name: input.name.trim(),
        weekdays:
          input.scheduleType === 'weekdays'
            ? [...input.weekdays].sort((a, b) => a - b)
            : [],
        scheduleType: input.scheduleType,
        targetCount: Math.min(Math.max(1, input.targetCount), maxTarget(input.scheduleType)),
        updatedAt: serverTimestamp(),
      });
    },
    [uid],
  );

  const setArchived = useCallback(
    async (habitId: string, archived: boolean) => {
      if (!uid) throw new Error('Not signed in');
      await updateDoc(doc(db, 'users', uid, 'habits', habitId), {
        archived,
        updatedAt: serverTimestamp(),
      });
    },
    [uid],
  );

  const toggleLog = useCallback(
    async (habitId: string, date: string, done: boolean) => {
      if (!uid) throw new Error('Not signed in');
      if (date > toDateKey(new Date())) throw new Error('Future days cannot be checked in');
      const logRef = doc(db, 'users', uid, 'habitLogs', logDocumentId(habitId, date));
      if (done) {
        // Deterministic document id means two devices mark the same day once.
        await setDoc(logRef, {
          habitId,
          date,
          createdAt: serverTimestamp(),
        });
      } else {
        await deleteDoc(logRef);
      }
    },
    [uid],
  );

  const value = useMemo<HabitsState>(
    () => ({
      habits,
      logs,
      loading: Boolean(uid) && !(habitsReady && logsReady),
      error,
      addHabit,
      updateHabit,
      archiveHabit: (habitId) => setArchived(habitId, true),
      restoreHabit: (habitId) => setArchived(habitId, false),
      toggleLog,
    }),
    [
      habits,
      logs,
      uid,
      habitsReady,
      logsReady,
      error,
      addHabit,
      updateHabit,
      setArchived,
      toggleLog,
    ],
  );

  return <HabitsContext.Provider value={value}>{children}</HabitsContext.Provider>;
}

export function useHabits(): HabitsState {
  const context = useContext(HabitsContext);
  if (!context) throw new Error('useHabits must be used inside HabitsProvider');
  return context;
}

export function useLogsFor(habitId: string): ReadonlySet<string> {
  const { logs } = useHabits();
  return logs.get(habitId) ?? EMPTY_LOGS;
}

const EMPTY_LOGS: ReadonlySet<string> = new Set<string>();
