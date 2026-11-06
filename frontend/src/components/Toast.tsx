import { Ionicons } from '@expo/vector-icons';
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { Animated, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { font, radius, space, type Palette } from '@/theme/tokens';
import { useStyles, useTheme } from '@/theme/theme-context';

type ToastKind = 'success' | 'error';

interface ToastState {
  readonly kind: ToastKind;
  readonly message: string;
}

interface ToastApi {
  readonly success: (message: string) => void;
  readonly error: (message: string) => void;
}

const ToastContext = createContext<ToastApi | null>(null);

const FADE_MS = 180;
const SUCCESS_MS = 2800;
/** Erros têm mensagens mais longas e importam mais: ficam mais tempo na tela. */
const ERROR_MS = 5000;

/** Aviso rápido no topo da tela (sucesso ou erro), que some sozinho. Um aviso novo substitui o anterior. */
export function ToastProvider({ children }: { readonly children: ReactNode }) {
  const styles = useStyles(makeStyles);
  const { palette } = useTheme();
  const insets = useSafeAreaInsets();
  const [toast, setToast] = useState<ToastState | null>(null);
  const [opacity] = useState(() => new Animated.Value(0));
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const hide = useCallback(() => {
    Animated.timing(opacity, { toValue: 0, duration: FADE_MS, useNativeDriver: true }).start(({ finished }) => {
      if (finished) setToast(null);
    });
  }, [opacity]);

  const show = useCallback(
    (kind: ToastKind, message: string) => {
      if (timer.current) clearTimeout(timer.current);
      setToast({ kind, message });
      Animated.timing(opacity, { toValue: 1, duration: FADE_MS, useNativeDriver: true }).start();
      timer.current = setTimeout(hide, kind === 'error' ? ERROR_MS : SUCCESS_MS);
    },
    [hide, opacity],
  );

  useEffect(
    () => () => {
      if (timer.current) clearTimeout(timer.current);
    },
    [],
  );

  const api = useMemo<ToastApi>(
    () => ({ success: (message) => show('success', message), error: (message) => show('error', message) }),
    [show],
  );

  const accent = toast?.kind === 'error' ? palette.expense : palette.income;

  return (
    <ToastContext.Provider value={api}>
      {children}
      {toast ? (
        <Animated.View
          pointerEvents="none"
          accessibilityLiveRegion="polite"
          accessibilityRole="alert"
          style={[styles.wrap, { top: insets.top + space.sm, opacity }]}
        >
          <View style={[styles.toast, { borderColor: accent }]}>
            <Ionicons name={toast.kind === 'error' ? 'alert-circle' : 'checkmark-circle'} size={22} color={accent} />
            <Text style={styles.message}>{toast.message}</Text>
          </View>
        </Animated.View>
      ) : null}
    </ToastContext.Provider>
  );
}

export function useToast(): ToastApi {
  const api = useContext(ToastContext);
  if (!api) throw new Error('useToast precisa estar dentro de ToastProvider');
  return api;
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    wrap: { position: 'absolute', left: 0, right: 0, alignItems: 'center', paddingHorizontal: space.lg },
    toast: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: space.md,
      maxWidth: 520,
      paddingHorizontal: space.lg,
      paddingVertical: space.md,
      borderRadius: radius.md,
      borderWidth: 1,
      backgroundColor: palette.surfaceRaised,
    },
    message: { flexShrink: 1, color: palette.text, fontSize: font.body, fontWeight: '600', lineHeight: 20 },
  });
