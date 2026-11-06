import { useAudioRecorder, RecordingPresets, requestRecordingPermissionsAsync, setAudioModeAsync, useAudioRecorderState } from 'expo-audio';
import { randomUUID } from 'expo-crypto';
import * as Speech from 'expo-speech';
import { useEffect, useRef, useState } from 'react';
import {
  FlatList,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { ChatBubble, type ChatMessage } from '@/components/ChatBubble';
import { MicButton } from '@/components/MicButton';
import { useToast } from '@/components/Toast';
import type { TransferCardState, TransferCardStatus } from '@/components/TransferProposalCard';
import { api, ApiError } from '@/lib/api';
import { formatBRL } from '@/lib/format';
import { prefsStorage } from '@/lib/prefs-storage';
import type { TransferEventType, TransferProposal } from '@/lib/types';
import { toSpeech } from '@/lib/speech-text';
import { speakAloud, warmUpVoice } from '@/lib/voice';
import { CONTENT_MAX_WIDTH, font, radius, SIDEBAR_WIDTH, space, type Palette } from '@/theme/tokens';
import { useIsWide, useStyles, useTheme } from '@/theme/theme-context';

const ASSISTANT_NAME = 'Claudinho';
const VOICE_PREF_KEY = 'claudinho.voice';

const GREETING: ChatMessage = {
  id: 'greeting',
  role: 'assistant',
  text: `Oi! Eu sou o ${ASSISTANT_NAME}. Posso registrar gastos, mostrar seu saldo, gerar relatórios, simular investimentos e preparar transferências. Pode falar ou digitar.`,
};

let messageCounter = 0;
const nextId = () => `msg-${++messageCounter}`;

/** A última mensagem do cliente veio de uma gravação (tem a marca "transcrição de voz")? */
function lastUserMessageWasVoice(messages: readonly ChatMessage[]): boolean {
  for (let i = messages.length - 1; i >= 0; i -= 1) {
    if (messages[i].role === 'user') return messages[i].kind === 'voice';
  }
  return false;
}

/** Cartão de confirmação para uma proposta do Claudinho (ou nada, quando ele não preparou transferência). */
const cardFor = (proposal: TransferProposal | null): TransferCardState | undefined =>
  proposal ? { proposal, status: 'pending', idempotencyKey: randomUUID() } : undefined;

export default function AssistantScreen() {
  const [messages, setMessages] = useState<ChatMessage[]>([GREETING]);
  const [draft, setDraft] = useState('');
  const [busy, setBusy] = useState(false);
  /** Se o Claudinho lê as respostas de voz em voz alta. Ligado por padrão; a escolha fica salva. */
  const [voiceOn, setVoiceOn] = useState(true);
  /** Espelho do estado: a resposta chega depois de um await e precisa ver o valor atual, não o da época do toque. */
  const voiceOnRef = useRef(true);
  /** Uma conversa por abertura da tela: o backend guarda o histórico com essa chave. */
  const [conversationId] = useState(() => randomUUID());
  const listRef = useRef<FlatList<ChatMessage>>(null);
  const { palette } = useTheme();
  const styles = useStyles(makeStyles);
  const wide = useIsWide();
  const toast = useToast();

  const recorder = useAudioRecorder(RecordingPresets.HIGH_QUALITY);
  const recorderState = useAudioRecorderState(recorder);
  const recording = recorderState.isRecording;

  const append = (message: Omit<ChatMessage, 'id'>) => {
    setMessages((prev) => [...prev, { ...message, id: nextId() }]);
    requestAnimationFrame(() => listRef.current?.scrollToEnd({ animated: true }));
  };

  const failure = (error: unknown) =>
    error instanceof ApiError ? error.message : 'Algo deu errado. Tente novamente.';

  useEffect(() => {
    void prefsStorage.read(VOICE_PREF_KEY).then((saved) => {
      if (saved === 'off') {
        setVoiceOn(false);
        voiceOnRef.current = false;
      }
    });
    warmUpVoice();
    // Sair da tela interrompe uma fala em andamento.
    return () => {
      void Speech.stop();
    };
  }, []);

  const toggleVoice = () => {
    const next = !voiceOn;
    setVoiceOn(next);
    voiceOnRef.current = next;
    void prefsStorage.write(VOICE_PREF_KEY, next ? 'on' : 'off');
    if (!next) void Speech.stop();
  };

  /** Lê a resposta em voz alta com a voz do dispositivo (não depende do backend nem de API paga). */
  const speak = (reply: string) => {
    if (!voiceOnRef.current) return;
    void Speech.stop();
    void speakAloud(toSpeech(reply));
  };

  const sendText = async () => {
    const text = draft.trim();
    if (!text || busy) return;
    setDraft('');
    append({ role: 'user', text });
    setBusy(true);
    try {
      const { reply, transferencia } = await api.sendText(text, conversationId);
      append({ role: 'assistant', text: reply, transfer: cardFor(transferencia) });
    } catch (error) {
      append({ role: 'assistant', text: failure(error) });
    } finally {
      setBusy(false);
    }
  };

  const setTransferStatus = (messageId: string, status: TransferCardStatus) =>
    setMessages((prev) =>
      prev.map((m) => (m.id === messageId && m.transfer ? { ...m, transfer: { ...m.transfer, status } } : m)),
    );

  /**
   * Quem envia o dinheiro é o toque do cliente, nunca o modelo. O pedido vai pelo mesmo endpoint da aba
   * Transferências (saldo, conta própria e idempotência validados de novo no servidor); a chave é a da proposta,
   * então repetir o toque depois de uma falha não debita duas vezes.
   */
  const confirmTransfer = async (messageId: string) => {
    const card = messages.find((m) => m.id === messageId)?.transfer;
    if (!card || card.status !== 'pending') return;
    const { proposal } = card;
    setTransferStatus(messageId, 'sending');
    try {
      await api.transfer(
        {
          destination: { numero: proposal.contaDestino },
          amount: proposal.valor,
          description: proposal.descricao ?? undefined,
        },
        card.idempotencyKey,
      );
      setTransferStatus(messageId, 'done');
      toast.success(`${formatBRL(proposal.valor)} enviados para ${proposal.nomeDestinatario}`);
      void tellClaudinho('TRANSFERENCIA_CONFIRMADA', proposal);
    } catch (error) {
      setTransferStatus(messageId, 'pending');
      toast.error(failure(error));
      void tellClaudinho('TRANSFERENCIA_FALHOU', proposal, failure(error));
    }
  };

  const cancelTransfer = (messageId: string) => {
    const card = messages.find((m) => m.id === messageId)?.transfer;
    if (!card || card.status !== 'pending') return;
    setTransferStatus(messageId, 'cancelled');
    void tellClaudinho('TRANSFERENCIA_CANCELADA', card.proposal);
  };

  /**
   * Conta ao Claudinho como terminou o cartão (ele não vê o toque), para ele responder na mesma conversa e propor o
   * próximo passo. O resultado já está no cartão e no aviso: se esta chamada falhar, a conversa só segue sem a fala dele.
   */
  const tellClaudinho = async (tipo: TransferEventType, proposal: TransferProposal, motivo?: string) => {
    // Conversa por voz: a última mensagem do cliente antes da confirmação tem a marca "transcrição de voz".
    // Se foi digitada, o Claudinho fica quieto. Em ambos os casos o toggle de voz manda (ver `speak`).
    const viaVoice = lastUserMessageWasVoice(messages);
    setBusy(true);
    try {
      const { reply, transferencia } = await api.sendTransferEvent(
        { tipo, nomeDestinatario: proposal.nomeDestinatario, valor: proposal.valor, motivo },
        conversationId,
      );
      append({ role: 'assistant', text: reply, transfer: cardFor(transferencia) });
      if (viaVoice) speak(reply);
    } catch {
      // sem a resposta do Claudinho, o cartão e o aviso já mostram o desfecho
    } finally {
      setBusy(false);
    }
  };

  const startRecording = async () => {
    const permission = await requestRecordingPermissionsAsync();
    if (!permission.granted) {
      append({ role: 'assistant', text: 'Preciso de permissão do microfone para ouvir seus comandos.' });
      return;
    }
    await setAudioModeAsync({ allowsRecording: true, playsInSilentMode: true });
    await recorder.prepareToRecordAsync();
    recorder.record();
  };

  const stopAndSend = async () => {
    await recorder.stop();
    const uri = recorder.uri;
    if (!uri) {
      append({ role: 'assistant', text: 'Não consegui capturar o áudio. Tente novamente.' });
      return;
    }
    setBusy(true);
    try {
      const { transcription, reply, transferencia } = await api.sendVoice(uri, conversationId);
      append({ role: 'user', text: transcription || '(áudio sem fala reconhecida)', kind: 'voice' });
      append({ role: 'assistant', text: reply, transfer: cardFor(transferencia) });
      speak(reply);
    } catch (error) {
      append({ role: 'assistant', text: failure(error) });
    } finally {
      setBusy(false);
    }
  };

  const onMicPress = () => {
    if (recording) {
      void stopAndSend();
    } else {
      void startRecording();
    }
  };

  const canSend = draft.trim().length > 0 && !busy;

  return (
    <SafeAreaView style={[styles.safe, wide && { paddingRight: SIDEBAR_WIDTH }]} edges={['top']}>
      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={Platform.OS === 'ios' ? 8 : 0}
      >
        <View style={[styles.column, { maxWidth: wide ? CONTENT_MAX_WIDTH.wide : CONTENT_MAX_WIDTH.compact }]}>
          <View style={styles.header}>
            <Text style={styles.title}>{ASSISTANT_NAME}</Text>
            <View style={styles.headerActions}>
              <Pressable
                onPress={toggleVoice}
                accessibilityRole="switch"
                accessibilityState={{ checked: voiceOn }}
                accessibilityLabel="Resposta em voz alta"
                hitSlop={8}
                style={[styles.voiceToggle, voiceOn && styles.voiceToggleOn]}
              >
                <Ionicons
                  name={voiceOn ? 'volume-high' : 'volume-mute'}
                  size={18}
                  color={voiceOn ? palette.primary : palette.textMuted}
                />
              </Pressable>
              <View style={styles.statusPill}>
                <View style={[styles.statusDot, { backgroundColor: busy ? palette.warning : palette.income }]} />
                <Text style={styles.statusText}>{busy ? 'pensando…' : 'online'}</Text>
              </View>
            </View>
          </View>

          <FlatList
            ref={listRef}
            data={messages}
            keyExtractor={(item) => item.id}
            renderItem={({ item }) => (
              <ChatBubble
                message={item}
                onConfirmTransfer={(id) => void confirmTransfer(id)}
                onCancelTransfer={cancelTransfer}
              />
            )}
            contentContainerStyle={styles.feed}
            onContentSizeChange={() => listRef.current?.scrollToEnd({ animated: true })}
            showsVerticalScrollIndicator={false}
          />

          <View style={styles.composer}>
            <TextInput
              value={draft}
              onChangeText={setDraft}
              placeholder="Ex.: gastei 45 reais na farmácia"
              placeholderTextColor={palette.textMuted}
              style={styles.input}
              editable={!busy && !recording}
              onSubmitEditing={sendText}
              returnKeyType="send"
              accessibilityLabel="Mensagem para o assistente"
            />
            <Pressable
              onPress={sendText}
              disabled={!canSend}
              accessibilityRole="button"
              accessibilityLabel="Enviar mensagem"
              style={[styles.send, !canSend && styles.sendDisabled]}
            >
              <Ionicons name="arrow-up" size={20} color="#FFFFFF" />
            </Pressable>
          </View>

          <View style={styles.micArea}>
            <MicButton recording={recording} disabled={busy} onPress={onMicPress} />
          </View>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const makeStyles = (palette: Palette) => StyleSheet.create({
  safe: { flex: 1, backgroundColor: palette.background, alignItems: 'center' },
  flex: { flex: 1, width: '100%', alignItems: 'center' },
  column: { flex: 1, width: '100%', paddingHorizontal: space.lg, paddingTop: space.md, gap: space.md },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  title: { color: palette.text, fontSize: font.title, fontWeight: '800' },
  headerActions: { flexDirection: 'row', alignItems: 'center', gap: space.sm },
  voiceToggle: {
    width: 32,
    height: 32,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: palette.surface,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: palette.border,
  },
  voiceToggleOn: { borderColor: palette.primary },
  statusPill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: space.md,
    paddingVertical: 4,
    borderRadius: radius.pill,
    backgroundColor: palette.surface,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: palette.border,
  },
  statusDot: { width: 7, height: 7, borderRadius: 4 },
  statusText: { color: palette.textMuted, fontSize: font.caption },
  feed: { gap: space.md, paddingVertical: space.md, flexGrow: 1 },
  composer: { flexDirection: 'row', alignItems: 'center', gap: space.sm },
  input: {
    flex: 1,
    height: 48,
    borderRadius: radius.pill,
    paddingHorizontal: space.lg,
    backgroundColor: palette.surface,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: palette.border,
    color: palette.text,
    fontSize: font.body,
  },
  send: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: palette.primaryStrong,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sendDisabled: { opacity: 0.4 },
  micArea: { alignItems: 'center', paddingVertical: space.md },
});
