import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

/**
 * Preferências do usuário (tema, notificações vistas). Não são segredos.
 * Mesmo padrão do session-storage: localStorage na web, SecureStore no nativo (sem dependência nova).
 * Todas as operações são tolerantes a falha: preferência perdida nunca derruba o app.
 */
export const prefsStorage = {
  async read(key: string): Promise<string | null> {
    try {
      if (Platform.OS === 'web') {
        return globalThis.localStorage?.getItem(key) ?? null;
      }
      return await SecureStore.getItemAsync(key);
    } catch {
      return null;
    }
  },

  async write(key: string, value: string): Promise<void> {
    try {
      if (Platform.OS === 'web') {
        globalThis.localStorage?.setItem(key, value);
        return;
      }
      await SecureStore.setItemAsync(key, value);
    } catch {
      // Sem persistência: a preferência vale até o app fechar.
    }
  },
};
