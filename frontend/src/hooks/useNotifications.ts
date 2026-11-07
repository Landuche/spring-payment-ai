import { useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { api } from '@/lib/api';
import type { AppNotification, NotificationsResponse } from '@/lib/types';

/** Sem rede ou com erro do servidor, devolve null e a tela segue com o que já tinha. */
async function fetchNotifications(): Promise<NotificationsResponse | null> {
  try {
    return await api.getNotifications();
  } catch {
    return null;
  }
}

/**
 * Notificações da conta e quantas ainda não foram lidas, ambas vindas do servidor (vale em qualquer aparelho).
 * `fresh` guarda, só enquanto o hook está montado, o que estava não lido quando foi marcado como lido.
 */
export function useNotifications() {
  const [items, setItems] = useState<readonly AppNotification[]>([]);
  const [unread, setUnread] = useState(0);
  const [loading, setLoading] = useState(true);
  const [fresh, setFresh] = useState<ReadonlySet<string>>(new Set());

  // Cada tela tem a sua cópia, e o início continua montado por baixo das notificações: relê a cada foco para o
  // contador do sino refletir o que foi lido e o que chegou enquanto a tela estava fora de foco.
  useFocusEffect(
    useCallback(() => {
      let alive = true;
      void fetchNotifications().then((response) => {
        if (!alive) return;
        if (response) {
          setItems(response.items);
          setUnread(response.unread);
        }
        setLoading(false);
      });
      return () => {
        alive = false;
      };
    }, []),
  );

  const refresh = useCallback(async () => {
    setLoading(true);
    const response = await fetchNotifications();
    if (response) {
      setItems(response.items);
      setUnread(response.unread);
    }
    setLoading(false);
  }, []);

  const markAllRead = useCallback(() => {
    const newIds = items.filter((item) => !item.read).map((item) => item.id);
    setFresh((previous) => new Set([...previous, ...newIds]));
    setItems((previous) => previous.map((item) => ({ ...item, read: true })));
    setUnread(0);
    // Se a chamada falhar, a próxima leitura traz o contador de volta.
    void api.markNotificationsRead().catch(() => undefined);
  }, [items]);

  return { items, unread, loading, fresh, refresh, markAllRead };
}
