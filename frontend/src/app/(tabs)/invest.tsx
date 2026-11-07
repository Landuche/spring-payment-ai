import { useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';
import { Card } from '@/components/Card';
import { ChipGroup } from '@/components/ChipGroup';
import { AmountInput } from '@/components/form';
import { Screen } from '@/components/Screen';
import { api, ApiError } from '@/lib/api';
import { formatBRL, parseAmount } from '@/lib/format';
import type { Simulation } from '@/lib/types';
import { font, gradients, radius, space, type Palette } from '@/theme/tokens';
import { useStyles, useTheme } from '@/theme/theme-context';
import { LinearGradient } from 'expo-linear-gradient';

const TERM_LABEL = (months: number) =>
  months < 12 ? `${months} meses` : months === 12 ? '1 ano' : `${months / 12} anos`;

const TERM_OPTIONS = ([6, 12, 24, 60] as const).map((value) => ({ value: value as number, label: TERM_LABEL(value) }));

export default function InvestScreen() {
  const [amountText, setAmountText] = useState('1000');
  const [months, setMonths] = useState<number>(12);
  const [result, setResult] = useState<Simulation | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const styles = useStyles(makeStyles);

  const amount = parseAmount(amountText);
  const valid = Number.isFinite(amount) && amount > 0;

  const simulate = async () => {
    if (!valid) {
      setError('Informe um valor maior que zero.');
      return;
    }
    setError(null);
    setLoading(true);
    try {
      setResult(await api.simulate(amount, months));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Não foi possível simular agora.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Screen sidebar keyboardShouldPersistTaps="handled">
      <View style={styles.header}>
        <Text style={styles.title}>Simular investimento</Text>
        <Text style={styles.subtitle}>Veja quanto seu dinheiro pode render com juros compostos.</Text>
      </View>

      <Card title="Quanto investir?">
        <AmountInput value={amountText} onChangeText={setAmountText} accessibilityLabel="Valor a investir" />

        <Text style={styles.sectionLabel}>Prazo</Text>
        <ChipGroup value={months} options={TERM_OPTIONS} onChange={setMonths} />

        {error ? <Text style={styles.error}>{error}</Text> : null}

        <Pressable
          onPress={simulate}
          disabled={loading}
          accessibilityRole="button"
          style={({ pressed }) => [styles.cta, (pressed || loading) && { opacity: 0.85 }]}
        >
          <LinearGradient colors={gradients.brand} start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }} style={styles.ctaGradient}>
            {loading ? (
              <ActivityIndicator color="#FFFFFF" />
            ) : (
              <Text style={styles.ctaText}>Simular</Text>
            )}
          </LinearGradient>
        </Pressable>
      </Card>

      {result ? (
        <LinearGradient colors={gradients.calm} style={styles.resultCard}>
          <Text style={styles.resultLabel}>Montante em {result.termMonths} meses</Text>
          <Text style={styles.resultValue}>{formatBRL(result.finalAmount)}</Text>
          <View style={styles.resultRow}>
            <ResultStat label="Investido" value={formatBRL(result.investedAmount)} />
            <ResultStat label="Rendimento" value={formatBRL(result.earnings)} highlight />
            <ResultStat label="Taxa/mês" value={`${(result.monthlyRate * 100).toFixed(2)}%`} />
          </View>
          <Text style={styles.disclaimer}>Simulação ilustrativa, sem considerar impostos ou taxas.</Text>
        </LinearGradient>
      ) : null}
    </Screen>
  );
}

function ResultStat({ label, value, highlight }: { readonly label: string; readonly value: string; readonly highlight?: boolean }) {
  const { palette } = useTheme();
  const styles = useStyles(makeStyles);
  return (
    <View style={styles.resultStat}>
      <Text style={styles.resultStatLabel}>{label}</Text>
      <Text style={[styles.resultStatValue, highlight && { color: palette.income }]}>{value}</Text>
    </View>
  );
}

const makeStyles = (palette: Palette) => StyleSheet.create({
  header: { gap: 4 },
  title: { color: palette.text, fontSize: font.title, fontWeight: '800' },
  subtitle: { color: palette.textMuted, fontSize: font.body, lineHeight: 20 },
  sectionLabel: { color: palette.textMuted, fontSize: font.caption, textTransform: 'uppercase', letterSpacing: 0.6 },
  error: { color: palette.expense, fontSize: font.body },
  cta: { borderRadius: radius.md, overflow: 'hidden', marginTop: space.sm },
  ctaGradient: { height: 52, alignItems: 'center', justifyContent: 'center' },
  ctaText: { color: '#FFFFFF', fontSize: font.subtitle, fontWeight: '700' },
  resultCard: { borderRadius: radius.lg, padding: space.xl, gap: space.sm },
  resultLabel: { color: 'rgba(255,255,255,0.75)', fontSize: font.body },
  resultValue: { color: '#FFFFFF', fontSize: font.display, fontWeight: '800', letterSpacing: -0.5, fontVariant: ['tabular-nums'] },
  resultRow: { flexDirection: 'row', marginTop: space.lg, gap: space.md },
  resultStat: { flex: 1, gap: 2 },
  resultStatLabel: { color: 'rgba(255,255,255,0.65)', fontSize: font.caption, textTransform: 'uppercase' },
  resultStatValue: { color: '#FFFFFF', fontSize: font.body, fontWeight: '700', fontVariant: ['tabular-nums'] },
  disclaimer: { color: 'rgba(255,255,255,0.55)', fontSize: font.caption, marginTop: space.sm },
});
