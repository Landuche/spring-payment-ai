import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { AppState } from 'react-native';
import { api } from './api';
import { useAuth } from './auth-context';
import { connectNotificationSocket } from './notification-socket';
import type { AppNotification, NotificationsResponse } from './types';

interface NotificationsValue {
  readonly items: readonly AppNotification[];
  readonly unread: number;
  readonly loading: boolean;
  /** Ids que estavam não lidos quando a lista foi marcada como lida, e ficam destacados enquanto a tela está aberta. */
  readonly fresh: ReadonlySet<string>;
  /** Quantos avisos de novidade chegaram pelo WebSocket. Telas com dados que mudam junto (saldo, extrato) reagem a isso. */
  readonly changes: number;
  refresh: () => Promise<void>;
  markAllRead: () => void;
}

const NotificationsContext = createContext<NotificationsValue | null>(null);

/** Sem rede ou com erro do servidor, devolve null e a tela segue com o que já tinha. */
async function fetchNotifications(): Promise<NotificationsResponse | null> {
  try {
    return await api.getNotifications();
  } catch {
    return null;
  }
}

/**
 * Dono das notificações do app: uma única conexão WebSocket avisa quando chega algo novo, e a lista e o contador
 * (do sino e da tela) vêm todos daqui. A lista é relida também ao conectar e quando o app volta do segundo plano,
 * porque o aviso em tempo real só vale enquanto a conexão está aberta.
 */
export function NotificationsProvider({ children }: { readonly children: ReactNode }) {
  const { session, getAccessToken } = useAuth();
  const signedIn = session !== null;
  const [items, setItems] = useState<readonly AppNotification[]>([]);
  const [unread, setUnread] = useState(0);
  const [loading, setLoading] = useState(true);
  const [fresh, setFresh] = useState<ReadonlySet<string>>(new Set());
  const [changes, setChanges] = useState(0);

  const apply = useCallback((response: NotificationsResponse | null) => {
    if (response) {
      setItems(response.items);
      setUnread(response.unread);
    }
    setLoading(false);
  }, []);

  const refresh = useCallback(async () => {
    setLoading(true);
    apply(await fetchNotifications());
  }, [apply]);

  useEffect(() => {
    if (!signedIn) return;
    let alive = true;
    const reload = () => {
      void fetchNotifications().then((response) => {
        if (alive) apply(response);
      });
    };
    reload();
    const onChange = () => {
      reload();
      setChanges((count) => count + 1);
    };
    const closeSocket = connectNotificationSocket({ getToken: getAccessToken, onReady: reload, onChange });
    const appState = AppState.addEventListener('change', (state) => {
      if (state === 'active') reload();
    });
    return () => {
      alive = false;
      closeSocket();
      appState.remove();
    };
  }, [signedIn, getAccessToken, apply]);

  const markAllRead = useCallback(() => {
    const newIds = items.filter((item) => !item.read).map((item) => item.id);
    setFresh((previous) => new Set([...previous, ...newIds]));
    setItems((previous) => previous.map((item) => ({ ...item, read: true })));
    setUnread(0);
    // Se a chamada falhar, a próxima leitura traz o contador de volta.
    void api.markNotificationsRead().catch(() => undefined);
  }, [items]);

  const value = useMemo<NotificationsValue>(
    () => ({ items, unread, loading, fresh, changes, refresh, markAllRead }),
    [items, unread, loading, fresh, changes, refresh, markAllRead],
  );

  return <NotificationsContext.Provider value={value}>{children}</NotificationsContext.Provider>;
}

export function useNotifications(): NotificationsValue {
  const context = useContext(NotificationsContext);
  if (!context) {
    throw new Error('useNotifications precisa estar dentro de NotificationsProvider');
  }
  return context;
}
