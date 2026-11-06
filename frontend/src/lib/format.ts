const BRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function formatBRL(value: number): string {
  return BRL.format(value);
}

/** yyyy-MM do mês corrente, no formato que o backend espera. */
export function currentPeriod(date: Date = new Date()): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  return `${date.getFullYear()}-${month}`;
}

/** Converte "1.234,56" ou "1234.5" em número. Retorna NaN se inválido. */
export function parseAmount(input: string): number {
  const normalized = input.trim().replace(/\./g, '').replace(',', '.');
  if (!/^\d+(\.\d{1,2})?$/.test(normalized)) {
    return Number.NaN;
  }
  return Number(normalized);
}

const DATE_FMT = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short' });

export function formatDay(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number);
  return DATE_FMT.format(new Date(year, month - 1, day));
}

const TIME_FMT = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' });

/** "4 de nov., 14:23" quando o lançamento tem hora; só o dia quando não tem (ou quando a hora é inválida). */
export function formatDayTime(isoDate: string, createdAt?: string | null): string {
  if (!createdAt) return formatDay(isoDate);
  const moment = new Date(createdAt);
  return Number.isNaN(moment.getTime()) ? formatDay(isoDate) : `${formatDay(isoDate)}, ${TIME_FMT.format(moment)}`;
}
