import { StyleSheet, Text, View } from 'react-native';
import { Markdown } from '@/components/Markdown';
import { TransferProposalCard, type TransferCardState } from '@/components/TransferProposalCard';
import { font, radius, space, type Palette } from '@/theme/tokens';
import { useStyles, useTheme } from '@/theme/theme-context';

export interface ChatMessage {
  readonly id: string;
  readonly role: 'user' | 'assistant';
  readonly text: string;
  readonly kind?: 'text' | 'voice';
  /** Cartão de confirmação, quando o Claudinho preparou uma transferência nesta resposta. */
  readonly transfer?: TransferCardState;
}

interface ChatBubbleProps {
  readonly message: ChatMessage;
  readonly onConfirmTransfer?: (messageId: string) => void;
  readonly onCancelTransfer?: (messageId: string) => void;
}

export function ChatBubble({ message, onConfirmTransfer, onCancelTransfer }: ChatBubbleProps) {
  const styles = useStyles(makeStyles);
  const { palette } = useTheme();
  const isUser = message.role === 'user';

  return (
    <View style={[styles.wrap, isUser ? styles.wrapUser : styles.wrapAssistant]}>
      <View style={[styles.bubble, isUser ? styles.bubbleUser : styles.bubbleAssistant]}>
        {message.kind === 'voice' ? <Text style={styles.tag}>transcrição de voz</Text> : null}
        {isUser ? (
          <Text style={[styles.text, styles.textUser]}>{message.text}</Text>
        ) : (
          <Markdown text={message.text} color={palette.text} />
        )}
        {message.transfer ? (
          <TransferProposalCard
            state={message.transfer}
            onConfirm={() => onConfirmTransfer?.(message.id)}
            onCancel={() => onCancelTransfer?.(message.id)}
          />
        ) : null}
      </View>
    </View>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    wrap: { flexDirection: 'row', width: '100%' },
    wrapUser: { justifyContent: 'flex-end' },
    wrapAssistant: { justifyContent: 'flex-start' },
    bubble: {
      maxWidth: '82%',
      paddingHorizontal: space.lg,
      paddingVertical: space.md,
      borderRadius: radius.lg,
      gap: 4,
    },
    bubbleUser: { backgroundColor: palette.primaryStrong, borderBottomRightRadius: 6 },
    bubbleAssistant: {
      backgroundColor: palette.surfaceRaised,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: palette.border,
      borderBottomLeftRadius: 6,
    },
    tag: { color: palette.textMuted, fontSize: font.caption },
    text: { color: palette.text, fontSize: font.body, lineHeight: 21 },
    textUser: { color: '#FFFFFF' },
  });
