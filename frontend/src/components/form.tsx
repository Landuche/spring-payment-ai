import { StyleSheet, Text, TextInput, View } from 'react-native';
import { font, radius, space, type Palette } from '@/theme/tokens';
import { useStyles, useTheme } from '@/theme/theme-context';

/** Estilos comuns aos formulários (transferir, depositar, registrar gasto). */
export const makeFormStyles = (palette: Palette) =>
  StyleSheet.create({
    label: {
      color: palette.textMuted,
      fontSize: font.caption,
      textTransform: 'uppercase',
      letterSpacing: 0.6,
      marginTop: space.sm,
    },
    input: {
      height: 50,
      paddingHorizontal: space.lg,
      borderRadius: radius.md,
      backgroundColor: palette.surfaceRaised,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: palette.border,
      color: palette.text,
      fontSize: font.body,
    },
    error: { color: palette.expense, fontSize: font.body },
    submit: {
      height: 52,
      borderRadius: radius.md,
      backgroundColor: palette.primaryStrong,
      alignItems: 'center',
      justifyContent: 'center',
      marginTop: space.md,
    },
    submitDisabled: { opacity: 0.4 },
    submitText: { color: '#FFFFFF', fontSize: font.subtitle, fontWeight: '700' },
  });

const makeAmountStyles = (palette: Palette) =>
  StyleSheet.create({
    wrap: {
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
  });

interface AmountInputProps {
  readonly value: string;
  readonly onChangeText: (value: string) => void;
  readonly accessibilityLabel: string;
  readonly editable?: boolean;
}

/** Campo grande de valor em reais. */
export function AmountInput({ value, onChangeText, accessibilityLabel, editable = true }: AmountInputProps) {
  const { palette } = useTheme();
  const styles = useStyles(makeAmountStyles);

  return (
    <View style={styles.wrap}>
      <Text style={styles.currency}>R$</Text>
      <TextInput
        value={value}
        onChangeText={onChangeText}
        keyboardType="decimal-pad"
        placeholder="0,00"
        placeholderTextColor={palette.textMuted}
        style={styles.input}
        accessibilityLabel={accessibilityLabel}
        editable={editable}
      />
    </View>
  );
}
