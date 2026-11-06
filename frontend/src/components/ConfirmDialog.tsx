import type { ReactNode } from 'react';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { font, radius, space, type Palette } from '@/theme/tokens';
import { useStyles } from '@/theme/theme-context';

interface ConfirmDialogProps {
  readonly visible: boolean;
  readonly title: string;
  /** Texto ou conteúdo com o resumo do que vai acontecer. */
  readonly message: ReactNode;
  readonly confirmLabel: string;
  readonly cancelLabel?: string;
  /** Pinta o botão de confirmar como ação de risco (ex.: sair da conta). */
  readonly destructive?: boolean;
  /** Enquanto a ação roda, os botões ficam travados e o fundo não fecha o modal. */
  readonly busy?: boolean;
  readonly onConfirm: () => void;
  readonly onCancel: () => void;
}

/** Modal de confirmar ou cancelar, igual no celular e na web. */
export function ConfirmDialog({
  visible,
  title,
  message,
  confirmLabel,
  cancelLabel = 'Cancelar',
  destructive = false,
  busy = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const styles = useStyles(makeStyles);

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={busy ? undefined : onCancel}>
      <View style={styles.backdrop}>
        <Pressable
          style={StyleSheet.absoluteFill}
          onPress={busy ? undefined : onCancel}
          accessibilityRole="button"
          accessibilityLabel={cancelLabel}
        />
        <View style={styles.dialog} accessibilityViewIsModal>
          <Text style={styles.title}>{title}</Text>
          {typeof message === 'string' ? <Text style={styles.message}>{message}</Text> : message}

          <View style={styles.actions}>
            <Pressable
              onPress={onCancel}
              disabled={busy}
              accessibilityRole="button"
              style={({ pressed }) => [styles.button, styles.cancel, (pressed || busy) && { opacity: 0.6 }]}
            >
              <Text style={styles.cancelText}>{cancelLabel}</Text>
            </Pressable>
            <Pressable
              onPress={onConfirm}
              disabled={busy}
              accessibilityRole="button"
              accessibilityState={{ busy }}
              style={({ pressed }) => [
                styles.button,
                destructive ? styles.destructive : styles.confirm,
                (pressed || busy) && { opacity: 0.7 },
              ]}
            >
              <Text style={styles.confirmText}>{busy ? 'Aguarde…' : confirmLabel}</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    backdrop: {
      flex: 1,
      alignItems: 'center',
      justifyContent: 'center',
      padding: space.xl,
      backgroundColor: 'rgba(0,0,0,0.6)',
    },
    dialog: {
      width: '100%',
      maxWidth: 420,
      gap: space.md,
      padding: space.xl,
      borderRadius: radius.lg,
      backgroundColor: palette.surface,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: palette.border,
    },
    title: { color: palette.text, fontSize: font.title, fontWeight: '800' },
    message: { color: palette.textMuted, fontSize: font.body, lineHeight: 21 },
    actions: { flexDirection: 'row', gap: space.md, marginTop: space.sm },
    button: { flex: 1, height: 48, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center' },
    cancel: { backgroundColor: palette.surfaceRaised, borderWidth: StyleSheet.hairlineWidth, borderColor: palette.border },
    cancelText: { color: palette.text, fontWeight: '700' },
    confirm: { backgroundColor: palette.primaryStrong },
    destructive: { backgroundColor: palette.expense },
    confirmText: { color: '#FFFFFF', fontWeight: '700' },
  });
