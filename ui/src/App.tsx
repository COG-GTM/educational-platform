import { Link, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth/AuthContext';
import CatalogPage from './pages/CatalogPage';
import CourseDetailPage from './pages/CourseDetailPage';
import CourseReviewsPage from './pages/CourseReviewsPage';
import LoginPage from './pages/LoginPage';

function RedirectToCatalog() {
  const location = useLocation();
  return <Navigate to={{ pathname: '/catalog', search: location.search }} replace />;
}

function HeaderNav() {
  const { user, signOut } = useAuth();
  const location = useLocation();
  return (
    <nav className="app-nav" aria-label="Main">
      <Link to="/catalog">Catalog</Link>
      {user ? (
        <span className="app-nav-user">
          <span>Signed in as {user.username}</span>
          <button type="button" onClick={signOut}>
            Sign out
          </button>
        </span>
      ) : (
        <Link to={`/login?redirect=${encodeURIComponent(location.pathname)}`}>Sign in</Link>
      )}
    </nav>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <div className="app">
        <header className="app-header">
          <h1>Educational Platform</h1>
          <HeaderNav />
        </header>
        <main>
          <Routes>
            <Route path="/catalog" element={<CatalogPage />} />
            <Route path="/courses/:uuid" element={<CourseDetailPage />} />
            <Route path="/courses/:uuid/reviews" element={<CourseReviewsPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="*" element={<RedirectToCatalog />} />
          </Routes>
        </main>
      </div>
    </AuthProvider>
  );
}
