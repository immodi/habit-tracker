import { NavLink, Outlet } from 'react-router-dom';
import { HabitsProvider } from '../hooks/useHabits';
import { ArchiveIcon, CheckIcon, HabitListIcon, SettingsIcon } from './icons';

const navItems = [
  { to: '/', label: 'Today', icon: HabitListIcon, end: true },
  { to: '/archived', label: 'Archived', icon: ArchiveIcon, end: false },
  { to: '/settings', label: 'Settings', icon: SettingsIcon, end: false },
];

export function AppShell() {
  return (
    <HabitsProvider>
      <div className="shell">
        <header className="topbar">
          <div className="topbar-inner">
            <NavLink to="/" className="brand" aria-label="Habit Tracker home">
              <span className="brand-mark">
                <CheckIcon size={16} />
              </span>
              <span>Habits</span>
            </NavLink>
            <nav className="nav" aria-label="Main">
              {navItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) => `nav-link${isActive ? ' is-active' : ''}`}
                  aria-label={item.label}
                  title={item.label}
                >
                  <item.icon size={20} />
                </NavLink>
              ))}
            </nav>
          </div>
        </header>
        <main className="main">
          <Outlet />
        </main>
      </div>
    </HabitsProvider>
  );
}

export function BootScreen() {
  return (
    <div className="boot" role="status" aria-label="Loading">
      <span className="spinner" aria-hidden="true" />
    </div>
  );
}

export function LoadingBlock({ label = 'Loading' }: { label?: string }) {
  return (
    <div className="loading-block" role="status">
      <span className="spinner" aria-hidden="true" />
      <span>{label}…</span>
    </div>
  );
}
