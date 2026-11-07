import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, Text, View } from 'react-native';
import { CATEGORY_LABEL, isCreditType, type Category, type Transaction } from '@/lib/types';
import { formatBRL, formatDayTime } from '@/lib/format';
import { font, radius, space, type Palette } from '@/theme/tokens';
import { useStyles, useTheme } from '@/theme/theme-context';

const CATEGORY_ICON: Record<Category, React.ComponentProps<typeof Ionicons>['name']> = {
  GROCERIES: 'cart-outline',
  PHARMA: 'medkit-outline',
  AUTO: 'car-sport-outline',
  OTHER: 'ellipsis-horizontal-circle-outline',
  TRANSFER: 'swap-horizontal-outline',
};

export function TransactionItem({ transaction }: { readonly transaction: Transaction }) {
  const { palette } = useTheme();
  const styles = useStyles(makeStyles);
  const isIncome = isCreditType(transaction.type);
  const signed = `${isIncome ? '+' : '-'} ${formatBRL(transaction.amount)}`;

  return (
    <View style={styles.row} accessibilityRole="text">
      <View style={[styles.iconWrap, { backgroundColor: isIncome ? 'rgba(52,211,153,0.14)' : 'rgba(139,92,246,0.16)' }]}>
        <Ionicons
          name={CATEGORY_ICON[transaction.category]}
          size={20}
          color={isIncome ? palette.income : palette.primary}
        />
      </View>

      <View style={styles.middle}>
        <Text style={styles.title} numberOfLines={1}>
          {transaction.description}
        </Text>
        <Text style={styles.meta}>
          {CATEGORY_LABEL[transaction.category]} · {formatDayTime(transaction.occurredAt, transaction.createdAt)}
        </Text>
      </View>

      <Text style={[styles.amount, { color: isIncome ? palette.income : palette.text }]}>{signed}</Text>
    </View>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    row: { flexDirection: 'row', alignItems: 'center', gap: space.md, paddingVertical: space.md },
    iconWrap: { width: 42, height: 42, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center' },
    middle: { flex: 1, gap: 2 },
    title: { color: palette.text, fontSize: font.body, fontWeight: '600' },
    meta: { color: palette.textMuted, fontSize: font.caption },
    amount: { fontSize: font.body, fontWeight: '700', fontVariant: ['tabular-nums'] },
  });
