import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { api, ApiError, setAccessToken, setUnauthorizedHandler, type Session } from './api';
import { sessionStorage } from './session-storage';

interface AuthContextValue {
  readonly session: Session | null;
  readonly restoring: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (name: string, email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

interface StoredSession {
  readonly session: Session;
  readonly issuedAt: number;
}

type RenewResult = 'ok' | 'expired' | 'offline';

const isAccessValid = (stored: StoredSession, now: number) =>
  now < stored.issuedAt + stored.session.expiresIn * 1000;
const isRefreshValid = (stored: StoredSession, now: number) =>
  now < stored.issuedAt + stored.session.refreshExpiresIn * 1000;

export function AuthProvider({ children }: { readonly children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [restoring, setRestoring] = useState(true);

  const sessionRef = useRef<Session | null>(null);

  const renewing = useRef<Promise<RenewResult> | null>(null);

  const persist = useCallback(async (next: Session | null) => {
    sessionRef.current = next;
    setSession(next);
    setAccessToken(next?.accessToken ?? null);
    if (next) {
      await sessionStorage.write(JSON.stringify({ session: next, issuedAt: Date.now() } satisfies StoredSession));
    } else {
      await sessionStorage.clear();
    }
  }, []);

  const renew = useCallback((): Promise<RenewResult> => {
    if (!renewing.current) {
      renewing.current = (async (): Promise<RenewResult> => {
        const current = sessionRef.current;
        if (!current) return 'expired';
        try {
          await persist(await api.refresh(current.refreshToken));
          return 'ok';
        } catch (error) {
          if (error instanceof ApiError && error.status === 0) return 'offline';
          await persist(null);
          return 'expired';
        }
      })().finally(() => {
        renewing.current = null;
      });
    }
    return renewing.current;
  }, [persist]);

  const signOut = useCallback(async () => {
    const current = sessionRef.current;
    if (current) {
      await api.logout(current.refreshToken);
    }
    await persist(null);
  }, [persist]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const raw = await sessionStorage.read();
      if (raw) {
        try {
          const stored = JSON.parse(raw) as StoredSession;
          const now = Date.now();
          if (!isRefreshValid(stored, now)) {
            await sessionStorage.clear();
          } else if (isAccessValid(stored, now)) {
            sessionRef.current = stored.session;
            setAccessToken(stored.session.accessToken);
            if (!cancelled) setSession(stored.session);
          } else {
            sessionRef.current = stored.session;
            const result = await renew();
            if (result === 'offline' && !cancelled) {
              setSession(stored.session);
              setAccessToken(stored.session.accessToken);
            }
          }
        } catch {
          await sessionStorage.clear();
        }
      }
      if (!cancelled) setRestoring(false);
    })();
    return () => {
      cancelled = true;
    };
  }, [renew]);

  useEffect(() => {
    setUnauthorizedHandler(async () => (await renew()) === 'ok');
    return () => setUnauthorizedHandler(null);
  }, [renew]);

  const signIn = useCallback(
    async (email: string, password: string) => {
      await persist(await api.login({ email, password }));
    },
    [persist],
  );

  const signUp = useCallback(
    async (name: string, email: string, password: string) => {
      await persist(await api.register({ name, email, password }));
    },
    [persist],
  );

  const value = useMemo<AuthContextValue>(
    () => ({ session, restoring, signIn, signUp, signOut }),
    [session, restoring, signIn, signUp, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth precisa estar dentro de AuthProvider');
  }
  return context;
}
