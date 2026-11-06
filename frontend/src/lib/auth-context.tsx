import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { api, ApiError, setAccessToken, setUnauthorizedHandler, type Session } from './api';
import { sessionStorage } from './session-storage';

interface AuthContextValue {
  readonly session: Session | null;
  /** true enquanto a sessão persistida ainda está sendo lida (e possivelmente renovada) na inicialização. */
  readonly restoring: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (name: string, email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/** Sessão guardada com o instante de emissão, para decidir se o access token ainda vale. */
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

  // Espelho síncrono da sessão: a renovação precisa do refresh token atual sem depender de re-render.
  const sessionRef = useRef<Session | null>(null);
  // Uma única renovação em voo: duas requisições 401 paralelas não podem disparar dois refresh
  // (o segundo usaria um token já trocado e derrubaria a sessão pela regra de reuso).
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

  /** Troca o refresh token por um novo par. 'expired' encerra a sessão local; 'offline' mantém para tentar depois. */
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

  // Restaura a sessão ao abrir o app: usa o access token se ainda vale; senão renova com o refresh token.
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
              // Sem rede no boot: mantém a sessão; a próxima chamada tenta renovar de novo.
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

  // 401 em requisição autenticada: tenta renovar uma vez; a API repete a chamada se der certo.
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
