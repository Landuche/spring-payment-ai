/**
 * Converte a resposta do assistente (Markdown pensado para a tela) em texto próprio para a voz do dispositivo.
 * Tudo é determinístico e local: não depende de API. A tela continua exibindo a resposta original.
 */

const MONTHS = [
  'janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho',
  'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro',
];

const DIGIT_WORDS = ['zero', 'um', 'dois', 'três', 'quatro', 'cinco', 'seis', 'sete', 'oito', 'nove'];

const spellDigits = (digits: string): string =>
  [...digits.replace(/\D/g, '')].map((digit) => DIGIT_WORDS[Number(digit)]).join(', ');

const DASH = '[-\\u2010-\\u2015\\u2212]';

const ACCOUNT_NUMBER = new RegExp(`\\b\\d{4}(?:\\s*${DASH}\\s*|\\s)\\d{6}\\b|\\b\\d{10}\\b`, 'g');

const NUMBER_AFTER_ACCOUNT = new RegExp(`\\b(conta\\s+(?:n[úu]mero\\s+)?)(\\d+(?:\\s*${DASH}\\s*\\d+)*)\\b`, 'gi');

function speakAccountNumbers(text: string): string {
  return text
    .replace(ACCOUNT_NUMBER, (match) => spellDigits(match))
    .replace(NUMBER_AFTER_ACCOUNT, (_match, prefix: string, digits: string) => `${prefix}${spellDigits(digits)}`);
}

function speakCurrency(text: string): string {
  return text.replace(/R\$\s*(\d{1,3}(?:\.\d{3})*|\d+)(?:,(\d{1,2}))?/g, (_match, whole: string, centsPart?: string) => {
    const reais = Number(whole.replace(/\./g, ''));
    const cents = centsPart ? Number(centsPart.padEnd(2, '0')) : 0;
    const reaisText = `${reais} ${reais === 1 ? 'real' : 'reais'}`;
    if (cents === 0) return reaisText;
    const centsText = `${cents} ${cents === 1 ? 'centavo' : 'centavos'}`;
    return reais === 0 ? centsText : `${reaisText} e ${centsText}`;
  });
}

function speakDates(text: string): string {
  return text.replace(/\b(\d{1,2})\/(\d{1,2})(?:\/(\d{4}))?\b/g, (match, day: string, month: string, year?: string) => {
    const monthIndex = Number(month) - 1;
    if (monthIndex < 0 || monthIndex > 11 || Number(day) < 1 || Number(day) > 31) return match;
    return `${Number(day)} de ${MONTHS[monthIndex]}${year ? ` de ${year}` : ''}`;
  });
}

function endWithPause(line: string): string {
  return /[.!?:;,…]$/.test(line) ? line : `${line}.`;
}

const LIST_TABLE_OR_HEADING = /^\s*(?:[-*•]\s+\S|\d+[.)]\s|\||#{1,6}\s)/;
const DETAILS_ON_SCREEN = 'Os detalhes estão na tela.';

export function toSpeech(markdown: string): string {
  const lines = markdown.split('\n');
  const dataLines = lines.filter((line) => LIST_TABLE_OR_HEADING.test(line));
  if (dataLines.length < 2) return plainSpeech(markdown);

  const prose = lines.filter((line) => !LIST_TABLE_OR_HEADING.test(line));
  return plainSpeech(prose.join('\n')) || DETAILS_ON_SCREEN;
}

function plainSpeech(markdown: string): string {
  const lines = markdown
    .replace(/\r\n?/g, '\n')
    .replace(/```[\s\S]*?```/g, ' ') // blocos de código não fazem sentido falados
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1') // links: fala só o texto
    .replace(/[\p{Extended_Pictographic}️‍]/gu, '') // emojis
    .split('\n')
    .filter((line) => !/^[\s|:-]*-[\s|:-]*$/.test(line)) // separador de tabela (|---|---|) e linhas "---"
    .map((line) =>
      line
        .replace(/^\s*#{1,6}\s*/, '') // títulos
        .replace(/^\s*(?:[-*•]|\d+[.)])\s+/, '') // marcadores de lista
        .replace(/(\*\*|__|[*_`~])/g, '') // negrito, itálico, código
        .replace(/^\||\|$/g, '') // bordas de tabela
        .replace(/\s*\|\s*/g, ': ') // células de tabela, no pior caso
        .trim(),
    )
    .filter(Boolean)
    .map(endWithPause);

  const joined = lines.join(' ').replace(/\s{2,}/g, ' ');
  return speakDates(speakCurrency(speakAccountNumbers(joined))).replace(/(\d)\s*%/g, '$1 por cento').trim();
}
