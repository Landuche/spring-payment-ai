import { API_URL } from './config';

const FIRST_BACKOFF_MS = 1_000;
const MAX_BACKOFF_MS = 30_000;
const UNAUTHORIZED_CLOSE = 4401;

interface Options {
  readonly getToken: (forceRenew: boolean) => Promise<string | null>;
  readonly onReady: () => void;
  readonly onChange: () => void;
}

const socketUrl = () => `${API_URL.replace(/^http/, 'ws')}/ws/notifications`;

function messageType(data: unknown): string | null {
  try {
    return (JSON.parse(String(data)) as { type?: string }).type ?? null;
  } catch {
    return null;
  }
}

/**
 * Mantém uma conexão WebSocket com o servidor para ser avisado de notificação nova, e reconecta sozinha (espera
 * crescente, até 30 s) quando cai: o token vence, a rede oscila ou o app volta do segundo plano.
 * Devolve a função que encerra a conexão.
 */
export function connectNotificationSocket({ getToken, onReady, onChange }: Options): () => void {
  let socket: WebSocket | null = null;
  let timer: ReturnType<typeof setTimeout> | null = null;
  let backoff = FIRST_BACKOFF_MS;
  let stopped = false;
  // O servidor recusou o token (por exemplo, a chave de assinatura mudou ao reiniciar): a próxima tentativa renova antes.
  let forceRenew = false;

  const scheduleReconnect = () => {
    if (stopped) return;
    timer = setTimeout(open, backoff);
    backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
  };

  async function open() {
    const token = await getToken(forceRenew).catch(() => null);
    forceRenew = false;
    if (stopped) return;
    if (!token) {
      scheduleReconnect();
      return;
    }
    const connection = new WebSocket(socketUrl());
    socket = connection;
    connection.onopen = () => connection.send(JSON.stringify({ type: 'auth', token }));
    connection.onmessage = (event) => {
      const type = messageType(event.data);
      if (type === 'ready') {
        backoff = FIRST_BACKOFF_MS;
        onReady();
      } else if (type === 'notifications-changed') {
        onChange();
      }
    };
    connection.onclose = (event) => {
      if (socket === connection) socket = null;
      forceRenew = event.code === UNAUTHORIZED_CLOSE;
      scheduleReconnect();
    };
  }

  void open();

  return () => {
    stopped = true;
    if (timer) clearTimeout(timer);
    socket?.close();
  };
}
