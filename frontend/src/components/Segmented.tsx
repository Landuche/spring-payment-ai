import { Pressable, StyleSheet, Text, View } from 'react-native';
import { radius, space, type Palette } from '@/theme/tokens';
import { useStyles } from '@/theme/theme-context';

interface SegmentedProps<T extends string> {
  readonly value: T;
  readonly options: readonly { readonly value: T; readonly label: string }[];
  readonly onChange: (value: T) => void;
}

export function Segmented<T extends string>({ value, options, onChange }: SegmentedProps<T>) {
  const styles = useStyles(makeStyles);

  return (
    <View style={styles.segmented}>
      {options.map((option) => {
        const selected = option.value === value;
        return (
          <Pressable
            key={option.value}
            onPress={() => onChange(option.value)}
            accessibilityRole="button"
            accessibilityState={{ selected }}
            style={[styles.segment, selected && styles.segmentSelected]}
          >
            <Text
              style={[styles.segmentText, selected && styles.segmentTextSelected]}
              numberOfLines={1}
              adjustsFontSizeToFit
            >
              {option.label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    segmented: { flexDirection: 'row', padding: 4, borderRadius: radius.pill, backgroundColor: palette.surfaceRaised },
    segment: { flex: 1, paddingVertical: space.sm, borderRadius: radius.pill, alignItems: 'center' },
    segmentSelected: { backgroundColor: palette.primaryStrong },
    segmentText: { color: palette.textMuted, fontWeight: '700' },
    segmentTextSelected: { color: '#FFFFFF' },
  });
