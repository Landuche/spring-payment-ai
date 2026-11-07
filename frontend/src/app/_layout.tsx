import { Stack, useRouter, useSegments } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { ActivityIndicator, View } from 'react-native';
import { ToastProvider } from '@/components/Toast';
import { AuthProvider, useAuth } from '@/lib/auth-context';
import { ThemeProvider, useTheme } from '@/theme/theme-context';

const PUBLIC_ROUTES = new Set(['login', 'signup']);

/** Guarda de rotas: sem sessão, só telas públicas; com sessão, nada de login/signup. */
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
    // Sem isso o iOS mostra o nome da rota anterior, "(tabs)". Só a seta serve de "voltar".
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

/** A barra de status acompanha o tema: claro no fundo escuro e vice-versa. */
function ThemedShell() {
  const { resolved } = useTheme();
  return (
    <>
      <StatusBar style={resolved === 'dark' ? 'light' : 'dark'} />
      <Navigator />
    </>
  );
}

export default function RootLayout() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <ToastProvider>
          <ThemedShell />
        </ToastProvider>
      </AuthProvider>
    </ThemeProvider>
  );
}
