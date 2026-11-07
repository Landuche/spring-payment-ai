import { useEffect, useRef, useState } from 'react';
import { prefsStorage } from '@/lib/prefs-storage';
import { toSpeech } from '@/lib/speech-text';
import { speakAloud, stopSpeaking, warmUpVoice } from '@/lib/voice';

const VOICE_PREF_KEY = 'claudinho.voice';

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
    return stopSpeaking;
  }, []);

  const toggle = () => {
    const next = !enabled;
    setEnabled(next);
    enabledRef.current = next;
    void prefsStorage.write(VOICE_PREF_KEY, next ? 'on' : 'off');
    if (!next) stopSpeaking();
  };

  const speak = (reply: string) => {
    if (!enabledRef.current) return;
    stopSpeaking();
    void speakAloud(toSpeech(reply));
  };

  return { enabled, toggle, speak };
}
