import { Platform } from 'react-native';
import { API_URL } from './config';
import type {
  AccountInfo,
  AssistantReply,
  Balance,
  Category,
  ExpenseReport,
  NewTransaction,
  NotificationsResponse,
  Simulation,
  Transaction,
  TransferEventType,
  TransferResult,
  VoiceReply,
} from './types';

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly retryAfterSeconds?: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export function describeWait(seconds: number): string {
  if (seconds >= 3600) {
    const hours = Math.ceil(seconds / 3600);
    return hours === 1 ? 'cerca de 1 hora' : `cerca de ${hours} horas`;
  }
  const minutes = Math.max(1, Math.ceil(seconds / 60));
  return `${minutes} minuto${minutes === 1 ? '' : 's'}`;
}

let accessToken: string | null = null;
let onUnauthorized: (() => Promise<boolean>) | null = null;

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function setUnauthorizedHandler(handler: (() => Promise<boolean>) | null): void {
  onUnauthorized = handler;
}

function authHeaders(): Record<string, string> {
  return accessToken ? { Authorization: `Bearer ${accessToken}` } : {};
}

async function toApiError(response: Response): Promise<ApiError> {
  let message = `Erro ${response.status}`;
  try {
    const body = (await response.json()) as { detail?: string; title?: string };
    message = body.detail ?? body.title ?? message;
  } catch {
    // mensagem genérica
  }
  const retryAfter = Number(response.headers.get('Retry-After'));
  return new ApiError(message, response.status, Number.isFinite(retryAfter) && retryAfter > 0 ? retryAfter : undefined);
}

const IS_WEB = Platform.OS === 'web';

const RETRY_DELAYS_MS = [400, 1200] as const;

const TRANSIENT_STATUS = new Set([502, 503, 504]);

function isSafeToRetry(init: RequestInit): boolean {
  const method = (init.method ?? 'GET').toUpperCase();
  const headers = init.headers as Record<string, string> | undefined;
  return method === 'GET' || headers?.['Idempotency-Key'] !== undefined;
}

const sleep = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms));

async function fetchOnce(path: string, init: RequestInit, isAuthPath: boolean): Promise<Response> {
  try {
    const auth = isAuthPath ? {} : authHeaders();
    const client: Record<string, string> = IS_WEB ? { 'X-Client': 'web' } : {};
    const headers: Record<string, string> = {
      Accept: 'application/json',
      ...client,
      ...auth,
      ...(init.headers as Record<string, string> | undefined),
    };
    return await fetch(`${API_URL}${path}`, {
      ...init,
      ...(IS_WEB ? { credentials: 'include' as const } : {}),
      headers,
    });
  } catch (cause) {
    console.warn('[api] falha de rede em', `${API_URL}${path}`, cause);
    throw new ApiError('Não foi possível conectar ao servidor. Verifique a conexão.', 0);
  }
}

async function fetchWithRetry(path: string, init: RequestInit, isAuthPath: boolean): Promise<Response> {
  const delays = isSafeToRetry(init) ? RETRY_DELAYS_MS : [];
  for (let attempt = 0; ; attempt += 1) {
    const lastAttempt = attempt >= delays.length;
    try {
      const response = await fetchOnce(path, init, isAuthPath);
      if (lastAttempt || !TRANSIENT_STATUS.has(response.status)) return response;
    } catch (error) {
      if (lastAttempt) throw error;
    }
    await sleep(delays[attempt]);
  }
}

async function send(path: string, init: RequestInit = {}, retried = false): Promise<Response> {
  const isAuthPath = path.startsWith('/auth/');
  const response = await fetchWithRetry(path, init, isAuthPath);

  if (response.status === 401 && !isAuthPath && accessToken && onUnauthorized && !retried) {
    const renewed = await onUnauthorized();
    if (renewed) {
      return send(path, init, true);
    }
  }
  if (!response.ok) {
    throw await toApiError(response);
  }
  return response;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await send(path, init);
  return (await response.json()) as T;
}

const json = (body: unknown): RequestInit => ({
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
});

