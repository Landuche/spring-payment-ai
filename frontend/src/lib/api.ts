import { Platform } from 'react-native';
import { API_URL } from './config';
import type {
  AccountInfo,
  AssistantReply,
  Balance,
  Category,
  ExpenseReport,
  NewTransaction,
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
    /** Segundos até poder tentar de novo (presente em 429). */
    readonly retryAfterSeconds?: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/** Mensagem amigável para o tempo de espera de um bloqueio (ex.: "cerca de 1 hora"). */
export function describeWait(seconds: number): string {
  if (seconds >= 3600) {
    const hours = Math.ceil(seconds / 3600);
    return hours === 1 ? 'cerca de 1 hora' : `cerca de ${hours} horas`;
  }
  const minutes = Math.max(1, Math.ceil(seconds / 60));
  return `${minutes} minuto${minutes === 1 ? '' : 's'}`;
}

/** Token em memória. A persistência fica em session-storage; aqui só o que as requisições usam. */
let accessToken: string | null = null;
let onUnauthorized: (() => Promise<boolean>) | null = null;

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

/**
 * Chamado quando o servidor responde 401 a uma requisição autenticada. Deve tentar renovar a sessão e
 * devolver true se conseguiu (a requisição é repetida uma vez); false encerra a sessão local.
 */
export function setUnauthorizedHandler(handler: (() => Promise<boolean>) | null): void {
  onUnauthorized = handler;
}

function authHeaders(): Record<string, string> {
  return accessToken ? { Authorization: `Bearer ${accessToken}` } : {};
}

/** Lê o ProblemDetail do Spring (campo `detail`) quando disponível. */
async function toApiError(response: Response): Promise<ApiError> {
  let message = `Erro ${response.status}`;
  try {
    const body = (await response.json()) as { detail?: string; title?: string };
    message = body.detail ?? body.title ?? message;
  } catch {
    // corpo sem JSON: mantém a mensagem genérica
  }
  const retryAfter = Number(response.headers.get('Retry-After'));
  return new ApiError(message, response.status, Number.isFinite(retryAfter) && retryAfter > 0 ? retryAfter : undefined);
}

/** No web, o refresh token fica num cookie HttpOnly: a requisição precisa levar credenciais e se identificar. */
const IS_WEB = Platform.OS === 'web';

/** Esperas (ms) antes de cada repetição automática. */
const RETRY_DELAYS_MS = [400, 1200] as const;

/** Respostas que indicam falha momentânea do servidor ou de algo no caminho (proxy, túnel). */
const TRANSIENT_STATUS = new Set([502, 503, 504]);

/**
 * Só repete o que é seguro repetir: leituras (GET) e escritas com chave de idempotência, em que o servidor devolve
 * o mesmo resultado em vez de executar de novo. Registrar transação e falar com o Claudinho NÃO entram: se a
 * primeira tentativa chegou ao servidor, repetir duplicaria o lançamento.
 */
function isSafeToRetry(init: RequestInit): boolean {
  const method = (init.method ?? 'GET').toUpperCase();
  const headers = init.headers as Record<string, string> | undefined;
  return method === 'GET' || headers?.['Idempotency-Key'] !== undefined;
}

const sleep = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms));

async function fetchOnce(path: string, init: RequestInit, isAuthPath: boolean): Promise<Response> {
  try {
    // Rotas /auth não levam Bearer: um access token expirado faria o Spring responder 401 antes do controller.
    const auth = isAuthPath ? {} : authHeaders();
    const client: Record<string, string> = IS_WEB ? { 'X-Client': 'web' } : {};
    // Os chamadores deste módulo sempre passam cabeçalhos como objeto simples.
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
    // Diagnóstico: registra o motivo real da falha de rede (visível no log do Metro).
    console.warn('[api] falha de rede em', `${API_URL}${path}`, cause);
    throw new ApiError('Não foi possível conectar ao servidor. Verifique a conexão.', 0);
  }
}

/** Repete falhas de rede e respostas 502/503/504 com espera crescente, mas só quando é seguro (ver `isSafeToRetry`). */
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

  // 401 com sessão ativa: tenta renovar uma única vez e repete a requisição com o token novo.
  // Rotas /auth nunca disparam renovação: /auth/refresh com 401 chamaria a própria renovação (deadlock).
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
  /** Validade do access token, em segundos (15 min). */
  readonly expiresIn: number;
  /** Nativo: token no corpo. Web: null, pois o token fica no cookie HttpOnly do servidor. */
  readonly refreshToken: string | null;
  /** Validade do refresh token, em segundos (30 dias). */
  readonly refreshExpiresIn: number;
  readonly name: string;
  readonly email: string;
}

export const api = {
  register: (input: { name: string; email: string; password: string }) =>
    request<Session>('/auth/register', json(input)),

  login: (input: { email: string; password: string }) => request<Session>('/auth/login', json(input)),

  /**
   * Rotação: o refresh token usado deixa de valer; a resposta traz o novo par.
   * Sem argumento (web), o servidor lê o cookie. Com argumento (nativo), usa o token do corpo.
   */
  refresh: (refreshToken?: string | null) =>
    request<Session>('/auth/refresh', json(refreshToken ? { refreshToken } : {})),

  /** Revoga a sessão no servidor. Falhas são ignoradas: o logout local acontece de qualquer forma. */
  logout: async (refreshToken?: string | null): Promise<void> => {
    try {
      await send('/auth/logout', json(refreshToken ? { refreshToken } : {}));
    } catch {
      // sem rede ou token já inválido: nada a fazer além de limpar a sessão local
    }
  },

  getBalance: () => request<Balance>('/api/balance'),

  getTransactions: () => request<Transaction[]>('/transactions'),

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

  /** Busca o destinatário por número OU e-mail (apenas um). */
  lookupAccount: (target: { number: string } | { email: string }) => {
    const query = 'number' in target
      ? `number=${encodeURIComponent(target.number)}`
      : `email=${encodeURIComponent(target.email)}`;
    return request<AccountInfo>(`/api/accounts/lookup?${query}`);
  },

  /** A chave de idempotência deve ser gerada uma vez por intenção e reenviada em qualquer retry. */
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

  /** Conta ao Claudinho como terminou o cartão de transferência (enviada, cancelada ou com falha). */
  sendTransferEvent: (
    input: { type: TransferEventType; recipientName: string; amount: number; reason?: string },
    conversationId: string,
  ) => request<AssistantReply>('/assistant/event', json({ ...input, conversationId })),

  sendVoice: async (uri: string, conversationId: string): Promise<VoiceReply> => {
    const form = new FormData();
    form.append('conversationId', conversationId);
    if (Platform.OS === 'web') {
      // No web, a gravação é um blob URL: envia o blob real com nome e tipo.
      const blob = await (await fetch(uri)).blob();
      const extension = blob.type.includes('ogg') ? 'ogg' : blob.type.includes('mp4') ? 'm4a' : 'webm';
      form.append('file', blob, `comando.${extension}`);
    } else {
      // No nativo, o objeto { uri, name, type } não é mais aceito pelo FormData ("Unsupported FormDataPart").
      // Lê o arquivo gravado como Blob e envia com nome e tipo explícitos, como no web.
      const blob = await (await fetch(uri)).blob();
      form.append('file', blob, 'comando.m4a');
    }
    const response = await send('/assistant/voice', { method: 'POST', body: form });
    return (await response.json()) as VoiceReply;
  },
};
