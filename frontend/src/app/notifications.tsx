import { useFocusEffect } from 'expo-router';
import { useCallback, useEffect } from 'react';
import { RefreshControl, StyleSheet, Text, View } from 'react-native';
import { Card } from '@/components/Card';
import { NotificationItem } from '@/components/NotificationItem';
import { Screen } from '@/components/Screen';
import { useNotifications } from '@/lib/notifications-context';
import { font, radius, space, type Palette } from '@/theme/tokens';
import { useStyles, useTheme } from '@/theme/theme-context';

/**
 * Notificações de transferência. Abrir a tela marca tudo como lido (o contador do sino zera), mas as novas
 * continuam com um ponto colorido enquanto a tela está aberta, para o usuário ver o que chegou.
 */
export default function NotificationsScreen() {
  const { palette } = useTheme();
  const styles = useStyles(makeStyles);
  const { items, unread, loading, fresh, refresh, markAllRead } = useNotifications();

  useFocusEffect(
    useCallback(() => {
      void refresh();
    }, [refresh]),
  );

  useEffect(() => {
    if (unread > 0) markAllRead();
  }, [unread, markAllRead]);

  return (
    <Screen refreshControl={<RefreshControl refreshing={loading} onRefresh={refresh} tintColor={palette.text} />}>
      <View style={styles.header}>
        <Text style={styles.subtitle}>{fresh.size > 0 ? `${fresh.size} ${fresh.size === 1 ? 'nova' : 'novas'}` : 'Tudo em dia'}</Text>
      </View>

      <Card>
        {items.length === 0 ? (
          <Text style={styles.empty}>
            {loading ? 'Carregando...' : 'Nenhuma transferência ainda. Quando você enviar ou receber dinheiro, aparece aqui.'}
          </Text>
        ) : (
          items.map((item, index) => {
            const isFresh = fresh.has(item.id);
            return (
              <View key={item.id}>
                {index > 0 ? <View style={styles.separator} /> : null}
                <View style={styles.row}>
                  <View
                    style={[styles.dot, { backgroundColor: isFresh ? palette.accent : 'transparent' }]}
                    accessibilityLabel={isFresh ? 'Nova' : 'Já vista'}
                  />
                  <View style={styles.flex}>
                    <NotificationItem notification={item} />
                  </View>
                </View>
              </View>
            );
          })
        )}
      </Card>
    </Screen>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    flex: { flex: 1 },
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: space.md },
    subtitle: { color: palette.textMuted, fontSize: font.body },
    empty: { color: palette.textMuted, fontSize: font.body, lineHeight: 20 },
    separator: { height: StyleSheet.hairlineWidth, backgroundColor: palette.border },
    row: { flexDirection: 'row', alignItems: 'center', gap: space.sm },
    dot: { width: 8, height: 8, borderRadius: radius.pill },
  });
