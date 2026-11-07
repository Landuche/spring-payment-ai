import { API_URL } from './config';

const FIRST_BACKOFF_MS = 1_000;
const MAX_BACKOFF_MS = 30_000;

interface Options {
  /** Token de acesso vigente (já renovado, se preciso); null se não há sessão. */
  readonly getToken: () => Promise<string | null>;
  /** O servidor aceitou a conexão. Vale reler a lista: pode ter chegado algo enquanto estava desconectado. */
  readonly onReady: () => void;
  /** Chegou notificação nova. */
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

  const scheduleReconnect = () => {
    if (stopped) return;
    timer = setTimeout(open, backoff);
    backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
  };

  async function open() {
    const token = await getToken().catch(() => null);
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
    connection.onclose = () => {
      if (socket === connection) socket = null;
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
