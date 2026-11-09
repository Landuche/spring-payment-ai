import { StyleSheet, Text, View } from 'react-native';
import { font, space } from '@/theme/tokens';

/**
 * Renderizador de Markdown para as respostas do Claudinho.
 */

const INLINE = /(\*\*[^*\n]+\*\*|\*[^*\s][^*\n]*\*)/g;

function renderInline(text: string, color: string, keyPrefix: string) {
  return text.split(INLINE).filter(Boolean).map((part, index) => {
    const key = `${keyPrefix}-${index}`;
    if (part.startsWith('**') && part.endsWith('**') && part.length > 4) {
      return <Text key={key} style={{ fontWeight: '700', color }}>{part.slice(2, -2)}</Text>;
    }
    if (part.startsWith('*') && part.endsWith('*') && part.length > 2) {
      return <Text key={key} style={{ fontStyle: 'italic', color }}>{part.slice(1, -1)}</Text>;
    }
    return <Text key={key} style={{ color }}>{part}</Text>;
  });
}

interface MarkdownProps {
  readonly text: string;
  readonly color: string;
}

export function Markdown({ text, color }: MarkdownProps) {
  const lines = text.split('\n');

  return (
    <View style={styles.block}>
      {lines.map((raw, index) => {
        const line = raw.trimEnd();
        const key = `line-${index}`;

        if (line.trim() === '') {
          return <View key={key} style={styles.gap} />;
        }

        const heading = /^#{1,6}\s+(.*)$/.exec(line);
        if (heading) {
          return (
            <Text key={key} style={[styles.text, styles.heading, { color }]}>
              {renderInline(heading[1], color, key)}
            </Text>
          );
        }

        const bullet = /^\s*[-*]\s+(.*)$/.exec(line);
        if (bullet) {
          return (
            <View key={key} style={styles.bulletRow}>
              <Text style={[styles.text, { color }]}>•</Text>
              <Text style={[styles.text, styles.bulletText]}>{renderInline(bullet[1], color, key)}</Text>
            </View>
          );
        }

        return (
          <Text key={key} style={[styles.text, { color }]}>
            {renderInline(line, color, key)}
          </Text>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  block: { gap: 2 },
  gap: { height: space.xs },
  text: { fontSize: font.body, lineHeight: 21 },
  heading: { fontWeight: '700' },
  bulletRow: { flexDirection: 'row', gap: space.sm, paddingLeft: space.xs },
  bulletText: { flexShrink: 1 },
});
