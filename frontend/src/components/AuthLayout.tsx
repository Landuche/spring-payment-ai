import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useState, type ReactNode } from 'react';
import { KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { CONTENT_MAX_WIDTH, font, gradients, radius, space, type Palette } from '@/theme/tokens';
import { useStyles, useTheme } from '@/theme/theme-context';

/** Moldura das telas de autenticação: marca no topo, formulário centralizado. */
export function AuthLayout({ title, subtitle, children }: {
  readonly title: string;
  readonly subtitle: string;
  readonly children: ReactNode;
}) {
  const styles = useStyles(makeStyles);

  return (
    <SafeAreaView style={styles.safe} edges={['top', 'bottom']}>
      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
          <View style={styles.column}>
            <LinearGradient colors={gradients.brand} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} style={styles.logo}>
              <Ionicons name="sparkles" size={26} color="#FFFFFF" />
            </LinearGradient>
            <Text style={styles.title}>{title}</Text>
            <Text style={styles.subtitle}>{subtitle}</Text>
            <View style={styles.card}>{children}</View>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

interface FieldProps extends Omit<React.ComponentProps<typeof TextInput>, 'style'> {
  readonly label: string;
  readonly error?: string;
  readonly secure?: boolean;
}

/** Campo com rótulo, erro visível e alternância de exibição da senha quando `secure`. */
export function AuthField({ label, error, secure, ...input }: FieldProps) {
  const { palette } = useTheme();
  const styles = useStyles(makeStyles);
  const [hidden, setHidden] = useState(!!secure);

  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <View style={[styles.inputWrap, !!error && styles.inputError]}>
        <TextInput
          {...input}
          secureTextEntry={hidden}
          placeholderTextColor={palette.textMuted}
          style={styles.input}
          accessibilityLabel={label}
          accessibilityState={{ disabled: input.editable === false }}
        />
        {secure ? (
          <Pressable
            onPress={() => setHidden((v) => !v)}
            accessibilityRole="button"
            accessibilityLabel={hidden ? 'Mostrar senha' : 'Ocultar senha'}
            hitSlop={8}
          >
            <Ionicons name={hidden ? 'eye-outline' : 'eye-off-outline'} size={20} color={palette.textMuted} />
          </Pressable>
        ) : null}
      </View>
      {error ? <Text style={styles.error} accessibilityLiveRegion="polite">{error}</Text> : null}
    </View>
  );
}

export function PrimaryButton({ label, onPress, loading, disabled }: {
  readonly label: string;
  readonly onPress: () => void;
  readonly loading?: boolean;
  readonly disabled?: boolean;
}) {
  const styles = useStyles(makeStyles);

  return (
    <Pressable
      onPress={onPress}
      disabled={disabled || loading}
      accessibilityRole="button"
      accessibilityState={{ disabled: !!disabled || !!loading, busy: !!loading }}
      style={({ pressed }) => [styles.button, (disabled || loading) && styles.buttonDisabled, pressed && { opacity: 0.85 }]}
    >
      <LinearGradient colors={gradients.brand} start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }} style={styles.buttonGradient}>
        <Text style={styles.buttonText}>{loading ? 'Aguarde...' : label}</Text>
      </LinearGradient>
    </Pressable>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    safe: { flex: 1, backgroundColor: palette.background },
    flex: { flex: 1 },
    scroll: { flexGrow: 1, alignItems: 'center', justifyContent: 'center', padding: space.lg },
    column: { width: '100%', maxWidth: 420, gap: space.md },
    logo: { width: 56, height: 56, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center', alignSelf: 'center' },
    title: { color: palette.text, fontSize: font.title, fontWeight: '800', textAlign: 'center', marginTop: space.md },
    subtitle: { color: palette.textMuted, fontSize: font.body, textAlign: 'center', lineHeight: 20 },
    card: {
      backgroundColor: palette.surface,
      borderRadius: radius.lg,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: palette.border,
      padding: space.xl,
      gap: space.lg,
      marginTop: space.md,
      maxWidth: CONTENT_MAX_WIDTH.compact,
    },
    field: { gap: 6 },
    label: { color: palette.textMuted, fontSize: font.caption, textTransform: 'uppercase', letterSpacing: 0.6 },
    inputWrap: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: space.sm,
      height: 50,
      paddingHorizontal: space.lg,
      borderRadius: radius.md,
      backgroundColor: palette.surfaceRaised,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: palette.border,
    },
    inputError: { borderColor: palette.expense },
    input: { flex: 1, color: palette.text, fontSize: font.body, height: '100%' },
    error: { color: palette.expense, fontSize: font.caption },
    button: { borderRadius: radius.md, overflow: 'hidden', marginTop: space.sm },
    buttonDisabled: { opacity: 0.5 },
    buttonGradient: { height: 52, alignItems: 'center', justifyContent: 'center' },
    buttonText: { color: '#FFFFFF', fontSize: font.subtitle, fontWeight: '700' },
  });
