import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, Text, View } from 'react-native';
import { formatBRL, formatMoment } from '@/lib/format';
import type { AppNotification } from '@/lib/types';
import { font, radius, space, type Palette } from '@/theme/tokens';
import { useStyles, useTheme } from '@/theme/theme-context';

function sentence(notification: AppNotification): string {
  const amount = formatBRL(notification.amount);
  return notification.type === 'TRANSFER_SENT'
    ? `Você enviou ${amount} para ${notification.counterpartName}`
    : `Você recebeu ${amount} de ${notification.counterpartName}`;
}

export function NotificationItem({ notification }: { readonly notification: AppNotification }) {
  const { palette } = useTheme();
  const styles = useStyles(makeStyles);
  const received = notification.type === 'TRANSFER_RECEIVED';

  return (
    <View style={styles.row} accessibilityRole="text">
      <View style={[styles.iconWrap, { backgroundColor: received ? 'rgba(52,211,153,0.14)' : 'rgba(139,92,246,0.16)' }]}>
        <Ionicons
          name={received ? 'arrow-down-circle-outline' : 'arrow-up-circle-outline'}
          size={22}
          color={received ? palette.income : palette.primary}
        />
      </View>
      <View style={styles.middle}>
        <Text style={styles.title} numberOfLines={2}>
          {sentence(notification)}
        </Text>
        <Text style={styles.meta}>{formatMoment(notification.createdAt)}</Text>
      </View>
    </View>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    row: { flexDirection: 'row', alignItems: 'center', gap: space.md, paddingVertical: space.md },
    iconWrap: { width: 42, height: 42, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center' },
    middle: { flex: 1, gap: 2 },
    title: { color: palette.text, fontSize: font.body, fontWeight: '600' },
    meta: { color: palette.textMuted, fontSize: font.caption },
  });
