import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { signIn as signInRequest } from '../api/auth';
import { getToken, isTokenExpired, setToken, TOKEN_CLEARED_EVENT } from '../api/http';

export interface AuthUser {
  username: string;
  roles: string[];
}

interface AuthContextValue {
  user: AuthUser | null;
  isStudent: boolean;
  signIn: (username: string, password: string) => Promise<void>;
  signOut: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

interface JwtPayload {
  sub?: string;
  exp?: number;
  auth?: { authority: string }[];
}

export function decodeToken(token: string): AuthUser | null {
  try {
    const [, payload] = token.split('.');
    if (!payload) return null;
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = JSON.parse(atob(base64)) as JwtPayload;
    if (!json.sub) return null;
    if (isTokenExpired(token)) return null;
    return { username: json.sub, roles: (json.auth ?? []).map((a) => a.authority) };
  } catch {
    return null;
  }
}

function userFromStorage(): AuthUser | null {
  const token = getToken();
  if (!token) return null;
  const user = decodeToken(token);
  if (!user) setToken(null);
  return user;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(userFromStorage);

  useEffect(() => {
    const onCleared = () => setUser(null);
    window.addEventListener(TOKEN_CLEARED_EVENT, onCleared);
    return () => window.removeEventListener(TOKEN_CLEARED_EVENT, onCleared);
  }, []);

  const signIn = useCallback(async (username: string, password: string) => {
    const token = await signInRequest(username, password);
    const decoded = decodeToken(token);
    if (!decoded) {
      throw new Error('Received an invalid session token');
    }
    setToken(token);
    setUser(decoded);
  }, []);

  const signOut = useCallback(() => {
    setToken(null);
    setUser(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ user, isStudent: user?.roles.includes('ROLE_STUDENT') ?? false, signIn, signOut }),
    [user, signIn, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
