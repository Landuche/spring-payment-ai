import * as Speech from 'expo-speech';
import { useEffect, useRef, useState } from 'react';
import { prefsStorage } from '@/lib/prefs-storage';
import { toSpeech } from '@/lib/speech-text';
import { speakAloud, warmUpVoice } from '@/lib/voice';

const VOICE_PREF_KEY = 'claudinho.voice';

/**
 * Leitura das respostas em voz alta com a voz do aparelho (não depende do backend nem de API paga).
 * Ligada por padrão; a escolha fica salva. Sair da tela interrompe uma fala em andamento.
 */
export function useSpokenReplies() {
  const [enabled, setEnabled] = useState(true);
  // Espelho do estado: a resposta chega depois de um await e precisa ver o valor atual, não o da época do toque.
  const enabledRef = useRef(true);

  useEffect(() => {
    void prefsStorage.read(VOICE_PREF_KEY).then((saved) => {
      if (saved === 'off') {
        setEnabled(false);
        enabledRef.current = false;
      }
    });
    warmUpVoice();
    return () => {
      void Speech.stop();
    };
  }, []);

  const toggle = () => {
    const next = !enabled;
    setEnabled(next);
    enabledRef.current = next;
    void prefsStorage.write(VOICE_PREF_KEY, next ? 'on' : 'off');
    if (!next) void Speech.stop();
  };

  const speak = (reply: string) => {
    if (!enabledRef.current) return;
    void Speech.stop();
    void speakAloud(toSpeech(reply));
  };

  return { enabled, toggle, speak };
}
