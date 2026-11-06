/**
 * Converte a resposta do assistente (Markdown pensado para a tela) em texto próprio para a voz do dispositivo.
 * Tudo é determinístico e local: não depende de API. A tela continua exibindo a resposta original.
 * A regra de produto: a voz resume (introdução e pergunta), a tela detalha (listas e números).
 */

const MONTHS = [
  'janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho',
  'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro',
];

const DIGIT_WORDS = ['zero', 'um', 'dois', 'três', 'quatro', 'cinco', 'seis', 'sete', 'oito', 'nove'];

/** "9886" -> "nove, oito, oito, seis". As vírgulas dão a pausa entre os dígitos. */
const spellDigits = (digits: string): string =>
  [...digits.replace(/\D/g, '')].map((digit) => DIGIT_WORDS[Number(digit)]).join(', ');

/**
 * Traços que separam as duas partes do número da conta: o hífen comum, mas também o hífen não separável, os travessões
 * e o sinal de menos (U+2010 a U+2015 e U+2212). O modelo costuma escrever um desses no lugar do "-", e na tela
 * são indistinguíveis; sem cobri-los, só a primeira parte era lida dígito a dígito.
 */
const DASH = '[-\\u2010-\\u2015\\u2212]';

/** Duas partes (4 + 6 dígitos) separadas por traço ou espaço, ou 10 dígitos seguidos. */
const ACCOUNT_NUMBER = new RegExp(`\\b\\d{4}(?:\\s*${DASH}\\s*|\\s)\\d{6}\\b|\\b\\d{10}\\b`, 'g');

/** Qualquer número (com partes separadas por traço) logo depois da palavra "conta". */
const NUMBER_AFTER_CONTA = new RegExp(`\\b(conta\\s+(?:n[úu]mero\\s+)?)(\\d+(?:\\s*${DASH}\\s*\\d+)*)\\b`, 'gi');

/**
 * Número de conta é um identificador, não uma quantia: "9886" deve sair "nove, oito, oito, seis" e não "nove mil...".
 * Cobre o formato XXXX-XXXXXX (com qualquer traço), 10 dígitos soltos e qualquer número logo depois de "conta".
 */
function speakAccountNumbers(text: string): string {
  return text
    .replace(ACCOUNT_NUMBER, (match) => spellDigits(match))
    .replace(NUMBER_AFTER_CONTA, (_match, prefix: string, digits: string) => `${prefix}${spellDigits(digits)}`);
}

/** "R$ 1.234,56" -> "1234 reais e 56 centavos"; "R$ 50,00" -> "50 reais"; "R$ 1,00" -> "1 real". */
function speakCurrency(text: string): string {
  return text.replace(/R\$\s*(\d{1,3}(?:\.\d{3})*|\d+)(?:,(\d{1,2}))?/g, (_match, whole: string, cents?: string) => {
    const reais = Number(whole.replace(/\./g, ''));
    const centavos = cents ? Number(cents.padEnd(2, '0')) : 0;
    const reaisText = `${reais} ${reais === 1 ? 'real' : 'reais'}`;
    if (centavos === 0) return reaisText;
    const centavosText = `${centavos} ${centavos === 1 ? 'centavo' : 'centavos'}`;
    return reais === 0 ? centavosText : `${reaisText} e ${centavosText}`;
  });
}

/** "04/11/2026" -> "4 de novembro de 2026"; "04/11" -> "4 de novembro". Datas inválidas ficam como estão. */
function speakDates(text: string): string {
  return text.replace(/\b(\d{1,2})\/(\d{1,2})(?:\/(\d{4}))?\b/g, (match, day: string, month: string, year?: string) => {
    const monthIndex = Number(month) - 1;
    if (monthIndex < 0 || monthIndex > 11 || Number(day) < 1 || Number(day) > 31) return match;
    return `${Number(day)} de ${MONTHS[monthIndex]}${year ? ` de ${year}` : ''}`;
  });
}

/** Garante uma pausa no fim da linha para que itens de lista não virem uma frase só. */
function endWithPause(line: string): string {
  return /[.!?:;,…]$/.test(line) ? line : `${line}.`;
}

/** Linha de lista, tabela (inclui o separador |---|) ou título: dado para os olhos, não para os ouvidos. */
const LIST_TABLE_OR_HEADING = /^\s*(?:[-*•]\s+\S|\d+[.)]\s|\||#{1,6}\s)/;
const DETAILS_ON_SCREEN = 'Os detalhes estão na tela.';

/**
 * O que a voz deve ler. Respostas curtas são lidas inteiras. Quando há uma lista ou tabela (duas ou mais linhas),
 * a voz lê só as frases ao redor, como a introdução e a pergunta de fechamento, e os números ficam na tela.
 */
export function toSpeech(markdown: string): string {
  const lines = markdown.split('\n');
  const dataLines = lines.filter((line) => LIST_TABLE_OR_HEADING.test(line));
  if (dataLines.length < 2) return plainSpeech(markdown);

  const prose = lines.filter((line) => !LIST_TABLE_OR_HEADING.test(line));
  return plainSpeech(prose.join('\n')) || DETAILS_ON_SCREEN;
}

/** Limpa o Markdown e lê valores, datas e porcentagens por extenso. */
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
