import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useColorScheme, useWindowDimensions } from 'react-native';
import { prefsStorage } from '@/lib/prefs-storage';
import { darkPalette, lightPalette, WIDE_BREAKPOINT, type Palette } from './tokens';

export type ThemeMode = 'system' | 'light' | 'dark';

const THEME_KEY = 'fintech.theme.v1';
const MODES: readonly ThemeMode[] = ['system', 'light', 'dark'];

interface ThemeValue {
  readonly mode: ThemeMode;
  readonly resolved: 'light' | 'dark';
  readonly palette: Palette;
  readonly setMode: (mode: ThemeMode) => void;
}

const ThemeContext = createContext<ThemeValue | null>(null);

export function ThemeProvider({ children }: { readonly children: ReactNode }) {
  const system = useColorScheme();
  const [mode, setModeState] = useState<ThemeMode>('system');

  useEffect(() => {
    let alive = true;
    void prefsStorage.read(THEME_KEY).then((saved) => {
      if (alive && saved && (MODES as readonly string[]).includes(saved)) {
        setModeState(saved as ThemeMode);
      }
    });
    return () => {
      alive = false;
    };
  }, []);

  const setMode = useCallback((next: ThemeMode) => {
    setModeState(next);
    void prefsStorage.write(THEME_KEY, next);
  }, []);

  const resolved: 'light' | 'dark' = mode === 'system' ? (system === 'light' ? 'light' : 'dark') : mode;
  const palette = resolved === 'light' ? lightPalette : darkPalette;

  const value = useMemo<ThemeValue>(
    () => ({ mode, resolved, palette, setMode }),
    [mode, resolved, palette, setMode],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme(): ThemeValue {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme precisa estar dentro de ThemeProvider');
  }
  return context;
}

export function useStyles<T>(factory: (palette: Palette) => T): T {
  const { palette } = useTheme();
  return useMemo(() => factory(palette), [factory, palette]);
}

export function useIsWide(): boolean {
  const { width } = useWindowDimensions();
  return width >= WIDE_BREAKPOINT;
}
