import { isTransferType, type Transaction } from './types';

export const SEEN_KEY = 'fintech.notifications.seen.v1';

const MAX_SEEN = 200;

export function notificationsFrom(transactions: readonly Transaction[]): readonly Transaction[] {
  return transactions
    .filter((t) => isTransferType(t.type))
    .slice()
    .sort((a, b) => b.occurredAt.localeCompare(a.occurredAt) || (b.createdAt ?? '').localeCompare(a.createdAt ?? ''));
}

export function mergeSeen(seen: readonly string[], items: readonly Transaction[]): readonly string[] {
  const ids = [...items.map((item) => item.id), ...seen];
  return [...new Set(ids)].slice(0, MAX_SEEN);
}

export function parseSeen(raw: string | null): readonly string[] {
  if (!raw) return [];
  try {
    const parsed: unknown = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((v): v is string => typeof v === 'string') : [];
  } catch {
    return [];
  }
}
