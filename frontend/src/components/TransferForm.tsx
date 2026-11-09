import { Ionicons } from '@expo/vector-icons';
import { randomUUID } from 'expo-crypto';
import { useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { Card } from '@/components/Card';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { AmountInput, makeFormStyles } from '@/components/form';
import { useToast } from '@/components/Toast';
import { api, ApiError, describeWait } from '@/lib/api';
import { formatBRL, parseAmount } from '@/lib/format';
import type { AccountInfo } from '@/lib/types';
import { validateEmail } from '@/lib/validation';
import { font, radius, space, type Palette } from '@/theme/tokens';
import { useStyles, useTheme } from '@/theme/theme-context';

type Destination = { number: string } | { email: string };

/** Letras ou @ indicam que o usuário digitou um e-mail. */
const isEmailInput = (value: string) => /[a-z@]/i.test(value);
/** Aceita "1234-567890" ou só dígitos, devolve só os 10 dígitos. */
const onlyDigits = (value: string) => value.replace(/\D/g, '').slice(0, 10);
const formatNumber = (digits: string) => (digits.length > 4 ? `${digits.slice(0, 4)}-${digits.slice(4)}` : digits);

function limitMessage(error: ApiError): string {
  const wait = describeWait(error.retryAfterSeconds ?? 3600);
  return `Você fez muitas buscas de destinatário sem concluir uma transferência. Tente novamente em ${wait}.`;
}

function lookupMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 429) return limitMessage(error);
  if (error instanceof ApiError && error.status === 404) return 'Não encontramos nenhuma conta com esses dados.';
  return 'Não foi possível buscar o destinatário. Tente novamente.';
}

function submitMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 429) return limitMessage(error);
  if (error instanceof ApiError) return error.message;
  return 'Não foi possível concluir a transferência. Tente novamente: a mesma operação não será cobrada duas vezes.';
}

/** Resolve o que o usuário digitou em um destino válido, ou devolve o erro de validação. */
function toDestination(raw: string): { destination: Destination } | { error: string } {
  const value = raw.trim();
  if (isEmailInput(value)) {
    const error = validateEmail(value);
    return error ? { error } : { destination: { email: value } };
  }
  const digits = onlyDigits(value);
  if (digits.length !== 10) {
    return { error: 'Informe o número da conta (10 dígitos) ou o e-mail do destinatário.' };
  }
  return { destination: { number: digits } };
}

