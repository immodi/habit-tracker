import { useEffect, useState } from 'react';
import { signOut } from 'firebase/auth';
import { auth } from '../firebase';
import { useAuth } from '../hooks/useAuth';
import { downloadJson, exportAllData } from '../lib/export';
import { LoadingBlock } from '../components/AppShell';
import { DownloadIcon, LogOutIcon } from '../components/icons';

export function Settings() {
  const { user } = useAuth();
  const [exporting, setExporting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    document.title = 'Settings · Habits';
  }, []);

  async function handleExport() {
    if (!user) return;
    setExporting(true);
    setError(null);
    setMessage(null);
    try {
      const bundle = await exportAllData(user.uid);
      downloadJson(bundle);
      setMessage(
        `Exported ${bundle.habits.length} habit${bundle.habits.length === 1 ? '' : 's'} and ${bundle.habitLogs.length} check-in${bundle.habitLogs.length === 1 ? '' : 's'}.`,
      );
    } catch (exportError) {
      setError(exportError instanceof Error ? exportError.message : 'Export failed.');
    } finally {
      setExporting(false);
    }
  }

  if (!user) return <div className="page"><LoadingBlock label="Loading account" /></div>;

  return (
    <div className="page">
      <section className="page-head">
        <p className="eyebrow">Your account</p>
        <h1 className="page-title">Settings</h1>
      </section>

      <div className="list">
        <div className="list-row">
          <div>
            <p className="row-title">Signed in as</p>
            <p className="row-sub">{user.email ?? user.uid}</p>
          </div>
        </div>

        <div className="list-row">
          <div>
            <p className="row-title">Reminders</p>
            <p className="row-sub">
              Reminders are Android only. Set a time in the Android app and it will nudge you
              when habits are left unchecked.
            </p>
          </div>
        </div>

        <div className="list-row">
          <div>
            <p className="row-title">Export data</p>
            <p className="row-sub">Download every habit and check-in as JSON.</p>
          </div>
          <button
            type="button"
            className="btn btn-ghost btn-small"
            onClick={() => void handleExport()}
            disabled={exporting}
          >
            <DownloadIcon size={16} />
            {exporting ? 'Preparing…' : 'Export'}
          </button>
        </div>

        <div className="list-row">
          <div>
            <p className="row-title">Sign out</p>
            <p className="row-sub">Your data stays safe in your account.</p>
          </div>
          <button
            type="button"
            className="btn btn-ghost btn-small"
            onClick={() => void signOut(auth)}
          >
            <LogOutIcon size={16} />
            Sign out
          </button>
        </div>
      </div>

      {message ? <div className="notice notice-ok">{message}</div> : null}
      {error ? <div className="notice notice-error">{error}</div> : null}

      <p className="page-footnote">
        Habit Tracker · synced with Firebase. Private to your account.
      </p>
    </div>
  );
}
