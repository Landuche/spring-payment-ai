import { Ionicons } from '@expo/vector-icons';
import { Tabs } from 'expo-router';
import { Platform } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { SIDEBAR_WIDTH } from '@/theme/tokens';
import { useIsWide, useTheme } from '@/theme/theme-context';

/** Altura da barra sem a área segura do aparelho (ícone + rótulo). */
const TAB_BAR_CONTENT_HEIGHT = 40;

type IconName = React.ComponentProps<typeof Ionicons>['name'];

function tabIcon(active: IconName, inactive: IconName) {
  return function TabIcon({ color, focused }: { color: unknown; focused: boolean }) {
    return <Ionicons name={focused ? active : inactive} size={22} color={color as string} />;
  };
}

/**
 * No celular a navegação fica embaixo. No desktop/web (janela larga) vira uma barra lateral:
 * uma barra horizontal esticada ocupava a largura toda da janela.
 */
export default function TabsLayout() {
  const { palette } = useTheme();
  const wide = useIsWide();
  // A área segura inferior (indicador da tela inicial no iPhone) entra explicitamente na altura da barra,
  // para os rótulos ficarem acima dela e nada ser empurrado para fora da tela.
  const insets = useSafeAreaInsets();

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarPosition: wide ? 'left' : 'bottom',
        tabBarLabelPosition: wide ? 'beside-icon' : 'below-icon',
        tabBarActiveTintColor: palette.text,
        tabBarInactiveTintColor: palette.textMuted,
        tabBarStyle: wide
          ? { backgroundColor: palette.surface, borderRightColor: palette.border, width: SIDEBAR_WIDTH, paddingTop: 24 }
          : {
              backgroundColor: palette.surface,
              borderTopColor: palette.border,
              // No web a barra fica compacta; no aparelho, soma a área segura do sistema.
              height: Platform.OS === 'web' ? 60 : TAB_BAR_CONTENT_HEIGHT + insets.bottom,
              paddingBottom: Platform.OS === 'web' ? 0 : insets.bottom,
            },
        // Com cinco abas no celular, "Transferências" não cabe em 13: o rótulo diminui só na barra de baixo.
        tabBarLabelStyle: { fontSize: wide ? 13 : 11, fontWeight: '600' },
      }}
    >
      <Tabs.Screen name="index" options={{ title: 'Início', tabBarIcon: tabIcon('wallet', 'wallet-outline') }} />
      <Tabs.Screen name="statement" options={{ title: 'Extrato', tabBarIcon: tabIcon('receipt', 'receipt-outline') }} />
      <Tabs.Screen
        name="transfers"
        options={{ title: 'Transferências', tabBarIcon: tabIcon('swap-horizontal', 'swap-horizontal-outline') }}
      />
      <Tabs.Screen name="assistant" options={{ title: 'Claudinho', tabBarIcon: tabIcon('chatbubbles', 'chatbubbles-outline') }} />
      <Tabs.Screen name="invest" options={{ title: 'Investir', tabBarIcon: tabIcon('trending-up', 'trending-up-outline') }} />
    </Tabs>
  );
}
