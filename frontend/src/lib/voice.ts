import * as Speech from 'expo-speech';

/**
 * O expo-speech não informa o gênero das vozes (só nome, identificador, idioma e qualidade), vozes estão sendo escolhidas pelo nome.
 */
const MALE_VOICE_PREFERENCE = ['felipe', 'antonio', 'daniel'];

let maleVoice: Promise<string | undefined> | null = null;

const isEnhanced = (voice: Speech.Voice) => voice.quality === Speech.VoiceQuality.Enhanced;

/** Procura uma vez e guarda o resultado: as vozes instaladas não mudam enquanto o app está aberto. */
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

/** Adianta a busca da voz para a primeira fala não esperar por ela. */
export function warmUpVoice(): void {
  void findMaleVoice();
}

export async function speakAloud(text: string): Promise<void> {
  const voice = await findMaleVoice();
  Speech.speak(text, { language: 'pt-BR', ...(voice ? { voice } : {}) });
}
