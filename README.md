[![Java 25](https://img.shields.io/badge/Java_25-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot 4](https://img.shields.io/badge/Spring_Boot_4-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI 2](https://img.shields.io/badge/Spring_AI_2-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://spring.io/projects/spring-ai)
[![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL_16-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Kafka](https://img.shields.io/badge/Kafka_3.9-231F20?style=flat-square&logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![Expo 58](https://img.shields.io/badge/Expo_58-000020?style=flat-square&logo=expo&logoColor=white)](https://expo.dev/)
[![Docker](https://img.shields.io/badge/Docker_Compose-2496ED?style=flat-square&logo=docker&logoColor=white)](https://docs.docker.com/compose/)

# Spring Payment AI

Feito por Daniel Landuche

Aplicativo de finanças pessoais com transferências entre contas e um assistente de IA, o Claudinho, que registra gastos, consulta o saldo, gera relatórios, simula investimentos e prepara transferências por texto ou por voz. O backend é Spring Boot com Spring AI, o aplicativo é Expo (celular e web). Projeto do bootcamp Itaú - Java com Inteligência Artificial da DIO, desafio
Desenvolvendo sua API Inteligente com Reconhecimento de Fala e Spring Boot.

## Como executar

### Requisitos 

- Docker Compose 
- Node.js 22.13+ 

### Backend

```bash
cd backend
cp .env.example .env
```

Três variáveis são obrigatórias no `.env`:

| Variável | Descrição |
| --- | --- |
| `DATABASE_USER`, `DATABASE_PASSWORD` | Credenciais do Postgres |
| `AI_API_KEY` | Chave do provedor de IA (por padrão, a OpenAI) |

As demais têm valor padrão mas podem ser alteradas:

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `AI_BASE_URL` | `https://api.openai.com` | Provedor de IA: a OpenAI ou `https://api.groq.com/openai` |
| `AI_SPEECH` | `true` | Voz da IA ou do dispositivo, veja [Claudinho](#claudinho) |
| `JWT_SECRET` | chave aleatória | Secret dos tokens (mínimo de 32 bytes). |

```bash
docker compose up -d --build
```

- API em `http://localhost:8080`. O Postgres fica na porta `5433` do host e o Kafka em `29092`.
- O serviço `kokoro` só é usado com o Groq. Com a OpenAI, ou com `AI_SPEECH=false`, dá para economizar memória com `docker compose stop kokoro`.

### Aplicativo

```bash
cd frontend
cp .env.example .env
npm install
npx expo start
```

- **Web e emulador:** `EXPO_PUBLIC_API_URL=http://localhost:8080`.
- **Celular físico (Expo Go):** Crie um túnel (por exemplo `cloudflared tunnel --url http://localhost:8080`) e coloque a URL gerada em `EXPO_PUBLIC_API_URL`.
- Testado em Galaxy S9, iPhone X, iPhone 11 Pro Max, iPhone 15 Pro Max e no Chrome (Windows).

### Testes

```bash
cd backend
./gradlew test       # exige Docker rodando
cd ../frontend
npm run typecheck    # checagem de tipos
npm run lint         # ESLint (regras do Expo)
```

## Funcionalidades

- Cadastro e login. Cada cliente tem uma conta com número de 10 dígitos.
- Saldo, extrato e lançamento por categoria.
- Relatório de despesas por mês e por categoria.
- Simulação de investimento com juros compostos (0,8% ao mês por padrão).
- Transferência por número da conta ou por e-mail, com confirmação antes de executar.
- Notificações e atualização em tempo real.
- O Claudinho faz tudo conversando, por texto ou por voz.

## Claudinho

O assistente usa o `ChatClient` do Spring AI com ferramentas (`@Tool`) que chamam os mesmos casos de uso das telas: `createTransaction`, `getBalance`, `generateExpenseReport`, `simulateInvestment` e `prepareTransfer`.

- **A IA nunca move dinheiro sozinha.** `prepareTransfer` só monta uma proposta. O cliente confirma tocando no cartão, e quem executa é o endpoint `/api/transfers`, o mesmo das telas. O resultado volta ao Claudinho por `/assistant/event`, para ele responder de acordo.
- **Voz de entrada.** O áudio gravado vai para `/assistant/voice`, é transcrito (Whisper) e segue o mesmo fluxo do texto.
- **Voz de saída.** Controlada pela variável `AI_SPEECH`:

| `AI_SPEECH` | Provedor | Voz do Claudinho |
| --- | --- | --- |
| `true` (padrão) | OpenAI | OpenAI (`gpt-4o-mini-tts`, voz `onyx`) |
| `true` (padrão) | Groq | Kokoro, servidor local do compose (voz `pm_alex`) |
| `false` | qualquer | voz do dispositivo (`expo-speech`) |

  O Groq não tem voz em português, por isso o Kokoro, que expõe a mesma API da OpenAI. Se a voz da IA falhar, o aplicativo lê a resposta com a voz do dispositivo.
- **Provedor.** Só `AI_BASE_URL` e `AI_API_KEY` vêm do ambiente. Os modelos de chat, de transcrição e de voz são escolhidos pela URL em `AiProviderDefaults`, e qualquer propriedade definida explicitamente tem precedência.
- **Limite do provedor.** Em caso de 429, o Spring AI repete até 3 vezes com espera crescente. Outros erros não são repetidos, porque um 400 (áudio inválido, por exemplo) não pode ser corrigido automaticamente.

## Arquitetura

O backend é organizado por funcionalidade e, dentro de cada uma, em três camadas:

```text
backend/src/main/java/dio/budgeting
  ├─ account/       Conta, busca de destinatário e proteção contra varredura
  ├─ assistant/     Claudinho: chat, ferramentas e voz (único módulo que acessa o Spring AI)
  ├─ audit/         Registro de eventos de segurança
  ├─ auth/          Cadastro, login, JWT e refresh token
  ├─ cleanup/       Limpeza periódica de registros vencidos
  ├─ investment/    Simulação de investimento
  ├─ notification/  Avisos de transferência e envio por WebSocket
  ├─ outbox/        Publicação de eventos no Kafka
  ├─ shared/        Tratamento de erros e tipos comuns
  ├─ transaction/   Lançamentos, saldo e relatórios
  └─ transfer/      Transferência entre contas
       (cada módulo: domain/ · application/ · infrastructure/)
backend/src/main/resources
  ├─ db/migration/  Esquema do banco (Flyway)
  └─ prompts/       Instruções do Claudinho
frontend/src
  ├─ app/           Telas (Expo Router)
  ├─ components/    Componentes visuais
  ├─ hooks/         Estado das telas
  ├─ lib/           API, sessão, voz e WebSocket
  └─ theme/
```

O `LayerRulesTest` garante as dependências entre camadas: o domínio não depende de nada, e o código do Spring AI fica só em `assistant`.

### Fluxo de uma transferência

```text
POST /api/transfers
  └─ na mesma transação: débito, crédito, lançamentos e evento no outbox
        └─ publicador do outbox (a cada 1 s) ──▶ Kafka (transfer.completed)
              └─ consumidor cria os avisos (idempotente) e grava um novo evento no outbox
                    └─ Kafka (notification.created) ──▶ WebSocket ──▶ aplicativo
                          └─ o aplicativo recarrega aviso, saldo e extrato
```

1. A transferência e o evento são gravados no mesmo commit. Se o Kafka estiver fora do ar, a transferência funciona e o evento sai quando ele voltar.
2. O publicador envia os eventos pendentes ao Kafka. A busca usa `for update skip locked`, então duas instâncias do servidor nunca publicam o mesmo evento.
3. O consumidor cria um aviso para cada conta envolvida. Mensagens que falham vão para os tópicos `.DLT`.
4. Cada instância do servidor tem o próprio grupo de consumo, para que todas recebam o sinal. Quem tem o WebSocket aberto recebe `{"type":"notifications-changed"}`.
5. O aplicativo busca o que mudou pela API, já autenticada.

### WebSocket

O aplicativo conecta em `/ws/notifications` sem credenciais, e a primeira mensagem é `{"type":"auth","token":...}`. O servidor responde `{"type":"ready"}`. Sem autenticar em 10 s, ou com token vencido, a conexão é fechada (códigos `4401` e `4402`) e o aplicativo reconecta com um token renovado. Uma rotina a cada 30 s fecha as conexões de token vencido e envia pings.

## Decisões

- **Outbox transacional em vez de publicar direto no Kafka.** Publicar dentro da transação criaria dois estados possíveis (dinheiro movido sem evento, ou evento sem dinheiro movido). Gravar o evento no mesmo commit elimina isso, ao custo de um pequeno delay.
- **Consumidor idempotente.** Como o Kafka opera com entrega at-least-once, o consumidor utiliza restrições de chave única no banco de dados (ON CONFLICT DO NOTHING) para absorver reentregas seguras sem duplicar notificações.
- **O WebSocket só avisa, não carrega dados.** O servidor envia apenas o sinal e o aplicativo busca os dados pela API, que já esta autenticada. Assim não existe uma segunda via de dados para proteger, e o formato do aviso não fica duplicado.
- **Um grupo de consumo por instância.** Com um grupo único, só uma instância receberia cada sinal, e o usuário conectado em outra não seria avisado. O grupo usa `auto.offset.reset=latest`, porque o sinal só importa para quem está conectado agora.
- **Origens do WebSocket liberadas (`*`).** O Expo Go conecta com a origem do túnel público, que muda a cada execução. Isso é aceitável aqui porque a autenticação é por token na primeira mensagem, sem cookies.
- **Limpeza do outbox.** A rotina periódica apaga os eventos já publicados há mais de 7 dias; os pendentes nunca são apagados.
- **Bloqueio síncrono controlado no outbox.** A thread aguarda o ACK do Kafka por até 10s. O uso de FOR UPDATE SKIP LOCKED impede que instâncias concorrentes disputem os mesmos registros, limitando o impacto de eventuais lentidões do broker apenas ao lote em processamento.
- **Contrato do evento compartilhado.** O módulo `notification` importa `TransferCompleted` do módulo `transfer`. É uma escolha consciente do monólito modular; em microsserviços, cada consumidor teria o próprio DTO.
- **A IA só propõe, o cliente confirma.** Uma transferência que a IA executasse sozinha dependeria de ela interpretar corretamente valor e destinatário. Com a confirmação no cartão, o pior erro dela vira uma proposta recusada.
- **Spring AI isolado em um módulo.** Só `assistant` conhece o framework, e as ferramentas chamam os casos de uso. Trocar o provedor, ou o próprio framework, não toca nas regras de negócio.
- **Voz de saída.** `AI_SPEECH` decide (voz da IA ou do dispositivo) e o aplicativo verifica o que o servidor oferece em `/assistant/capabilities`.
- **Saldo calculado na leitura.** O saldo é a soma dos lançamentos da conta, carregados em memória a cada consulta e a cada transferência. Mantém o saldo sempre consistente e sem coluna duplicada, mas custa mais conforme o extrato cresce; o próximo passo seria uma soma no banco (ou um saldo materializado, atualizado sob a mesma trava da conta).
- **Memória do Claudinho em processo.** A conversa (últimas 20 mensagens por conta e conversa) fica na memória da JVM: reiniciar o servidor zera o contexto e o consumo cresce com o número de conversas. Para produção, o repositório de memória do Spring AI pode ser trocado por um persistente (JDBC).
- **Consistência eventual nos avisos.** O saldo e o extrato são sempre consistentes (mesma transação); só o aviso em tempo real chega depois.

## Segurança

- **Senhas** com BCrypt.
- **Sessão:** access token JWT (HS256) de 15 minutos e refresh token de 30 dias, com renovação no aplicativo e no WebSocket. Na web o refresh vai em cookie; no celular fica no armazenamento seguro.
- **Login:** 5 senhas erradas em 15 minutos bloqueiam o e-mail por 15 minutos.
- **Busca de destinatário:** 10 buscas sem transferência concluída bloqueiam a conta por 1 hora, para impedir a varredura de contas e e-mails.
- **Transferência idempotente:** repetir a `Idempotency-Key` devolve o resultado original, sem debitar de novo. O aplicativo só repete automaticamente requisições que são seguras de repetir.
- **Privacidade nos avisos:** os nomes de outras pessoas aparecem mascarados ("Bruno L.").
- **Auditoria:** eventos de segurança (login, bloqueios, transferências) ficam registrados em `audit_event`.
- **Limitação:** `/assistant/speech` exige login, mas não tem limite de uso próprio. Com a OpenAI, cada chamada tem custo.
- **Limitação:** o WebSocket aceita conexões anônimas por até 10 s antes da autenticação, sem limite de conexões pendentes. O fechamento é feito pela rotina de 30 s, então uma conexão sem autenticar pode durar até cerca de 40 s.
- **Limitação:** o app para iOS permite HTTP sem criptografia (`NSAllowsArbitraryLoads`), para o desenvolvimento com o IP da máquina. Em produção a API ficaria atrás de HTTPS, e essa exceção seria removida.
- **Limitação:** não há notificação push com o aplicativo fechado. O aviso em tempo real só chega com o aplicativo aberto; ao abrir, ele busca pela API.

## Testes

São 186 testes no backend, todos passando. Sete deles sobem Postgres e Kafka reais com Testcontainers, então o Docker precisa estar rodando. Os demais rodam em memória, sem Docker nem chamadas à IA.

- **Transferência e lançamentos (58):** regras de negócio, idempotência, saldo, relatórios e controladores.
- **Autenticação e contas (36):** cadastro, login, bloqueio por tentativas, refresh token, proteção contra varredura de contas e auditoria.
- **Avisos e WebSocket (37):** criação idempotente dos avisos, registro de conexões, protocolo do WebSocket e um cliente WebSocket real recebendo o sinal, inclusive com a origem de um túnel.
- **Outbox e limpeza (10):** publicação, falha do broker e remoção de registros vencidos.
- **Claudinho (33):** ferramentas, escolha do provedor, endpoint de voz (ligado e desligado) e tratamento de erros da IA.
- **Arquitetura e demais (12):** `LayerRulesTest` valida as dependências entre camadas; o restante cobre investimento, valores monetários e o carregamento do contexto.

## Stack

- Java 25
- Spring Boot 4.0 (MVC, Data JPA, Security, Validation, WebSocket)
- Spring AI 2.0
- Spring Kafka
- PostgreSQL 16
- Flyway
- Apache Kafka 3.9 (modo KRaft, sem Zookeeper)
- Kokoro (voz local, pt-BR)
- JUnit 5
- Mockito
- Testcontainers
- Expo 58
- React Native 0.88
- Expo Router
- TypeScript
- Docker Compose

## Rotas

| Método | Rota | Descrição |
| --- | --- | --- |
| POST | `/auth/register`, `/auth/login` | Cadastro e login |
| POST | `/auth/refresh`, `/auth/logout` | Renova e encerra a sessão |
| GET | `/api/account` | Dados da própria conta |
| GET | `/api/accounts/lookup` | Busca o destinatário por número ou e-mail |
| GET | `/api/balance` | Saldo, receitas e despesas |
| GET, POST | `/transactions` | Extrato e novo lançamento |
| GET | `/api/reports/expenses` | Relatório de despesas por mês e categoria |
| POST | `/api/simulations` | Simulação de investimento |
| POST | `/api/transfers` | Transferência (exige `Idempotency-Key`) |
| GET | `/api/notifications` | Avisos e quantidade de não lidos |
| POST | `/api/notifications/read` | Marca os avisos como lidos |
| POST | `/assistant/text` | Mensagem de texto ao Claudinho |
| POST | `/assistant/voice` | Áudio gravado ao Claudinho |
| POST | `/assistant/event` | Resultado de uma transferência proposta |
| GET | `/assistant/capabilities` | O que o servidor oferece (`aiSpeech`) |
| POST | `/assistant/speech` | Áudio (mp3) da resposta, na voz do Claudinho |
| WS | `/ws/notifications` | Avisos em tempo real |

Todas, menos `/auth/**` e `/ws/notifications` (que autentica na primeira mensagem), exigem `Authorization: Bearer <token>`.
