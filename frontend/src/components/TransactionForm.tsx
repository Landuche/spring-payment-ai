import { useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { Card } from '@/components/Card';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { AmountInput, makeFormStyles } from '@/components/form';
import { useToast } from '@/components/Toast';
import { api, ApiError } from '@/lib/api';
import { formatBRL, parseAmount } from '@/lib/format';
import { CATEGORIES, CATEGORY_LABEL, type Category } from '@/lib/types';
import { font, radius, space, type Palette } from '@/theme/tokens';
import { useStyles, useTheme } from '@/theme/theme-context';

export type TransactionMode = 'deposito' | 'gasto';

interface TransactionFormProps {
  /** "deposito" soma ao saldo; "gasto" registra uma despesa com categoria. */
  readonly mode: TransactionMode;
  readonly onDone: () => void;
}

/** Depósito (entrada) e registro manual de gasto (saída). Os dois pedem confirmação antes de salvar. */
export function TransactionForm({ mode, onDone }: TransactionFormProps) {
  const { palette } = useTheme();
  const styles = useStyles(makeStyles);
  const form = useStyles(makeFormStyles);
  const toast = useToast();
  const isDeposit = mode === 'deposito';

  const [description, setDescription] = useState('');
  const [amountText, setAmountText] = useState('');
  const [category, setCategory] = useState<Category>('GROCERIES');
  const [confirming, setConfirming] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const amount = parseAmount(amountText);
  const amountValid = Number.isFinite(amount) && amount > 0;
  const canSubmit = amountValid && !submitting;
  // Depósito não tem categoria na tela; entra como "Outros".
  const effectiveCategory: Category = isDeposit ? 'OTHER' : category;

  const submit = async () => {
    if (!canSubmit) return;
    setSubmitting(true);
    try {
      await api.createTransaction({
        description,
        category: effectiveCategory,
        type: isDeposit ? 'INCOME' : 'EXPENSE',
        amount,
      });
      setConfirming(false);
      toast.success(
        isDeposit
          ? `Depósito de ${formatBRL(amount)} realizado`
          : `Gasto de ${formatBRL(amount)} registrado em ${CATEGORY_LABEL[category]}`,
      );
      onDone();
    } catch (e) {
      setConfirming(false);
      toast.error(e instanceof ApiError ? e.message : 'Não foi possível salvar. Tente novamente.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <Card>
        <Text style={form.label}>Valor</Text>
        <AmountInput
          value={amountText}
          onChangeText={setAmountText}
          accessibilityLabel={isDeposit ? 'Valor do depósito' : 'Valor do gasto'}
          editable={!submitting}
        />

        <Text style={form.label}>Descrição (opcional)</Text>
        <TextInput
          value={description}
          onChangeText={setDescription}
          placeholder={isDeposit ? 'Ex.: Salário' : 'Ex.: Supermercado do bairro'}
          placeholderTextColor={palette.textMuted}
          maxLength={120}
          style={form.input}
          accessibilityLabel="Descrição"
          editable={!submitting}
        />

        {isDeposit ? null : (
          <>
            <Text style={form.label}>Categoria</Text>
            <View style={styles.chips}>
              {CATEGORIES.map((value) => {
                const selected = value === category;
                return (
                  <Pressable
                    key={value}
                    onPress={() => setCategory(value)}
                    accessibilityRole="button"
                    accessibilityState={{ selected }}
                    style={[styles.chip, selected && styles.chipSelected]}
                  >
                    <Text style={[styles.chipText, selected && styles.chipTextSelected]}>{CATEGORY_LABEL[value]}</Text>
                  </Pressable>
                );
              })}
            </View>
          </>
        )}

        <Pressable
          onPress={() => setConfirming(true)}
          disabled={!canSubmit}
          accessibilityRole="button"
          accessibilityState={{ disabled: !canSubmit }}
          style={({ pressed }) => [form.submit, !canSubmit && form.submitDisabled, pressed && { opacity: 0.85 }]}
        >
          <Text style={form.submitText}>{isDeposit ? 'Revisar depósito' : 'Revisar gasto'}</Text>
        </Pressable>
      </Card>

      <ConfirmDialog
        visible={confirming}
        title={isDeposit ? 'Confirmar depósito' : 'Confirmar gasto'}
        message={
          isDeposit
            ? `Adicionar ${formatBRL(amountValid ? amount : 0)} ao seu saldo?`
            : `Registrar um gasto de ${formatBRL(amountValid ? amount : 0)} em ${CATEGORY_LABEL[category]}?`
        }
        confirmLabel={isDeposit ? 'Depositar' : 'Registrar'}
        busy={submitting}
        onConfirm={submit}
        onCancel={() => setConfirming(false)}
      />
    </>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
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
  });
