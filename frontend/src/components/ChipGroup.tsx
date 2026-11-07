import { Pressable, StyleSheet, Text, View } from 'react-native';
import { font, radius, space, type Palette } from '@/theme/tokens';
import { useStyles } from '@/theme/theme-context';

interface ChipGroupProps<T extends string | number> {
  readonly value: T;
  readonly options: readonly { readonly value: T; readonly label: string }[];
  readonly onChange: (value: T) => void;
}

/** Escolha de uma opção entre várias, em "fichas" que quebram de linha (para listas maiores que o Segmented comporta). */
export function ChipGroup<T extends string | number>({ value, options, onChange }: ChipGroupProps<T>) {
  const styles = useStyles(makeStyles);

  return (
    <View style={styles.chips} accessibilityRole="radiogroup">
      {options.map((option) => {
        const selected = option.value === value;
        return (
          <Pressable
            key={option.value}
            onPress={() => onChange(option.value)}
            accessibilityRole="radio"
            accessibilityState={{ selected }}
            style={[styles.chip, selected && styles.chipSelected]}
          >
            <Text style={[styles.chipText, selected && styles.chipTextSelected]}>{option.label}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    chips: { flexDirection: 'row', flexWrap: 'wrap', gap: space.sm },
    chip: {
      paddingHorizontal: space.lg,
      paddingVertical: space.sm,
      borderRadius: radius.pill,
      backgroundColor: palette.surfaceRaised,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: palette.border,
    },
    chipSelected: { backgroundColor: palette.primaryStrong, borderColor: palette.primaryStrong },
    chipText: { color: palette.textMuted, fontWeight: '600', fontSize: font.body },
    chipTextSelected: { color: '#FFFFFF' },
  });
