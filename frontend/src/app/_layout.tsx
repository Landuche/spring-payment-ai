import { Stack, useRouter, useSegments } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useEffect, type ReactNode } from 'react';
import { ActivityIndicator, View } from 'react-native';
import { ToastProvider } from '@/components/Toast';
import { AuthProvider, useAuth } from '@/lib/auth-context';
import { NotificationsProvider } from '@/lib/notifications-context';
import { ThemeProvider, useTheme } from '@/theme/theme-context';

const PUBLIC_ROUTES = new Set(['login', 'signup']);

/** sem sessão, só telas públicas */
function AuthGate() {
  const { session, restoring } = useAuth();
  const segments = useSegments();
  const router = useRouter();

  useEffect(() => {
    if (restoring) return;
    const current = segments[0] ?? '';
    const isPublic = PUBLIC_ROUTES.has(current);

    if (!session && !isPublic) {
      router.replace('/login');
    } else if (session && isPublic) {
      router.replace('/');
    }
  }, [restoring, session, segments, router]);

  return null;
}

function Navigator() {
  const { restoring } = useAuth();
  const { palette } = useTheme();

  if (restoring) {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: palette.background }}>
        <ActivityIndicator color={palette.primary} />
      </View>
    );
  }

  const headerOptions = {
    headerShown: true,
    headerStyle: { backgroundColor: palette.surface },
    headerTintColor: palette.text,
    headerBackButtonDisplayMode: 'minimal' as const,
  };

  return (
    <>
      <AuthGate />
      <Stack
        screenOptions={{
          headerShown: false,
          contentStyle: { backgroundColor: palette.background },
        }}
      >
        <Stack.Screen name="(tabs)" />
        <Stack.Screen name="login" />
        <Stack.Screen name="signup" />
        <Stack.Screen name="notifications" options={{ ...headerOptions, title: 'Notificações' }} />
      </Stack>
    </>
  );
}

function ThemedShell() {
  const { resolved } = useTheme();
  return (
    <>
      <StatusBar style={resolved === 'dark' ? 'light' : 'dark'} />
      <Navigator />
    </>
  );
}

/** Uma conta por vez: a chave refaz o estado das notificações quando outra pessoa entra. */
function SessionScope({ children }: { readonly children: ReactNode }) {
  const { session } = useAuth();
  return <NotificationsProvider key={session?.email ?? 'anonymous'}>{children}</NotificationsProvider>;
}

export default function RootLayout() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <SessionScope>
          <ToastProvider>
            <ThemedShell />
          </ToastProvider>
        </SessionScope>
      </AuthProvider>
    </ThemeProvider>
  );
}
