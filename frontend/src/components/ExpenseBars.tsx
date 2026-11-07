import { StyleSheet, Text, View } from 'react-native';
import { formatBRL } from '@/lib/format';
import { CATEGORIES, CATEGORY_LABEL, type Category, type Transaction } from '@/lib/types';
import { font, radius, space, type Palette } from '@/theme/tokens';
import { useStyles, useTheme } from '@/theme/theme-context';

/** Gastos do mês agrupados por categoria, em barras proporcionais ao maior valor. */
export function ExpenseBars({ expenses }: { readonly expenses: readonly Transaction[] }) {
  const { palette } = useTheme();
  const styles = useStyles(makeStyles);

  const barColor: Record<Category, string> = {
    GROCERIES: palette.primary,
    PHARMA: palette.accent,
    AUTO: palette.warning,
    OTHER: palette.primaryStrong,
    TRANSFER: palette.textMuted,
  };

  const totals = CATEGORIES.map((category) => ({
    category,
    total: expenses
      .filter((t) => t.type === 'EXPENSE' && t.category === category)
      .reduce((sum, t) => sum + t.amount, 0),
  }));
  const max = Math.max(...totals.map((t) => t.total), 0);

  if (max === 0) {
    return <Text style={styles.empty}>Nenhuma despesa neste mês ainda.</Text>;
  }

  return (
    <View style={styles.container}>
      {totals.map(({ category, total }) => (
        <View key={category} style={styles.row}>
          <View style={styles.labels}>
            <Text style={styles.label}>{CATEGORY_LABEL[category]}</Text>
            <Text style={styles.value}>{formatBRL(total)}</Text>
          </View>
          <View style={styles.track}>
            <View
              style={[
                styles.fill,
                { width: `${total === 0 ? 0 : Math.max((total / max) * 100, 4)}%`, backgroundColor: barColor[category] },
              ]}
            />
          </View>
        </View>
      ))}
    </View>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    container: { gap: space.lg },
    row: { gap: space.sm },
    labels: { flexDirection: 'row', justifyContent: 'space-between' },
    label: { color: palette.text, fontSize: font.body, fontWeight: '600' },
    value: { color: palette.textMuted, fontSize: font.body, fontVariant: ['tabular-nums'] },
    track: { height: 10, borderRadius: radius.pill, backgroundColor: palette.surfaceRaised, overflow: 'hidden' },
    fill: { height: '100%', borderRadius: radius.pill },
    empty: { color: palette.textMuted, fontSize: font.body },
  });
