import { Ionicons } from '@expo/vector-icons';
import { useEffect, useState } from 'react';
import { Animated, Pressable, StyleSheet, Text, View } from 'react-native';
import { gradients, type Palette } from '@/theme/tokens';
import { useStyles } from '@/theme/theme-context';

interface MicButtonProps {
  readonly recording: boolean;
  readonly disabled?: boolean;
  readonly onPress: () => void;
}

export function MicButton({ recording, disabled, onPress }: MicButtonProps) {
  const styles = useStyles(makeStyles);
  // Valor animado e interpolações são criados uma única vez, na inicialização: nada de ref lida durante o render.
  const [anim] = useState(() => {
    const pulse = new Animated.Value(0);
    return {
      pulse,
      ringScale: pulse.interpolate({ inputRange: [0, 1], outputRange: [1, 1.45] }),
      ringOpacity: pulse.interpolate({ inputRange: [0, 1], outputRange: [0.45, 0] }),
    };
  });
  const { pulse, ringScale, ringOpacity } = anim;

  useEffect(() => {
    if (!recording) {
      pulse.setValue(0);
      return;
    }
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(pulse, { toValue: 1, duration: 900, useNativeDriver: true }),
        Animated.timing(pulse, { toValue: 0, duration: 900, useNativeDriver: true }),
      ]),
    );
    loop.start();
    return () => loop.stop();
  }, [recording, pulse]);

  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      accessibilityRole="button"
      accessibilityLabel={recording ? 'Parar gravação' : 'Gravar comando por voz'}
      style={({ pressed }) => [styles.wrap, pressed && { transform: [{ scale: 0.96 }] }, disabled && { opacity: 0.5 }]}
    >
      {recording ? (
        <Animated.View style={[styles.ring, { transform: [{ scale: ringScale }], opacity: ringOpacity }]} />
      ) : null}
      <View style={[styles.button, recording && styles.buttonRecording]}>
        <Ionicons name={recording ? 'stop' : 'mic'} size={26} color="#FFFFFF" />
      </View>
      <Text style={styles.label}>{recording ? 'Toque para enviar' : 'Segure o botão ou toque para falar'}</Text>
    </Pressable>
  );
}

const makeStyles = (palette: Palette) =>
  StyleSheet.create({
    wrap: { alignItems: 'center', gap: 8 },
    ring: {
      position: 'absolute',
      top: 0,
      width: 64,
      height: 64,
      borderRadius: 32,
      backgroundColor: palette.accent,
    },
    button: {
      width: 64,
      height: 64,
      borderRadius: 32,
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: gradients.brand[0],
      shadowColor: palette.primary,
      shadowOpacity: 0.5,
      shadowRadius: 16,
      shadowOffset: { width: 0, height: 6 },
      elevation: 8,
    },
    buttonRecording: { backgroundColor: palette.expense },
    label: { color: palette.textMuted, fontSize: 12 },
  });
