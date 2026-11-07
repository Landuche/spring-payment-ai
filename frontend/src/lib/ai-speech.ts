import { createAudioPlayer, setAudioModeAsync, type AudioPlayer } from 'expo-audio';
import { File, Paths } from 'expo-file-system';
import { Platform } from 'react-native';
import { api } from './api';

let availability: Promise<boolean> | null = null;
let player: AudioPlayer | null = null;
let cleanup: (() => void) | null = null;

/** Pergunta uma vez ao servidor se a voz por IA está ligada. Se a pergunta falhar, vale a voz do aparelho e tenta de novo depois. */
export function isAiSpeechAvailable(): Promise<boolean> {
  availability ??= api
    .getCapabilities()
    .then((capabilities) => capabilities.aiSpeech)
    .catch(() => {
      availability = null;
      return false;
    });
  return availability;
}

/** Interrompe o áudio que está tocando e apaga o arquivo temporário. */
export function stopAiSpeech(): void {
  player?.remove();
  player = null;
  cleanup?.();
  cleanup = null;
}

/** Guarda o áudio onde o player consegue ler: um arquivo no cache (celular) ou um blob URL (navegador). */
async function toSource(audio: Uint8Array): Promise<{ readonly uri: string; readonly cleanup: () => void }> {
  if (Platform.OS === 'web') {
    const url = URL.createObjectURL(new Blob([audio as BlobPart], { type: 'audio/mpeg' }));
    return { uri: url, cleanup: () => URL.revokeObjectURL(url) };
  }
  const file = new File(Paths.cache, `claudinho-voz-${Date.now()}.mp3`);
  file.create({ overwrite: true });
  await file.write(audio);
  return { uri: file.uri, cleanup: () => file.delete() };
}

/**
 * Pede o áudio ao servidor e toca. `stillWanted` é conferido depois de cada espera: se a pessoa silenciou ou pediu
 * outra fala enquanto o áudio vinha, ele é descartado. Lança erro se o servidor não entregar o áudio.
 */
export async function playAiSpeech(text: string, stillWanted: () => boolean): Promise<void> {
  const audio = new Uint8Array(await api.synthesizeSpeech(text));
  if (!stillWanted()) return;
  const source = await toSource(audio);
  if (!stillWanted()) {
    source.cleanup();
    return;
  }
  stopAiSpeech();
  // Depois de gravar, o iOS manda o som para o fone de ouvido (a "conversa"), baixo; e o modo silencioso o cala.
  if (Platform.OS === 'ios') {
    await setAudioModeAsync({ allowsRecording: false, playsInSilentMode: true });
  }
  const next = createAudioPlayer(source.uri);
  next.addListener('playbackStatusUpdate', (status) => {
    if (status.didJustFinish && player === next) stopAiSpeech();
  });
  player = next;
  cleanup = source.cleanup;
  next.play();
}
