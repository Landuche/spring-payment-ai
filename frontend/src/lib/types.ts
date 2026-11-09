export type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER_RECEIVED' | 'TRANSFER_SENT';

export type Category = 'GROCERIES' | 'PHARMA' | 'AUTO' | 'OTHER' | 'TRANSFER';

export const CATEGORIES: readonly Category[] = ['GROCERIES', 'PHARMA', 'AUTO', 'OTHER'] as const;

export const CATEGORY_LABEL: Record<Category, string> = {
  GROCERIES: 'Mercado',
  PHARMA: 'Farmácia',
  AUTO: 'Automóvel',
  OTHER: 'Outros',
  TRANSFER: 'Transferência',
};

export const isTransferType = (type: TransactionType): boolean =>
  type === 'TRANSFER_RECEIVED' || type === 'TRANSFER_SENT';
export const isCreditType = (type: TransactionType): boolean =>
  type === 'INCOME' || type === 'TRANSFER_RECEIVED';

export interface Transaction {
  readonly id: string;
  readonly description: string;
  readonly category: Category;
  readonly type: TransactionType;
  readonly amount: number;
  readonly occurredAt: string;
  readonly createdAt?: string | null;
}

export interface Balance {
  readonly income: number;
  readonly expenses: number;
  readonly balance: number;
}

export interface ExpenseReport {
  readonly period: string;
  readonly category: Category | 'ALL';
  readonly total: number;
  readonly count: number;
  readonly transactions: readonly Transaction[];
}

export interface Simulation {
  readonly investedAmount: number;
  readonly finalAmount: number;
  readonly earnings: number;
  readonly termMonths: number;
  readonly monthlyRate: number;
}

export interface TransferProposal {
  readonly destinationAccount: string
  readonly recipientName: string;
  readonly amount: number;
  readonly description: string | null;
}

export type TransferEventType = 'TRANSFER_CONFIRMED' | 'TRANSFER_CANCELED' | 'TRANSFER_FAILED';

export interface AssistantReply {
  readonly reply: string;
  readonly transfer: TransferProposal | null;
}

export interface VoiceReply extends AssistantReply {
  readonly transcription: string;
}

export interface NewTransaction {
  readonly description?: string;
  readonly category: Category;
  readonly type: TransactionType;
  readonly amount: number;
}

export interface AccountInfo {
  readonly number: string;
  readonly holderName: string;
}

export interface TransferResult {
  readonly id: string;
  readonly amount: number;
  readonly description: string;
  readonly createdAt: string;
  readonly replayed: boolean;
}

export type NotificationType = 'TRANSFER_SENT' | 'TRANSFER_RECEIVED';

/** Aviso estruturado: o texto da frase é montado na tela. */
export interface AppNotification {
  readonly id: string;
  readonly type: NotificationType;
  readonly amount: number;
  /** Nome mascarado da outra parte ("Bruno L."). */
  readonly counterpartName: string;
  readonly createdAt: string;
  readonly read: boolean;
}

export interface NotificationsResponse {
  readonly items: readonly AppNotification[];
  /** Todos os não lidos da conta, mesmo que a lista traga só os mais recentes. */
  readonly unread: number;
}

/** O que o servidor oferece. `aiSpeech`: gera a voz do Claudinho. Sem ela, o app lê com a voz do aparelho. */
export interface Capabilities {
  readonly aiSpeech: boolean;
}
