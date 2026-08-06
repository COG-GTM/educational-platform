import { Navigate, Route, Routes } from 'react-router-dom';
import CatalogPage from './pages/CatalogPage';

export default function App() {
  return (
    <div className="app">
      <header className="app-header">
        <h1>Educational Platform</h1>
      </header>
      <main>
        <Routes>
          <Route path="/catalog" element={<CatalogPage />} />
          <Route path="*" element={<Navigate to="/catalog" replace />} />
        </Routes>
      </main>
    </div>
  );
}
