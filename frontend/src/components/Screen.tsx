import type { ReactNode } from 'react';
import { ScrollView, StyleSheet, View, type ScrollViewProps } from 'react-native';
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
    </SafeAreaView>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    safe: { flex: 1, backgroundColor: palette.background },
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
