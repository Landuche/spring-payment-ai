import { isTransferType, type Transaction } from './types';

/** Chave das preferências com os ids de notificações já vistas. */
export const SEEN_KEY = 'fintech.notifications.seen.v1';

/** Limite do histórico de ids vistos: o suficiente para o extrato de um uso normal, sem crescer para sempre. */
const MAX_SEEN = 200;

/**
 * Notificações são as movimentações de transferência (enviadas e recebidas), da mais recente para a mais antiga.
 * O backend não tem canal de notificação: o app deriva a lista do extrato, quando está aberto.
 */
export function notificationsFrom(transactions: readonly Transaction[]): readonly Transaction[] {
  return transactions
    .filter((t) => isTransferType(t.type))
    .slice()
    .sort((a, b) => b.occurredAt.localeCompare(a.occurredAt) || (b.createdAt ?? '').localeCompare(a.createdAt ?? ''));
}

/** Atualiza a lista de vistos com os itens atuais na frente, sem duplicar e respeitando o limite. */
export function mergeSeen(seen: readonly string[], items: readonly Transaction[]): readonly string[] {
  const ids = [...items.map((item) => item.id), ...seen];
  return [...new Set(ids)].slice(0, MAX_SEEN);
}

/** Lê a preferência guardada. Conteúdo inválido vira lista vazia, nunca erro. */
export function parseSeen(raw: string | null): readonly string[] {
  if (!raw) return [];
  try {
    const parsed: unknown = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((v): v is string => typeof v === 'string') : [];
  } catch {
    return [];
  }
}
