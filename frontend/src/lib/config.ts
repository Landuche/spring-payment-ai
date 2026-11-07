const LOCAL_URL = 'http://localhost:8080';

/**
 * URL do backend, definida em `frontend/.env` como EXPO_PUBLIC_API_URL. Em desenvolvimento:
 * - Web e emulador iOS: http://localhost:8080
 * - Celular (Expo Go): use o endereço público (tipo cloudflare)
 */
export const API_URL: string = process.env.EXPO_PUBLIC_API_URL?.trim().replace(/\/+$/, '') || LOCAL_URL;
