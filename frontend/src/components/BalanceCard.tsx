import { LinearGradient } from 'expo-linear-gradient';
import { StyleSheet, Text, View } from 'react-native';
import { formatBRL } from '@/lib/format';
import type { Balance } from '@/lib/types';
import { font, gradients, radius, space } from '@/theme/tokens';

interface BalanceCardProps {
  readonly balance: Balance;
}

export function BalanceCard({ balance }: BalanceCardProps) {
  return (
    <LinearGradient colors={gradients.brand} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} style={styles.card}>
      <View style={styles.glow} />
      <Text style={styles.label}>Saldo disponível</Text>
      <Text style={styles.amount} accessibilityLabel={`Saldo ${formatBRL(balance.balance)}`}>
        {formatBRL(balance.balance)}
      </Text>

      <View style={styles.row}>
        <Stat label="Entradas" value={formatBRL(balance.income)} />
        <View style={styles.divider} />
        <Stat label="Saídas" value={formatBRL(balance.expenses)} />
      </View>
    </LinearGradient>
  );
}

function Stat({ label, value }: { readonly label: string; readonly value: string }) {
  return (
    <View style={styles.stat}>
      <Text style={styles.statLabel}>{label}</Text>
      <Text style={styles.statValue}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    borderRadius: radius.lg,
    padding: space.xl,
    gap: space.sm,
    overflow: 'hidden',
  },
  glow: {
    position: 'absolute',
    top: -60,
    right: -60,
    width: 180,
    height: 180,
    borderRadius: 90,
    backgroundColor: 'rgba(255,255,255,0.12)',
  },
  label: { color: 'rgba(255,255,255,0.8)', fontSize: font.body, fontWeight: '500' },
  amount: { color: '#FFFFFF', fontSize: font.display, fontWeight: '800', letterSpacing: -0.5 },
  row: {
    flexDirection: 'row',
    marginTop: space.lg,
    alignItems: 'center',
  },
  stat: { flex: 1, gap: 2 },
  statLabel: { color: 'rgba(255,255,255,0.7)', fontSize: font.caption, textTransform: 'uppercase' },
  statValue: { color: '#FFFFFF', fontSize: font.subtitle, fontWeight: '700' },
  divider: { width: 1, height: 32, backgroundColor: 'rgba(255,255,255,0.25)', marginHorizontal: space.lg },
});

