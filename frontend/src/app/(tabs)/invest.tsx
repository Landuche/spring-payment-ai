import { useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { Card } from '@/components/Card';
import { Screen } from '@/components/Screen';
import { api, ApiError } from '@/lib/api';
import { formatBRL, parseAmount } from '@/lib/format';
import type { Simulation } from '@/lib/types';
import { font, gradients, radius, space, type Palette } from '@/theme/tokens';
import { useStyles, useTheme } from '@/theme/theme-context';
import { LinearGradient } from 'expo-linear-gradient';

const TERMS = [6, 12, 24, 60] as const;

export default function InvestScreen() {
  const [amountText, setAmountText] = useState('1000');
  const [months, setMonths] = useState<number>(12);
  const [result, setResult] = useState<Simulation | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { palette } = useTheme();
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
        <View style={styles.inputWrap}>
          <Text style={styles.currency}>R$</Text>
          <TextInput
            value={amountText}
            onChangeText={setAmountText}
            keyboardType="decimal-pad"
            placeholder="0,00"
            placeholderTextColor={palette.textMuted}
            style={styles.input}
            accessibilityLabel="Valor a investir"
          />
        </View>

        <Text style={styles.sectionLabel}>Prazo</Text>
        <View style={styles.chips}>
          {TERMS.map((value) => {
            const selected = value === months;
            return (
              <Pressable
                key={value}
                onPress={() => setMonths(value)}
                accessibilityRole="button"
                accessibilityState={{ selected }}
                style={[styles.chip, selected && styles.chipSelected]}
              >
                <Text style={[styles.chipText, selected && styles.chipTextSelected]}>
                  {value < 12 ? `${value} meses` : value === 12 ? '1 ano' : `${value / 12} anos`}
                </Text>
              </Pressable>
            );
          })}
        </View>

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
  inputWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: space.sm,
    paddingHorizontal: space.lg,
    height: 60,
    borderRadius: radius.md,
    backgroundColor: palette.surfaceRaised,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: palette.border,
  },
  currency: { color: palette.textMuted, fontSize: font.subtitle, fontWeight: '600' },
  input: { flex: 1, color: palette.text, fontSize: 26, fontWeight: '700', fontVariant: ['tabular-nums'] },
  sectionLabel: { color: palette.textMuted, fontSize: font.caption, textTransform: 'uppercase', letterSpacing: 0.6 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: space.sm },
  chip: {
    paddingHorizontal: space.lg,
    paddingVertical: space.sm,
    borderRadius: radius.pill,
    backgroundColor: palette.surfaceRaised,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: palette.border,
  },
  chipSelected: { backgroundColor: palette.primaryStrong, borderColor: palette.primaryStrong },
  chipText: { color: palette.textMuted, fontWeight: '600', fontSize: font.body },
  chipTextSelected: { color: '#FFFFFF' },
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
