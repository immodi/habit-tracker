import { useEffect, useState, type FormEvent } from 'react';
import { Navigate } from 'react-router-dom';
import {
  GoogleAuthProvider,
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  signInWithPopup,
} from 'firebase/auth';
import { auth } from '../firebase';
import { useAuth } from '../hooks/useAuth';
import { BootScreen } from '../components/AppShell';
import { CheckIcon, GoogleIcon } from '../components/icons';

type Mode = 'signin' | 'register';

function authErrorMessage(error: unknown): string {
  const code = typeof error === 'object' && error && 'code' in error ? String(error.code) : '';
  switch (code) {
    case 'auth/invalid-credential':
    case 'auth/user-not-found':
    case 'auth/wrong-password':
      return 'That email and password combination does not match an account.';
    case 'auth/email-already-in-use':
      return 'An account with this email already exists. Try signing in instead.';
    case 'auth/weak-password':
      return 'Choose a password with at least 6 characters.';
    case 'auth/invalid-email':
      return 'That email address does not look right.';
    case 'auth/too-many-requests':
      return 'Too many attempts. Wait a moment and try again.';
    case 'auth/popup-closed-by-user':
    case 'auth/cancelled-popup-request':
      return 'Google sign-in was closed before it finished.';
    case 'auth/popup-blocked':
      return 'Your browser blocked the sign-in pop-up. Allow pop-ups and try again.';
    case 'auth/operation-not-allowed':
    case 'auth/configuration-not-found':
      return 'This sign-in method is not enabled for the project yet.';
    default:
      return error instanceof Error ? error.message : 'Sign-in failed. Please try again.';
  }
}

export function SignIn() {
  const { user, loading } = useAuth();
  const [mode, setMode] = useState<Mode>('signin');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    document.title = 'Sign in · Habits';
  }, []);

  if (loading) return <BootScreen />;
  if (user) return <Navigate to="/" replace />;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (mode === 'signin') {
        await signInWithEmailAndPassword(auth, email.trim(), password);
      } else {
        await createUserWithEmailAndPassword(auth, email.trim(), password);
      }
    } catch (submitError) {
      setError(authErrorMessage(submitError));
      setBusy(false);
    }
  }

  async function google() {
    setBusy(true);
    setError(null);
    try {
      const provider = new GoogleAuthProvider();
      await signInWithPopup(auth, provider);
    } catch (googleError) {
      setError(authErrorMessage(googleError));
      setBusy(false);
    }
  }

  return (
    <div className="auth">
      <div className="auth-inner">
        <div className="auth-brand">
          <span className="brand-mark brand-mark-lg">
            <CheckIcon size={22} />
          </span>
          <div>
            <h1>Habits</h1>
            <p>Small days, kept.</p>
          </div>
        </div>

        <form className="auth-card" onSubmit={submit}>
          <h2>{mode === 'signin' ? 'Welcome back' : 'Create your account'}</h2>

          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              className="text-input"
              type="email"
              inputMode="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="you@example.com"
              required
            />
          </div>

          <div className="field">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              className="text-input"
              type="password"
              autoComplete={mode === 'signin' ? 'current-password' : 'new-password'}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder={mode === 'signin' ? 'Your password' : 'At least 6 characters'}
              minLength={6}
              required
            />
          </div>

          {error ? <div className="notice notice-error">{error}</div> : null}

          <button type="submit" className="btn btn-primary btn-block" disabled={busy}>
            {busy ? 'Working…' : mode === 'signin' ? 'Sign in' : 'Create account'}
          </button>

          <div className="or">
            <span>or</span>
          </div>

          <button
            type="button"
            className="btn btn-google btn-block"
            onClick={() => void google()}
            disabled={busy}
          >
            <GoogleIcon size={18} />
            Continue with Google
          </button>

          <p className="auth-switch">
            {mode === 'signin' ? 'New here?' : 'Already have an account?'}{' '}
            <button
              type="button"
              className="link"
              onClick={() => {
                setMode(mode === 'signin' ? 'register' : 'signin');
                setError(null);
              }}
            >
              {mode === 'signin' ? 'Create an account' : 'Sign in'}
            </button>
          </p>
        </form>

        <p className="auth-foot">Only you can read your habits and check-ins.</p>
      </div>
    </div>
  );
}
