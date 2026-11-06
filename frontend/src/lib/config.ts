import { Platform } from 'react-native';

const LOCAL_URL = 'http://localhost:8080';

/**
 * URL do backend, definida em `mobile/.env` como EXPO_PUBLIC_API_URL. Em desenvolvimento:
 * - Web e emulador iOS: http://localhost:8080 (o padrão, quando a variável está ausente ou vazia)
 * - Celular físico (Expo Go): "localhost" é o próprio celular, então use o endereço público do túnel
 *   (ex.: https://xxxx.trycloudflare.com) ou o IP da sua máquina na rede local (ex.: http://192.168.0.10:8080)
 */
export const API_URL: string = process.env.EXPO_PUBLIC_API_URL?.trim().replace(/\/+$/, '') || LOCAL_URL;

if (__DEV__ && Platform.OS !== 'web' && API_URL === LOCAL_URL) {
  console.warn(
    '[config] EXPO_PUBLIC_API_URL não definido: usando http://localhost:8080. Em um celular físico isso não ' +
      'alcança o backend; defina a URL do túnel ou o IP da máquina em mobile/.env e reinicie o Expo com `-c`.',
  );
}
