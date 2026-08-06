import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import CatalogPage from './pages/CatalogPage';

function RedirectToCatalog() {
  const location = useLocation();
  return <Navigate to={{ pathname: '/catalog', search: location.search }} replace />;
}

export default function App() {
  return (
    <div className="app">
      <header className="app-header">
        <h1>Educational Platform</h1>
      </header>
      <main>
        <Routes>
          <Route path="/catalog" element={<CatalogPage />} />
          <Route path="*" element={<RedirectToCatalog />} />
        </Routes>
      </main>
    </div>
  );
}
