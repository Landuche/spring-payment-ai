import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { AuthField, AuthLayout, PrimaryButton } from '@/components/AuthLayout';
import { ApiError, describeWait } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import { validateEmail, validatePassword } from '@/lib/validation';
import { font, space, type Palette } from '@/theme/tokens';
import { useStyles } from '@/theme/theme-context';

export default function LoginScreen() {
  const router = useRouter();
  const { signIn } = useAuth();
  const styles = useStyles(makeStyles);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [touched, setTouched] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const emailError = touched ? validateEmail(email) : undefined;
  const passwordError = touched ? validatePassword(password) : undefined;

  const submit = async () => {
    setTouched(true);
    setServerError(null);
    if (validateEmail(email) || validatePassword(password)) return;

    setSubmitting(true);
    try {
      await signIn(email.trim(), password);
      // O AuthGate da raiz redireciona para a área logada.
    } catch (error) {
      if (error instanceof ApiError && error.status === 429) {
        const wait = describeWait(error.retryAfterSeconds ?? 900);
        setServerError(`Muitas tentativas de acesso. Aguarde ${wait} e tente novamente.`);
      } else if (error instanceof ApiError && error.status === 401) {
        setServerError('E-mail ou senha incorretos.');
      } else {
        setServerError(error instanceof ApiError ? error.message : 'Não foi possível entrar. Tente novamente.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout title="Bem-vindo de volta" subtitle="Entre para acompanhar seu saldo e falar com seu assistente.">
      <AuthField
        label="E-mail"
        value={email}
        onChangeText={setEmail}
        error={emailError}
        keyboardType="email-address"
        autoCapitalize="none"
        autoComplete="email"
        textContentType="emailAddress"
        autoCorrect={false}
        placeholder="voce@exemplo.com"
        editable={!submitting}
      />
      <AuthField
        label="Senha"
        value={password}
        onChangeText={setPassword}
        error={passwordError}
        secure
        autoCapitalize="none"
        autoComplete="current-password"
        textContentType="password"
        placeholder="Sua senha"
        editable={!submitting}
        onSubmitEditing={submit}
        returnKeyType="go"
      />

      {serverError ? <Text style={styles.serverError} accessibilityLiveRegion="polite">{serverError}</Text> : null}

      <PrimaryButton label="Entrar" onPress={submit} loading={submitting} />

      <Pressable onPress={() => router.push('/signup')} accessibilityRole="link" style={styles.switch}>
        <Text style={styles.switchText}>
          Não tem conta? <Text style={styles.switchLink}>Criar conta</Text>
        </Text>
      </Pressable>

      <View style={styles.footer}>
        <Text style={styles.footerText}>Sua sessão expira automaticamente após um período de inatividade.</Text>
      </View>
    </AuthLayout>
  );
}

const makeStyles = (palette: Palette) => StyleSheet.create({
  serverError: { color: palette.expense, fontSize: font.body, textAlign: 'center' },
  switch: { alignItems: 'center', paddingVertical: space.sm },
  switchText: { color: palette.textMuted, fontSize: font.body },
  switchLink: { color: palette.primary, fontWeight: '700' },
  footer: { marginTop: space.sm },
  footerText: { color: palette.textMuted, fontSize: font.caption, textAlign: 'center' },
});
