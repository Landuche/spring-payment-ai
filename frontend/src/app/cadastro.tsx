import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, Text } from 'react-native';
import { AuthField, AuthLayout, PrimaryButton } from '@/components/AuthLayout';
import { ApiError } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import { validateEmail, validateName, validatePassword } from '@/lib/validation';
import { font, space, type Palette } from '@/theme/tokens';
import { useStyles } from '@/theme/theme-context';

export default function SignUpScreen() {
  const router = useRouter();
  const { signUp } = useAuth();
  const styles = useStyles(makeStyles);

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [touched, setTouched] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const nameError = touched ? validateName(name) : undefined;
  const emailError = touched ? validateEmail(email) : undefined;
  const passwordError = touched ? validatePassword(password) : undefined;

  const submit = async () => {
    setTouched(true);
    setServerError(null);
    if (validateName(name) || validateEmail(email) || validatePassword(password)) return;

    setSubmitting(true);
    try {
      await signUp(name.trim(), email.trim(), password);
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : 'Não foi possível criar sua conta.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout title="Criar conta" subtitle="Sua conta corrente é aberta junto com o cadastro.">
      <AuthField
        label="Nome"
        value={name}
        onChangeText={setName}
        error={nameError}
        autoComplete="name"
        textContentType="name"
        placeholder="Como devemos te chamar?"
        editable={!submitting}
      />
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
        autoComplete="new-password"
        textContentType="newPassword"
        placeholder="Mínimo de 8 caracteres"
        editable={!submitting}
        onSubmitEditing={submit}
        returnKeyType="go"
      />

      {serverError ? <Text style={styles.serverError} accessibilityLiveRegion="polite">{serverError}</Text> : null}

      <PrimaryButton label="Criar conta" onPress={submit} loading={submitting} />

      <Pressable onPress={() => router.back()} accessibilityRole="link" style={styles.switch}>
        <Text style={styles.switchText}>
          Já tem conta? <Text style={styles.switchLink}>Entrar</Text>
        </Text>
      </Pressable>
    </AuthLayout>
  );
}

const makeStyles = (palette: Palette) => StyleSheet.create({
  serverError: { color: palette.expense, fontSize: font.body, textAlign: 'center' },
  switch: { alignItems: 'center', paddingVertical: space.sm },
  switchText: { color: palette.textMuted, fontSize: font.body },
  switchLink: { color: palette.primary, fontWeight: '700' },
});
