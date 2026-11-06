import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { formatBRL } from '@/lib/format';
import type { TransferProposal } from '@/lib/types';
import { font, radius, space, type Palette } from '@/theme/tokens';
import { useStyles, useTheme } from '@/theme/theme-context';

export type TransferCardStatus = 'pending' | 'sending' | 'done' | 'cancelled';

export interface TransferCardState {
  readonly proposal: TransferProposal;
  readonly status: TransferCardStatus;
  /** Uma por proposta: tocar de novo (ou repetir depois de uma falha) não debita duas vezes. */
  readonly idempotencyKey: string;
}

interface TransferProposalCardProps {
  readonly state: TransferCardState;
  readonly onConfirm: () => void;
  readonly onCancel: () => void;
}

/**
 * Cartão de confirmação que o Claudinho coloca no chat quando prepara uma transferência. O modelo só prepara:
 * quem executa é o cliente, tocando em "Transferir". O nome do titular vem do servidor (mascarado), para conferir.
 */
export function TransferProposalCard({ state, onConfirm, onCancel }: TransferProposalCardProps) {
  const { palette } = useTheme();
  const styles = useStyles(makeStyles);
  const { proposal, status } = state;
  const open = status === 'pending' || status === 'sending';
  const sending = status === 'sending';

  return (
    <View style={styles.card} accessibilityRole="summary">
      <View style={styles.header}>
        <Ionicons name="paper-plane-outline" size={18} color={palette.primary} />
        <Text style={styles.title}>Transferência</Text>
      </View>

      <Row label="Para" value={`${proposal.nomeDestinatario} · conta ${proposal.contaDestino}`} />
      <Row label="Valor" value={formatBRL(proposal.valor)} strong />
      {proposal.descricao ? <Row label="Descrição" value={proposal.descricao} /> : null}

      {open ? (
        <View style={styles.actions}>
          <Pressable
            onPress={onCancel}
            disabled={sending}
            accessibilityRole="button"
            style={({ pressed }) => [styles.button, styles.cancel, (pressed || sending) && { opacity: 0.6 }]}
          >
            <Text style={styles.cancelText}>Cancelar</Text>
          </Pressable>
          <Pressable
            onPress={onConfirm}
            disabled={sending}
            accessibilityRole="button"
            accessibilityState={{ busy: sending }}
            style={({ pressed }) => [styles.button, styles.confirm, (pressed || sending) && { opacity: 0.7 }]}
          >
            <Text style={styles.confirmText}>{sending ? 'Enviando…' : 'Transferir'}</Text>
          </Pressable>
        </View>
      ) : (
        <View style={styles.result}>
          <Ionicons
            name={status === 'done' ? 'checkmark-circle' : 'close-circle-outline'}
            size={18}
            color={status === 'done' ? palette.income : palette.textMuted}
          />
          <Text style={[styles.resultText, status === 'done' && { color: palette.income }]}>
            {status === 'done' ? 'Transferência enviada' : 'Transferência cancelada'}
          </Text>
        </View>
      )}
    </View>
  );
}

function Row({ label, value, strong = false }: { readonly label: string; readonly value: string; readonly strong?: boolean }) {
  const styles = useStyles(makeStyles);
  return (
    <View style={styles.row}>
      <Text style={styles.label}>{label}</Text>
      <Text style={[styles.value, strong && styles.valueStrong]}>{value}</Text>
    </View>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    card: {
      minWidth: 240,
      gap: space.sm,
      padding: space.md,
      borderRadius: radius.md,
      backgroundColor: palette.surface,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: palette.border,
    },
    header: { flexDirection: 'row', alignItems: 'center', gap: space.sm },
    title: { color: palette.text, fontSize: font.body, fontWeight: '800' },
    row: { gap: 2 },
    label: { color: palette.textMuted, fontSize: font.caption, textTransform: 'uppercase', letterSpacing: 0.6 },
    value: { color: palette.text, fontSize: font.body, lineHeight: 20 },
    valueStrong: { fontSize: font.subtitle, fontWeight: '800', fontVariant: ['tabular-nums'] },
    actions: { flexDirection: 'row', gap: space.sm, marginTop: space.xs },
    button: { flexGrow: 1, height: 42, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center' },
    cancel: { backgroundColor: palette.surfaceRaised, borderWidth: StyleSheet.hairlineWidth, borderColor: palette.border },
    cancelText: { color: palette.text, fontWeight: '700' },
    confirm: { backgroundColor: palette.primaryStrong },
    confirmText: { color: '#FFFFFF', fontWeight: '700' },
    result: { flexDirection: 'row', alignItems: 'center', gap: space.sm, marginTop: space.xs },
    resultText: { color: palette.textMuted, fontWeight: '700' },
  });
