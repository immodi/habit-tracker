import { Timestamp, collection, getDocs } from 'firebase/firestore';
import { db } from '../firebase';
import { todayKey } from './dates';

function serializeValue(value: unknown): unknown {
  if (value instanceof Timestamp) return value.toDate().toISOString();
  if (Array.isArray(value)) return value.map(serializeValue);
  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value as Record<string, unknown>).map(([key, entry]) => [
        key,
        serializeValue(entry),
      ]),
    );
  }
  return value;
}

export interface ExportBundle {
  exportedAt: string;
  app: 'habit-tracker';
  habits: Array<Record<string, unknown> & { id: string }>;
  habitLogs: Array<Record<string, unknown> & { id: string }>;
}

/** Reads every habit and check-in (not just the 90-day window) for a full backup. */
export async function exportAllData(uid: string): Promise<ExportBundle> {
  const [habitsSnapshot, logsSnapshot] = await Promise.all([
    getDocs(collection(db, 'users', uid, 'habits')),
    getDocs(collection(db, 'users', uid, 'habitLogs')),
  ]);

  return {
    exportedAt: new Date().toISOString(),
    app: 'habit-tracker',
    habits: habitsSnapshot.docs.map((document) => ({
      id: document.id,
      ...(serializeValue(document.data()) as Record<string, unknown>),
    })),
    habitLogs: logsSnapshot.docs.map((document) => ({
      id: document.id,
      ...(serializeValue(document.data()) as Record<string, unknown>),
    })),
  };
}

export function downloadJson(bundle: ExportBundle): void {
  const blob = new Blob([JSON.stringify(bundle, null, 2)], {
    type: 'application/json',
  });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = `habit-tracker-${todayKey()}.json`;
  document.body.append(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}
