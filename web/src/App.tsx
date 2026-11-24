import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from './hooks/useAuth';
import { AppShell, BootScreen } from './components/AppShell';
import { SignIn } from './pages/SignIn';
import { Today } from './pages/Today';
import { HabitEditor } from './pages/HabitEditor';
import { HabitDetail } from './pages/HabitDetail';
import { Archived } from './pages/Archived';
import { Settings } from './pages/Settings';

function RequireAuth() {
  const { user, loading } = useAuth();
  if (loading) return <BootScreen />;
  if (!user) return <Navigate to="/signin" replace />;
  return <Outlet />;
}

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/signin" element={<SignIn />} />
        <Route element={<RequireAuth />}>
          <Route element={<AppShell />}>
            <Route index element={<Today />} />
            <Route path="habits/new" element={<HabitEditor />} />
            <Route path="habits/:habitId" element={<HabitDetail />} />
            <Route path="habits/:habitId/edit" element={<HabitEditor />} />
            <Route path="archived" element={<Archived />} />
            <Route path="settings" element={<Settings />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
