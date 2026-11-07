// Espelha os DTOs de saída do backend (Spring Boot). Valores monetários em reais.

export type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER_RECEIVED' | 'TRANSFER_SENT';

export type Category = 'GROCERIES' | 'PHARMA' | 'AUTO' | 'OTHER' | 'TRANSFER';

/** Categorias que o usuário escolhe manualmente. */
export const CATEGORIES: readonly Category[] = ['GROCERIES', 'PHARMA', 'AUTO', 'OTHER'] as const;

export const CATEGORY_LABEL: Record<Category, string> = {
  GROCERIES: 'Mercado',
  PHARMA: 'Farmácia',
  AUTO: 'Automóvel',
  OTHER: 'Outros',
  TRANSFER: 'Transferência',
};

/** Lançamentos de transferência entram como entrada/saída, mas não como gasto do mês. */
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
  /** Momento exato (ISO 8601). Ausente quando só a data é conhecida. */
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

/** Transferência que o Claudinho preparou. Só o toque do cliente no cartão do chat a executa. */
export interface TransferProposal {
  /** Formato XXXX-XXXXXX. */
  readonly destinationAccount: string;
  /** Mascarado, como na busca de contas ("Bruno L."). */
  readonly recipientName: string;
  readonly amount: number;
  readonly description: string | null;
}

/** Desfecho do cartão de transferência, que o app conta ao Claudinho para ele responder. */
export type TransferEventType = 'TRANSFER_CONFIRMED' | 'TRANSFER_CANCELED' | 'TRANSFER_FAILED';

export interface AssistantReply {
  readonly reply: string;
  readonly transfer: TransferProposal | null;
}

export interface VoiceReply extends AssistantReply {
  readonly transcription: string;
}

export interface NewTransaction {
  /** Opcional: sem texto, o extrato usa o nome da categoria. */
  readonly description?: string;
  readonly category: Category;
  readonly type: TransactionType;
  readonly amount: number;
}

export interface AccountInfo {
  /** Formato XXXX-XXXXXX, número que outras pessoas usam para transferir. */
  readonly number: string;
  /** Nome completo para a própria conta; mascarado quando é a conta de outra pessoa. */
  readonly holderName: string;
}

export interface TransferResult {
  readonly id: string;
  readonly amount: number;
  readonly description: string;
  readonly createdAt: string;
  /** true quando a mesma Idempotency-Key já tinha sido processada: nada foi debitado de novo. */
  readonly replayed: boolean;
}
