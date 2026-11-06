import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

/**
 * Armazenamento do token de sessão.
 * - Nativo: Keychain (iOS) / Keystore (Android) via expo-secure-store, criptografado pelo sistema.
 * - Web: localStorage. É o melhor que um SPA consegue sem cookies httpOnly do servidor.
 *   Em produção web, o padrão bancário é cookie httpOnly + SameSite=Strict emitido pelo backend.
 *
 * Todas as operações são tolerantes a falha (modo privado, storage bloqueado): nunca derrubam o app.
 */
const KEY = 'fintech.session.v1';

export const sessionStorage = {
  async read(): Promise<string | null> {
    try {
      if (Platform.OS === 'web') {
        return globalThis.localStorage?.getItem(KEY) ?? null;
      }
      return await SecureStore.getItemAsync(KEY);
    } catch {
      return null;
    }
  },

  async write(value: string): Promise<void> {
    try {
      if (Platform.OS === 'web') {
        globalThis.localStorage?.setItem(KEY, value);
        return;
      }
      await SecureStore.setItemAsync(KEY, value, {
        keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY,
      });
    } catch {
      // Sem persistência: a sessão vale só enquanto o app estiver aberto.
    }
  },

  async clear(): Promise<void> {
    try {
      if (Platform.OS === 'web') {
        globalThis.localStorage?.removeItem(KEY);
        return;
      }
      await SecureStore.deleteItemAsync(KEY);
    } catch {
      // Nada a limpar.
    }
  },
};
