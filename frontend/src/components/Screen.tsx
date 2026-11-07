import type { ReactNode } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View, type ScrollViewProps } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { CONTENT_MAX_WIDTH, SIDEBAR_WIDTH, space, type Palette } from '@/theme/tokens';
import { useIsWide, useStyles } from '@/theme/theme-context';

interface ScreenProps extends Pick<ScrollViewProps, 'refreshControl' | 'keyboardShouldPersistTaps'> {
  readonly children: ReactNode;
  readonly scroll?: boolean;
  readonly sidebar?: boolean;
}

export function Screen({ children, scroll = true, refreshControl, keyboardShouldPersistTaps, sidebar = false }: ScreenProps) {
  const styles = useStyles(makeStyles);
  const wide = useIsWide();
  const balance = sidebar && wide ? { paddingRight: SIDEBAR_WIDTH } : null;

  const content = (
    <View style={[styles.column, { maxWidth: wide ? CONTENT_MAX_WIDTH.wide : CONTENT_MAX_WIDTH.compact }]}>
      {children}
    </View>
  );

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      {/* No Android, a janela não encolhe sozinha com o teclado (a tela vai até a borda): o padding faz isso. */}
      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'android' ? 'padding' : undefined}>
        {scroll ? (
          <ScrollView
            contentContainerStyle={[styles.scroll, balance]}
            showsVerticalScrollIndicator={false}
            refreshControl={refreshControl}
            keyboardShouldPersistTaps={keyboardShouldPersistTaps ?? 'handled'}
            automaticallyAdjustKeyboardInsets
            keyboardDismissMode="on-drag"
          >
            {content}
          </ScrollView>
        ) : (
          <View style={[styles.fill, balance]}>{content}</View>
        )}
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    safe: { flex: 1, backgroundColor: palette.background },
    flex: { flex: 1 },
    fill: { flex: 1, alignItems: 'center' },
    scroll: { alignItems: 'center', paddingBottom: space.xxl * 2 },
    column: {
      width: '100%',
      paddingHorizontal: space.lg,
      paddingTop: space.md,
      gap: space.lg,
      flex: 1,
    },
  });
