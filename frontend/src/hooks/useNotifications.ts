import { useFocusEffect } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import { mergeSeen, notificationsFrom, parseSeen, SEEN_KEY } from '@/lib/notifications';
import { prefsStorage } from '@/lib/prefs-storage';
import type { Transaction } from '@/lib/types';

/**
 * Notificações de transferência e quantas ainda não foram vistas.
 * Enquanto as preferências não carregam, o contador fica em zero, para não piscar um número falso.
 * `fresh` guarda, só enquanto o hook está montado, o que estava não lido quando foi marcado como visto.
 */
export function useNotifications(transactions: readonly Transaction[]) {
  const items = useMemo(() => notificationsFrom(transactions), [transactions]);
  const [seen, setSeen] = useState<readonly string[] | null>(null);
  const [fresh, setFresh] = useState<ReadonlySet<string>>(new Set());

  // Relê a cada vez que a tela ganha foco. Cada tela tem a sua cópia de `seen`, e o início continua montado por
  // baixo das notificações: sem reler, ele nunca saberia que as notificações foram vistas e o contador não zeraria.
  useFocusEffect(
    useCallback(() => {
      let alive = true;
      void prefsStorage.read(SEEN_KEY).then((raw) => {
        if (alive) setSeen(parseSeen(raw));
      });
      return () => {
        alive = false;
      };
    }, []),
  );

  const unread = useMemo(
    () => (seen === null ? 0 : items.filter((item) => !seen.includes(item.id)).length),
    [items, seen],
  );

  const markAllSeen = useCallback(() => {
    const current = seen ?? [];
    const newlySeen = items.filter((item) => !current.includes(item.id)).map((item) => item.id);
    setFresh((previous) => new Set([...previous, ...newlySeen]));
    const next = mergeSeen(current, items);
    setSeen(next);
    void prefsStorage.write(SEEN_KEY, JSON.stringify(next));
  }, [seen, items]);

  return { items, unread, fresh, markAllSeen };
}
