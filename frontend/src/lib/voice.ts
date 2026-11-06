import * as Speech from 'expo-speech';

/**
 * O expo-speech não informa o gênero das vozes (só nome, identificador, idioma e qualidade), então a voz masculina
 * é escolhida por nome. Só entram vozes de boa qualidade: no iOS, o Felipe (precisa estar baixado); no navegador,
 * Microsoft Antonio e Daniel. Eddy, Rocko, Grandpa e Reed ficam de fora: já vêm instaladas no iOS, mas soam artificiais.
 * Sem nenhuma delas, vale a voz padrão do aparelho, sem alterar o tom (baixar o pitch de uma voz feminina soa abafado).
 */
const MALE_VOICE_PREFERENCE = ['felipe', 'antonio', 'daniel'];

let maleVoice: Promise<string | undefined> | null = null;

const isEnhanced = (voice: Speech.Voice) => voice.quality === Speech.VoiceQuality.Enhanced;

/** Procura uma vez e guarda o resultado: as vozes instaladas não mudam enquanto o app está aberto. */
function findMaleVoice(): Promise<string | undefined> {
  maleVoice ??= Speech.getAvailableVoicesAsync()
    .then((voices) => {
      const portuguese = voices.filter((voice) => voice.language.replace('_', '-').toLowerCase() === 'pt-br');
      if (__DEV__) {
        console.log('Vozes pt-BR no aparelho:', portuguese.map((v) => `${v.name} [${v.quality}] ${v.identifier}`));
      }

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

/** Fala o texto em português do Brasil, com a voz masculina quando o aparelho tiver uma. */
export async function speakAloud(text: string): Promise<void> {
  const voice = await findMaleVoice();
  Speech.speak(text, { language: 'pt-BR', ...(voice ? { voice } : {}) });
}