export interface Session {
  readonly accessToken: string;
  readonly tokenType: 'Bearer';
  readonly expiresIn: number;
  readonly refreshToken: string | null;
  readonly refreshExpiresIn: number;
  readonly name: string;
  readonly email: string;
}

/** O navegador grava no formato que suporta: o nome do arquivo precisa combinar com o tipo para o servidor reconhecer. */
function webAudioExtension(mimeType: string): string {
  if (mimeType.includes('ogg')) return 'ogg';
  return mimeType.includes('mp4') ? 'm4a' : 'webm';
}

/** Falhar aqui (e não no envio) deixa claro se o problema é ler o arquivo gravado ou falar com o servidor. */
async function readRecording(uri: string): Promise<Blob> {
  try {
    return await (await fetch(uri)).blob();
  } catch (cause) {
    console.warn('[api] não foi possível ler a gravação', uri, cause);
    throw new ApiError('Não consegui ler a gravação de áudio.', 0);
  }
}

export const api = {
  register: (input: { name: string; email: string; password: string }) =>
    request<Session>('/auth/register', json(input)),

  login: (input: { email: string; password: string }) => request<Session>('/auth/login', json(input)),

  refresh: (refreshToken?: string | null) =>
    request<Session>('/auth/refresh', json(refreshToken ? { refreshToken } : {})),

  logout: async (refreshToken?: string | null): Promise<void> => {
    try {
      await send('/auth/logout', json(refreshToken ? { refreshToken } : {}));
    } catch {
      // sem rede ou token já inválido
    }
  },

  getBalance: () => request<Balance>('/api/balance'),

  getTransactions: () => request<Transaction[]>('/transactions'),

  getNotifications: () => request<NotificationsResponse>('/api/notifications'),

  markNotificationsRead: async (): Promise<void> => {
    await send('/api/notifications/read', { method: 'POST' });
  },

  getExpenseReport: (period: string, category?: Category) => {
    const query = category ? `&category=${category}` : '';
    return request<ExpenseReport>(`/api/reports/expenses?period=${period}${query}`);
  },

  createTransaction: (input: NewTransaction) => request<Transaction>('/transactions', json({
    description: input.description?.trim() || undefined,
    category: input.category,
    type: input.type,
    amount: input.amount,
  })),

  getAccount: () => request<AccountInfo>('/api/account'),

  lookupAccount: (target: { number: string } | { email: string }) => {
    const query = 'number' in target
      ? `number=${encodeURIComponent(target.number)}`
      : `email=${encodeURIComponent(target.email)}`;
    return request<AccountInfo>(`/api/accounts/lookup?${query}`);
  },

  transfer: (
    input: { destination: { number: string } | { email: string }; amount: number; description?: string },
    idempotencyKey: string,
  ) =>
    request<TransferResult>('/api/transfers', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Idempotency-Key': idempotencyKey },
      body: JSON.stringify({
        ...('number' in input.destination
          ? { destinationAccountNumber: input.destination.number }
          : { destinationEmail: input.destination.email }),
        amount: input.amount,
        description: input.description,
      }),
    }),

  simulate: (amount: number, months: number) => request<Simulation>('/api/simulations', json({ amount, months })),

  sendText: (message: string, conversationId: string) =>
    request<AssistantReply>('/assistant/text', json({ message, conversationId })),

  /** Conta ao Claudinho como terminou o cartão de transferência. */
  sendTransferEvent: (
    input: { type: TransferEventType; recipientName: string; amount: number; reason?: string },
    conversationId: string,
  ) => request<AssistantReply>('/assistant/event', json({ ...input, conversationId })),

  sendVoice: async (uri: string, conversationId: string): Promise<VoiceReply> => {
    const form = new FormData();
    form.append('conversationId', conversationId);
    // A gravação é um arquivo local (nativo) ou um blob URL (web)
    const blob = await readRecording(uri);
    const extension = Platform.OS === 'web' ? webAudioExtension(blob.type) : 'm4a';
    form.append('file', blob, `comando.${extension}`);
    const response = await send('/assistant/voice', { method: 'POST', body: form });
    return (await response.json()) as VoiceReply;
  },
};
