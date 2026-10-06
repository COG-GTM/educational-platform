import { post } from './http';

export function signIn(username: string, password: string): Promise<string> {
  return post<string>('/api/users/sign-in', { username, password }, 'text');
}
