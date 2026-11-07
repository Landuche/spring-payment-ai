import { useCallback, useEffect, useState } from 'react';
import { api, ApiError } from '@/lib/api';
import { currentPeriod } from '@/lib/format';
import { useNotifications } from '@/lib/notifications-context';
import type { Balance, ExpenseReport, Transaction } from '@/lib/types';

interface FinanceState {
  readonly balance: Balance | null;
  readonly transactions: readonly Transaction[];
  readonly report: ExpenseReport | null;
  readonly loading: boolean;
  readonly error: string | null;
}

const INITIAL: FinanceState = {
  balance: null,
  transactions: [],
  report: null,
  loading: true,
  error: null,
};

type Result =
  | {
      readonly ok: true;
      readonly balance: Balance;
      readonly transactions: readonly Transaction[];
      readonly report: ExpenseReport;
    }
  | { readonly ok: false; readonly message: string };

async function fetchData(): Promise<Result> {
  try {
    const [balance, transactions, report] = await Promise.all([
      api.getBalance(),
      api.getTransactions(),
      api.getExpenseReport(currentPeriod()),
    ]);
    return { ok: true, balance, transactions, report };
  } catch (error) {
    const message = error instanceof ApiError ? error.message : 'Não foi possível carregar seus dados.';
    return { ok: false, message };
  }
}

function apply(prev: FinanceState, result: Result): FinanceState {
  return result.ok
    ? {
        balance: result.balance,
        transactions: result.transactions,
        report: result.report,
        loading: false,
        error: null,
      }
    : { ...prev, loading: false, error: result.message };
}

export function useFinance() {
  const [state, setState] = useState<FinanceState>(INITIAL);
  const { changes } = useNotifications();

  // Carrega ao abrir e de novo, sem mostrar "carregando", quando chega um aviso em tempo real: transferência
  // recebida ou enviada muda o saldo, o extrato e o relatório junto com a notificação.
  useEffect(() => {
    let active = true;
    void fetchData().then((result) => {
      if (active) setState((prev) => apply(prev, result));
    });
    return () => {
      active = false;
    };
  }, [changes]);

  const refresh = useCallback(() => {
    setState((prev) => ({ ...prev, loading: true, error: null }));
    return fetchData().then((result) => setState((prev) => apply(prev, result)));
  }, []);

  return { ...state, refresh };
}
