import { FormEvent, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { HttpError } from '../api/http';
import { useAuth } from '../auth/AuthContext';

function safeRedirect(value: string | null): string {
  return value && value.startsWith('/') && !value.startsWith('//') ? value : '/catalog';
}

export default function LoginPage() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const redirectTo = safeRedirect(searchParams.get('redirect'));
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await signIn(username.trim(), password);
      navigate(redirectTo, { replace: true });
    } catch (e) {
      if (e instanceof HttpError && [400, 401, 403, 422].includes(e.status)) {
        setError('Wrong username or password.');
      } else {
        setError('Could not sign you in right now. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="login">
      <h2>Sign in</h2>
      <p className="login-hint">Sign in to enroll in courses and track your learning.</p>
      <form className="login-form" onSubmit={onSubmit} aria-describedby={error ? 'login-error' : undefined}>
        <label htmlFor="login-username">Username</label>
        <input
          id="login-username"
          name="username"
          autoComplete="username"
          required
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />
        <label htmlFor="login-password">Password</label>
        <input
          id="login-password"
          name="password"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {error && (
          <p id="login-error" className="form-error" role="alert">
            {error}
          </p>
        )}
        <button type="submit" disabled={submitting}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <p className="login-hint">
        <Link to={redirectTo}>Back</Link>
      </p>
    </section>
  );
}
