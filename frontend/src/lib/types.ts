// Espelha os DTOs de saída do backend (Spring Boot). Valores monetários em reais.

export type TransactionType = 'RECEITA' | 'DESPESA' | 'TRANSFERENCIA_RECEBIDA' | 'TRANSFERENCIA_ENVIADA';

export type Category = 'GROCERIES' | 'PHARMA' | 'AUTO' | 'OUTROS' | 'TRANSFERENCIA';

/** Categorias que o usuário escolhe manualmente. */
export const CATEGORIES: readonly Category[] = ['GROCERIES', 'PHARMA', 'AUTO', 'OUTROS'] as const;

export const CATEGORY_LABEL: Record<Category, string> = {
  GROCERIES: 'Mercado',
  PHARMA: 'Farmácia',
  AUTO: 'Automóvel',
  OUTROS: 'Outros',
  TRANSFERENCIA: 'Transferência',
};

/** Lançamentos de transferência entram como entrada/saída, mas não como gasto do mês. */
export const isTransferType = (type: TransactionType): boolean =>
  type === 'TRANSFERENCIA_RECEBIDA' || type === 'TRANSFERENCIA_ENVIADA';
export const isCreditType = (type: TransactionType): boolean =>
  type === 'RECEITA' || type === 'TRANSFERENCIA_RECEBIDA';

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
  readonly receitas: number;
  readonly despesas: number;
  readonly saldo: number;
}

export interface ExpenseReport {
  readonly periodo: string;
  readonly categoria: Category | 'TODAS';
  readonly total: number;
  readonly quantidade: number;
  readonly transacoes: readonly Transaction[];
}

export interface Simulation {
  readonly valorInvestido: number;
  readonly montanteFinal: number;
  readonly rendimento: number;
  readonly prazoMeses: number;
  readonly taxaMensal: number;
}

/** Transferência que o Claudinho preparou. Só o toque do cliente no cartão do chat a executa. */
export interface TransferProposal {
  /** Formato XXXX-XXXXXX. */
  readonly contaDestino: string;
  /** Mascarado, como na busca de contas ("Bruno L."). */
  readonly nomeDestinatario: string;
  readonly valor: number;
  readonly descricao: string | null;
}

/** Desfecho do cartão de transferência, que o app conta ao Claudinho para ele responder. */
export type TransferEventType = 'TRANSFERENCIA_CONFIRMADA' | 'TRANSFERENCIA_CANCELADA' | 'TRANSFERENCIA_FALHOU';

export interface AssistantReply {
  readonly reply: string;
  readonly transferencia: TransferProposal | null;
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
  readonly numero: string;
  /** Nome completo para a própria conta; mascarado quando é a conta de outra pessoa. */
  readonly nomeTitular: string;
}

export interface TransferResult {
  readonly id: string;
  readonly amount: number;
  readonly description: string;
  readonly createdAt: string;
  /** true quando a mesma Idempotency-Key já tinha sido processada: nada foi debitado de novo. */
  readonly replayed: boolean;
}
