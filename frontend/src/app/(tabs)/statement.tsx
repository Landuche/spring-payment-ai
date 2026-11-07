import { useFocusEffect } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import { RefreshControl, StyleSheet, Text, View } from 'react-native';
import { Card } from '@/components/Card';
import { ChipGroup } from '@/components/ChipGroup';
import { Screen } from '@/components/Screen';
import { TransactionItem } from '@/components/TransactionItem';
import { useFinance } from '@/hooks/useFinance';
import { formatBRL, formatDay } from '@/lib/format';
import { isCreditType, isTransferType, type Transaction } from '@/lib/types';
import { font, radius, space, type Palette } from '@/theme/tokens';
import { useStyles, useTheme } from '@/theme/theme-context';

type Filter = 'ALL' | 'INCOMING' | 'OUTGOING' | 'TRANSFERS';

const FILTERS: readonly { readonly value: Filter; readonly label: string }[] = [
  { value: 'ALL', label: 'Todos' },
  { value: 'INCOMING', label: 'Entradas' },
  { value: 'OUTGOING', label: 'Saídas' },
  { value: 'TRANSFERS', label: 'Transferências' },
];

const MATCHES: Record<Filter, (t: Transaction) => boolean> = {
  ALL: () => true,
  INCOMING: (t) => isCreditType(t.type),
  OUTGOING: (t) => !isCreditType(t.type),
  TRANSFERS: (t) => isTransferType(t.type),
};

interface DaySection {
  readonly day: string;
  readonly items: readonly Transaction[];
}

function groupByDay(items: readonly Transaction[]): readonly DaySection[] {
  const sorted = items
    .slice()
    .sort((a, b) => b.occurredAt.localeCompare(a.occurredAt) || (b.createdAt ?? '').localeCompare(a.createdAt ?? ''));
  const sections: { day: string; items: Transaction[] }[] = [];
  for (const transaction of sorted) {
    const last = sections[sections.length - 1];
    if (last && last.day === transaction.occurredAt) {
      last.items.push(transaction);
    } else {
      sections.push({ day: transaction.occurredAt, items: [transaction] });
    }
  }
  return sections;
}

export default function StatementScreen() {
  const { palette } = useTheme();
  const styles = useStyles(makeStyles);
  const { transactions, loading, refresh } = useFinance();
  const [filter, setFilter] = useState<Filter>('ALL');

  useFocusEffect(
    useCallback(() => {
      void refresh();
    }, [refresh]),
  );

  const filtered = useMemo(() => transactions.filter(MATCHES[filter]), [transactions, filter]);
  const sections = useMemo(() => groupByDay(filtered), [filtered]);
  const totals = useMemo(
    () =>
      filtered.reduce(
        (acc, t) => (isCreditType(t.type) ? { ...acc, incoming: acc.incoming + t.amount } : { ...acc, outgoing: acc.outgoing + t.amount }),
        { incoming: 0, outgoing: 0 },
      ),
    [filtered],
  );

  return (
    <Screen sidebar refreshControl={<RefreshControl refreshing={loading} onRefresh={refresh} tintColor={palette.text} />}>
      <View style={styles.header}>
        <Text style={styles.title}>Extrato</Text>
        <Text style={styles.subtitle}>Todas as suas movimentações, da mais recente para a mais antiga.</Text>
      </View>

      <ChipGroup value={filter} options={FILTERS} onChange={setFilter} />

      <View style={styles.totals}>
        <View style={styles.total}>
          <Text style={styles.totalLabel}>Entradas</Text>
          <Text style={[styles.totalValue, { color: palette.income }]}>{formatBRL(totals.incoming)}</Text>
        </View>
        <View style={styles.total}>
          <Text style={styles.totalLabel}>Saídas</Text>
          <Text style={[styles.totalValue, { color: palette.expense }]}>{formatBRL(totals.outgoing)}</Text>
        </View>
      </View>

      {sections.length === 0 ? (
        <Card>
          <Text style={styles.empty}>
            {loading ? 'Carregando...' : 'Nenhuma movimentação neste filtro.'}
          </Text>
        </Card>
      ) : (
        sections.map((section) => (
          <Card key={section.day} title={formatDay(section.day)}>
            {section.items.map((transaction, index) => (
              <View key={transaction.id}>
                {index > 0 ? <View style={styles.separator} /> : null}
                <TransactionItem transaction={transaction} />
              </View>
            ))}
          </Card>
        ))
      )}
    </Screen>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    header: { gap: 4 },
    title: { color: palette.text, fontSize: font.title, fontWeight: '800' },
    subtitle: { color: palette.textMuted, fontSize: font.body, lineHeight: 20 },
    totals: { flexDirection: 'row', gap: space.md },
    total: {
      flex: 1,
      padding: space.lg,
      borderRadius: radius.lg,
      backgroundColor: palette.surface,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: palette.border,
      gap: 4,
    },
    totalLabel: { color: palette.textMuted, fontSize: font.caption, textTransform: 'uppercase', letterSpacing: 0.6 },
    totalValue: { fontSize: font.subtitle, fontWeight: '700', fontVariant: ['tabular-nums'] },
    empty: { color: palette.textMuted, fontSize: font.body, lineHeight: 20 },
    separator: { height: StyleSheet.hairlineWidth, backgroundColor: palette.border },
  });
