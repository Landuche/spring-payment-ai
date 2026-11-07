import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect, useRouter } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { api } from '@/lib/api';
import type { AccountInfo } from '@/lib/types';
import { BalanceCard } from '@/components/BalanceCard';
import { Card } from '@/components/Card';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { ExpenseBars } from '@/components/ExpenseBars';
import { Screen } from '@/components/Screen';
import { ThemeToggle } from '@/components/ThemeToggle';
import { TransactionItem } from '@/components/TransactionItem';
import { useFinance } from '@/hooks/useFinance';
import { useNotifications } from '@/hooks/useNotifications';
import { useAuth } from '@/lib/auth-context';
import { font, radius, space, type Palette } from '@/theme/tokens';
import { useStyles, useTheme } from '@/theme/theme-context';

const RECENT_COUNT = 5;

export default function DashboardScreen() {
  const router = useRouter();
  const { palette } = useTheme();
  const styles = useStyles(makeStyles);
  const { session, signOut } = useAuth();
  const { balance, transactions, report, loading, error, refresh } = useFinance();
  const { unread, refresh: refreshNotifications } = useNotifications();
  const [account, setAccount] = useState<AccountInfo | null>(null);
  const [confirmingSignOut, setConfirmingSignOut] = useState(false);

  useFocusEffect(
    useCallback(() => {
      void refresh();
      api.getAccount().then(setAccount).catch(() => setAccount(null));
    }, [refresh]),
  );

  const refreshAll = useCallback(() => Promise.all([refresh(), refreshNotifications()]), [refresh, refreshNotifications]);
  const refreshControl = <RefreshControl refreshing={loading} onRefresh={refreshAll} tintColor={palette.text} />;
  const recent = transactions.slice(0, RECENT_COUNT);

  return (
    <Screen refreshControl={refreshControl} sidebar>
      <View style={styles.header}>
        <View style={styles.flex}>
          <Text style={styles.greeting}>Olá{session ? `, ${session.name.split(' ')[0]}` : ''}</Text>
          <Text style={styles.subtitle}>Seu resumo financeiro</Text>
        </View>
        <View style={styles.actions}>
          <Pressable
            onPress={() => router.push('/notifications')}
            accessibilityRole="button"
            accessibilityLabel={unread > 0 ? `Notificações, ${unread} não lidas` : 'Notificações'}
            style={({ pressed }) => [styles.iconButton, pressed && { opacity: 0.8 }]}
          >
            <Ionicons name="notifications-outline" size={20} color={palette.textMuted} />
            {unread > 0 ? (
              <View style={styles.badge}>
                <Text style={styles.badgeText}>{unread > 9 ? '9+' : unread}</Text>
              </View>
            ) : null}
          </Pressable>
          <Pressable
            onPress={() => setConfirmingSignOut(true)}
            accessibilityRole="button"
            accessibilityLabel="Sair da conta"
            style={({ pressed }) => [styles.iconButton, pressed && { opacity: 0.8 }]}
          >
            <Ionicons name="log-out-outline" size={20} color={palette.textMuted} />
          </Pressable>
        </View>
      </View>

      <ThemeToggle />

      {error ? (
        <Card>
          <Text style={styles.error}>{error}</Text>
          <Pressable onPress={refresh} accessibilityRole="button">
            <Text style={styles.retry}>Tentar novamente</Text>
          </Pressable>
        </Card>
      ) : null}

      {loading && !balance ? <ActivityIndicator color={palette.primary} style={styles.loader} /> : null}

      {balance ? <BalanceCard balance={balance} /> : null}

      {account ? (
        <Card title="Minha conta">
          <View style={styles.accountRow}>
            <View style={styles.flex}>
              <Text style={styles.accountLabel}>Número para receber</Text>
              <Text style={styles.accountNumber} accessibilityLabel={`Conta ${account.number}`}>{account.number}</Text>
            </View>
            <Pressable
              onPress={() => router.push('/transfers')}
              accessibilityRole="button"
              style={({ pressed }) => [styles.transferButton, pressed && { opacity: 0.8 }]}
            >
              <Ionicons name="paper-plane-outline" size={16} color="#FFFFFF" />
              <Text style={styles.transferText}>Transferir</Text>
            </Pressable>
          </View>
        </Card>
      ) : null}

      {report ? (
        <Card title="Gastos do mês">
          <ExpenseBars expenses={report.transactions} />
        </Card>
      ) : null}

      <Card
        title="Últimas movimentações"
        action={
          transactions.length > RECENT_COUNT ? (
            <Pressable onPress={() => router.push('/statement')} accessibilityRole="link">
              <Text style={styles.retry}>Ver tudo</Text>
            </Pressable>
          ) : null
        }
      >
        {transactions.length === 0 && !loading ? (
          <Text style={styles.empty}>Nenhuma transação ainda. Registre uma na aba Transferências ou fale com o Claudinho.</Text>
        ) : (
          recent.map((transaction, index) => (
            <View key={transaction.id}>
              {index > 0 ? <View style={styles.separator} /> : null}
              <TransactionItem transaction={transaction} />
            </View>
          ))
        )}
      </Card>

      <ConfirmDialog
        visible={confirmingSignOut}
        title="Sair da conta?"
        message="Você precisará entrar com seu e-mail e senha novamente."
        confirmLabel="Sair"
        destructive
        onConfirm={() => {
          setConfirmingSignOut(false);
          void signOut();
        }}
        onCancel={() => setConfirmingSignOut(false)}
      />
    </Screen>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: space.md },
    flex: { flex: 1 },
    accountRow: { flexDirection: 'row', alignItems: 'center', gap: space.md },
    accountLabel: { color: palette.textMuted, fontSize: font.caption, textTransform: 'uppercase', letterSpacing: 0.6 },
    accountNumber: { color: palette.text, fontSize: font.subtitle, fontWeight: '700', fontVariant: ['tabular-nums'], marginTop: 2 },
    transferButton: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 6,
      paddingHorizontal: space.lg,
      height: 40,
      borderRadius: radius.pill,
      backgroundColor: palette.primaryStrong,
    },
    transferText: { color: '#FFFFFF', fontWeight: '700' },
    greeting: { color: palette.text, fontSize: font.title, fontWeight: '800' },
    subtitle: { color: palette.textMuted, fontSize: font.body, marginTop: 2 },
    actions: { flexDirection: 'row', alignItems: 'center', gap: space.sm },
    iconButton: {
      width: 44,
      height: 44,
      borderRadius: radius.pill,
      backgroundColor: palette.surface,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: palette.border,
      alignItems: 'center',
      justifyContent: 'center',
    },
    badge: {
      position: 'absolute',
      top: 4,
      right: 2,
      minWidth: 18,
      height: 18,
      paddingHorizontal: 4,
      borderRadius: 9,
      backgroundColor: palette.accent,
      alignItems: 'center',
      justifyContent: 'center',
    },
    badgeText: { color: '#FFFFFF', fontSize: 10, fontWeight: '800' },
    loader: { marginVertical: space.xl },
    error: { color: palette.expense, fontSize: font.body },
    retry: { color: palette.primary, fontWeight: '700', marginTop: space.sm },
    empty: { color: palette.textMuted, fontSize: font.body, lineHeight: 20 },
    separator: { height: StyleSheet.hairlineWidth, backgroundColor: palette.border },
  });
