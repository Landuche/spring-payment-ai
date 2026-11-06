import type { ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { font, radius, space, type Palette } from '@/theme/tokens';
import { useStyles } from '@/theme/theme-context';

interface CardProps {
  readonly title?: string;
  readonly action?: ReactNode;
  readonly children: ReactNode;
}

/** Superfície elevada com título opcional. Base visual de quase todas as seções. */
export function Card({ title, action, children }: CardProps) {
  const styles = useStyles(makeStyles);

  return (
    <View style={styles.card}>
      {title ? (
        <View style={styles.header}>
          <Text style={styles.title}>{title}</Text>
          {action}
        </View>
      ) : null}
      {children}
    </View>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    card: {
      backgroundColor: palette.surface,
      borderRadius: radius.lg,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: palette.border,
      padding: space.lg,
      gap: space.md,
    },
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
    title: { color: palette.text, fontSize: font.subtitle, fontWeight: '700' },
  });
