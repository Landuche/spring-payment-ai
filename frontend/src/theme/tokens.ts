/**
 * Tokens visuais. Estilo neobank: superfícies em camadas, gradientes vibrantes e cores semânticas para
 * entrada/saída. Há duas paletas com as mesmas chaves; a escolhida vem do ThemeProvider.
 */
export const darkPalette = {
  background: '#07070D',
  surface: '#12121C',
  surfaceRaised: '#1A1A28',
  border: '#262638',
  text: '#F4F4FA',
  textMuted: '#8C8CA8',
  primary: '#8B5CF6',
  primaryStrong: '#7C3AED',
  accent: '#EC4899',
  income: '#34D399',
  expense: '#FB7185',
  warning: '#FBBF24',
} as const;

export type Palette = { readonly [K in keyof typeof darkPalette]: string };

export const lightPalette: Palette = {
  background: '#F4F5FA',
  surface: '#FFFFFF',
  surfaceRaised: '#EEF0F7',
  border: '#DCDFEA',
  text: '#14142B',
  textMuted: '#62647A',
  primary: '#7C3AED',
  primaryStrong: '#6D28D9',
  accent: '#DB2777',
  income: '#059669',
  expense: '#E11D48',
  warning: '#D97706',
};

export const gradients = {
  brand: ['#7C3AED', '#DB2777'] as const,
  calm: ['#1E1B4B', '#312E81'] as const,
};

export const space = { xs: 4, sm: 8, md: 12, lg: 16, xl: 24, xxl: 32 } as const;

export const radius = { md: 14, lg: 20, pill: 999 } as const;

export const font = {
  display: 36,
  title: 22,
  subtitle: 16,
  body: 15,
  caption: 12,
} as const;

/** Largura máxima do conteúdo: no celular ocupa a tela; no desktop usa a largura da janela, com teto. */
export const CONTENT_MAX_WIDTH = { compact: 560, wide: 1120 } as const;

/** A partir desta largura a navegação vira barra lateral (desktop/web). */
export const WIDE_BREAKPOINT = 1100;

/** Largura da barra lateral no desktop. */
export const SIDEBAR_WIDTH = 232;
