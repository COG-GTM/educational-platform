import { Link, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth/AuthContext';
import CatalogPage from './pages/CatalogPage';
import CourseDetailPage from './pages/CourseDetailPage';
import CourseReviewsPage from './pages/CourseReviewsPage';
import LearningCoursePage from './pages/LearningCoursePage';
import LoginPage from './pages/LoginPage';
import MyLearningPage from './pages/MyLearningPage';

function RedirectToCatalog() {
  const location = useLocation();
  return <Navigate to={{ pathname: '/catalog', search: location.search }} replace />;
}

function HeaderNav() {
  const { user, isStudent, signOut } = useAuth();
  const location = useLocation();
  return (
    <nav className="app-nav" aria-label="Main">
      <Link to="/catalog">Catalog</Link>
      {user && isStudent && <Link to="/learning">My Learning</Link>}
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
            <Route path="/learning" element={<MyLearningPage />} />
            <Route path="/learning/:uuid" element={<LearningCoursePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="*" element={<RedirectToCatalog />} />
          </Routes>
        </main>
      </div>
    </AuthProvider>
  );
}
