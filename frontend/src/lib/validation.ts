const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

export function validateEmail(value: string): string | undefined {
  if (!value.trim()) return 'Informe seu e-mail';
  if (!EMAIL.test(value.trim())) return 'E-mail inválido';
  return undefined;
}

export function validatePassword(value: string): string | undefined {
  if (!value) return 'Informe sua senha';
  if (value.length < 8) return 'A senha deve ter pelo menos 8 caracteres';
  if (value.length > 72) return 'A senha deve ter no máximo 72 caracteres';
  return undefined;
}

export function validateName(value: string): string | undefined {
  const trimmed = value.trim();
  if (trimmed.length < 2) return 'Informe seu nome';
  if (trimmed.length > 120) return 'Nome muito longo';
  return undefined;
}
