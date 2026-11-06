# Fintech Agent — app (Expo + Expo Router)

Um único código-base para **web (desktop)**, **iOS** e **Android**. Consome o backend Spring Boot na pasta `../backend`.

## Telas

- **Início:** cartão de saldo em gradiente, gastos do mês por categoria e extrato.
- **Assistente:** chat com a IA por texto ou voz (microfone). Mostra a transcrição do áudio e a resposta.
- **Investir:** simulador de juros compostos com prazos rápidos.
- **Nova transação:** modal para registrar entradas e saídas manualmente.

## Rodar

Antes, suba o backend (pasta `backend`) na porta 8080. O provedor de IA (Groq ou OpenAI) é escolhido no `.env` do backend.

```bash
npm install
npm run web        # navegador (desktop ou mobile responsivo)
npm run android    # emulador/dispositivo Android
npm run ios        # macOS
```

## Configurar a URL da API

Crie `mobile/.env`:

```bash
# Web e emulador: padrão localhost
EXPO_PUBLIC_API_URL=http://localhost:8080

# Celular físico (Expo Go) na mesma rede: use o IP da máquina
# EXPO_PUBLIC_API_URL=http://192.168.0.10:8080
```

O backend já libera CORS para `localhost` e `127.0.0.1`. Para celular, o acesso é direto, sem CORS.

## Estrutura

```
src/
  app/            rotas (Expo Router): _layout, (tabs)/ e nova-transacao
  components/     componentes visuais reutilizáveis
  hooks/          useFinance (saldo, extrato, relatório)
  lib/            api (cliente tipado), types (espelho dos DTOs), format, config
  theme/          tokens de cor, espaçamento, raio e tipografia
```

## Verificação

- `npx tsc --noEmit` passa.
- `npx expo export --platform web` compila sem erros.
