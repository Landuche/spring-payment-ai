import { useFocusEffect } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import { mergeSeen, notificationsFrom, parseSeen, SEEN_KEY } from '@/lib/notifications';
import { prefsStorage } from '@/lib/prefs-storage';
import type { Transaction } from '@/lib/types';

export function useNotifications(transactions: readonly Transaction[]) {
  const items = useMemo(() => notificationsFrom(transactions), [transactions]);
  const [seen, setSeen] = useState<readonly string[] | null>(null);
  const [fresh, setFresh] = useState<ReadonlySet<string>>(new Set());

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