export function TransferForm({ onDone }: { readonly onDone: () => void }) {
  const { palette } = useTheme();
  const styles = useStyles(makeStyles);
  const form = useStyles(makeFormStyles);
  const toast = useToast();

  const [target, setTarget] = useState('');
  const [recipient, setRecipient] = useState<AccountInfo | null>(null);
  const [lookupError, setLookupError] = useState<string | null>(null);
  const [looking, setLooking] = useState(false);

  const [amountText, setAmountText] = useState('');
  const [description, setDescription] = useState('');
  const [confirming, setConfirming] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [idempotencyKey] = useState(() => randomUUID());

  const amount = parseAmount(amountText);
  const amountValid = Number.isFinite(amount) && amount > 0;
  const ready = !!recipient && amountValid && !submitting;

  const lookup = async () => {
    const resolved = toDestination(target);
    if ('error' in resolved) {
      setLookupError(resolved.error);
      return;
    }
    setLookupError(null);
    setLooking(true);
    try {
      setRecipient(await api.lookupAccount(resolved.destination));
    } catch (e) {
      setRecipient(null);
      setLookupError(lookupMessage(e));
    } finally {
      setLooking(false);
    }
  };

  const changeTarget = (value: string) => {
    setTarget(isEmailInput(value) ? value : formatNumber(onlyDigits(value)));
    setRecipient(null);
    setLookupError(null);
  };

  const submit = async () => {
    const resolved = toDestination(target);
    if (!recipient || !amountValid || submitting || 'error' in resolved) return;
    setSubmitting(true);
    try {
      await api.transfer({ destination: resolved.destination, amount, description }, idempotencyKey);
      setConfirming(false);
      toast.success(`${formatBRL(amount)} enviados para ${recipient.holderName}`);
      onDone();
    } catch (e) {
      setConfirming(false);
      toast.error(submitMessage(e));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <Card title="Para quem?">
        <Text style={form.label}>Número da conta ou e-mail</Text>
        <View style={styles.row}>
          <TextInput
            value={target}
            onChangeText={changeTarget}
            keyboardType={isEmailInput(target) ? 'email-address' : 'default'}
            autoCapitalize="none"
            autoCorrect={false}
            placeholder="0000-000000 ou nome@exemplo.com"
            placeholderTextColor={palette.textMuted}
            style={[form.input, styles.flex, styles.tabular]}
            accessibilityLabel="Número da conta ou e-mail do destinatário"
            editable={!submitting}
            onSubmitEditing={lookup}
            returnKeyType="search"
          />
          <Pressable
            onPress={lookup}
            disabled={looking || submitting}
            accessibilityRole="button"
            style={({ pressed }) => [styles.lookupButton, (pressed || looking) && { opacity: 0.7 }]}
          >
            <Text style={styles.lookupText}>{looking ? '…' : 'Buscar'}</Text>
          </Pressable>
        </View>
        {lookupError ? <Text style={form.error} accessibilityLiveRegion="polite">{lookupError}</Text> : null}

        {recipient ? (
          <View style={styles.recipient} accessibilityRole="text">
            <Ionicons name="person-circle-outline" size={34} color={palette.primary} />
            <View style={styles.flex}>
              <Text style={styles.recipientName}>{recipient.holderName}</Text>
              <Text style={styles.recipientMeta}>Conta {recipient.number}</Text>
            </View>
            <Ionicons name="checkmark-circle" size={22} color={palette.income} />
          </View>
        ) : null}
      </Card>

      <Card title="Quanto?">
        <AmountInput
          value={amountText}
          onChangeText={setAmountText}
          accessibilityLabel="Valor da transferência"
          editable={!submitting}
        />
        <Text style={form.label}>Descrição (opcional)</Text>
        <TextInput
          value={description}
          onChangeText={setDescription}
          placeholder="Ex.: Divisão do almoço"
          placeholderTextColor={palette.textMuted}
          maxLength={120}
          style={form.input}
          accessibilityLabel="Descrição da transferência"
          editable={!submitting}
        />
      </Card>

      <Pressable
        onPress={() => setConfirming(true)}
        disabled={!ready}
        accessibilityRole="button"
        accessibilityState={{ disabled: !ready }}
        style={({ pressed }) => [form.submit, !ready && form.submitDisabled, pressed && { opacity: 0.85 }]}
      >
        <Text style={form.submitText}>Revisar transferência</Text>
      </Pressable>

      <ConfirmDialog
        visible={confirming}
        title="Confirmar transferência"
        message={
          <Text style={styles.summary}>
            Você vai transferir <Text style={styles.summaryStrong}>{formatBRL(amountValid ? amount : 0)}</Text> para{' '}
            <Text style={styles.summaryStrong}>{recipient?.holderName}</Text> (conta {recipient?.number}).{'\n\n'}
            Transferências são instantâneas e não podem ser desfeitas pelo app.
          </Text>
        }
        confirmLabel="Transferir"
        busy={submitting}
        onConfirm={submit}
        onCancel={() => setConfirming(false)}
      />
    </>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    flex: { flex: 1 },
    tabular: { fontVariant: ['tabular-nums'] },
    row: { flexDirection: 'row', alignItems: 'center', gap: space.sm },
    lookupButton: {
      height: 50,
      paddingHorizontal: space.lg,
      borderRadius: radius.md,
      backgroundColor: palette.primaryStrong,
      alignItems: 'center',
      justifyContent: 'center',
    },
    lookupText: { color: '#FFFFFF', fontWeight: '700' },
    recipient: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: space.md,
      padding: space.md,
      borderRadius: radius.md,
      backgroundColor: palette.surfaceRaised,
    },
    recipientName: { color: palette.text, fontSize: font.subtitle, fontWeight: '700' },
    recipientMeta: { color: palette.textMuted, fontSize: font.caption },
    summary: { color: palette.textMuted, fontSize: font.body, lineHeight: 21 },
    summaryStrong: { color: palette.text, fontWeight: '700' },
  });
