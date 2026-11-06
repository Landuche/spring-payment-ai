import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { font, radius, space, type Palette } from '@/theme/tokens';
import { useStyles, useTheme, type ThemeMode } from '@/theme/theme-context';

type IconName = React.ComponentProps<typeof Ionicons>['name'];

const OPTIONS: readonly { readonly mode: ThemeMode; readonly label: string; readonly icon: IconName }[] = [
  { mode: 'system', label: 'Sistema', icon: 'phone-portrait-outline' },
  { mode: 'light', label: 'Claro', icon: 'sunny-outline' },
  { mode: 'dark', label: 'Escuro', icon: 'moon-outline' },
];

/** Seletor de tema: sistema, claro ou escuro. A escolha fica guardada no aparelho. */
export function ThemeToggle() {
  const { mode, setMode, palette } = useTheme();
  const styles = useStyles(makeStyles);

  return (
    <View style={styles.wrap} accessibilityRole="radiogroup">
      {OPTIONS.map((option) => {
        const selected = option.mode === mode;
        return (
          <Pressable
            key={option.mode}
            onPress={() => setMode(option.mode)}
            accessibilityRole="radio"
            accessibilityState={{ selected }}
            accessibilityLabel={`Tema ${option.label}`}
            style={[styles.segment, selected && styles.segmentSelected]}
          >
            <Ionicons name={option.icon} size={15} color={selected ? '#FFFFFF' : palette.textMuted} />
            <Text style={[styles.label, selected && styles.labelSelected]}>{option.label}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    wrap: {
      flexDirection: 'row',
      alignSelf: 'flex-start',
      padding: 4,
      borderRadius: radius.pill,
      backgroundColor: palette.surfaceRaised,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: palette.border,
    },
    segment: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 6,
      paddingHorizontal: space.md,
      paddingVertical: space.sm,
      borderRadius: radius.pill,
    },
    segmentSelected: { backgroundColor: palette.primaryStrong },
    label: { color: palette.textMuted, fontSize: font.caption, fontWeight: '700' },
    labelSelected: { color: '#FFFFFF' },
  });
