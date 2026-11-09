import * as Speech from 'expo-speech';
import { isAiSpeechAvailable, playAiSpeech, stopAiSpeech } from './ai-speech';

/**
 * O expo-speech não informa o gênero das vozes (só nome, identificador, idioma e qualidade), então a voz masculina
 * é escolhida pelo nome. É a voz de reserva, usada quando a voz da IA não está disponível.
 */
const MALE_VOICE_PREFERENCE = ['felipe', 'antonio', 'daniel'];

let maleVoice: Promise<string | undefined> | null = null;

const isEnhanced = (voice: Speech.Voice) => voice.quality === Speech.VoiceQuality.Enhanced;

/** Procura uma vez e guarda o resultado. */
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

async function speakWithDevice(text: string): Promise<void> {
  const voice = await findMaleVoice();
  Speech.speak(text, { language: 'pt-BR', ...(voice ? { voice } : {}) });
}

// Cada fala ganha um número. Se o número mudou quando uma espera termina, outra fala (ou um "silenciar") passou na frente.
let currentSpeech = 0;

/** Fala com a voz da IA quando o servidor oferece. */
export async function speakAloud(text: string): Promise<void> {
  const mine = ++currentSpeech;
  const stillWanted = () => mine === currentSpeech;
  if (await isAiSpeechAvailable()) {
    try {
      await playAiSpeech(text, stillWanted);
      return;
    } catch {
      // Sem o áudio da IA nesta fala: segue para a voz do aparelho.
    }
  }
  if (stillWanted()) await speakWithDevice(text);
}

export function stopSpeaking(): void {
  currentSpeech += 1;
  stopAiSpeech();
  void Speech.stop();
}
