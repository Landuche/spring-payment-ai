import * as Speech from 'expo-speech';

const MALE_VOICE_PREFERENCE = ['felipe', 'antonio', 'daniel'];

let maleVoice: Promise<string | undefined> | null = null;

const isEnhanced = (voice: Speech.Voice) => voice.quality === Speech.VoiceQuality.Enhanced;

function findMaleVoice(): Promise<string | undefined> {
  maleVoice ??= Speech.getAvailableVoicesAsync()
    .then((voices) => {
      const portuguese = voices.filter((voice) => voice.language.replace('_', '-').toLowerCase() === 'pt-br');
      for (const name of MALE_VOICE_PREFERENCE) {
        const matches = portuguese
          .filter((voice) => `${voice.name} ${voice.identifier}`.toLowerCase().includes(name))
          .sort((a, b) => Number(isEnhanced(b)) - Number(isEnhanced(a)));
        if (matches.length > 0) return matches[0].identifier;
      }
      return undefined;
    })
    .catch(() => undefined);
  return maleVoice;
}

export function warmUpVoice(): void {
  void findMaleVoice();
}

export async function speakAloud(text: string): Promise<void> {
  const voice = await findMaleVoice();
  Speech.speak(text, { language: 'pt-BR', ...(voice ? { voice } : {}) });
}
