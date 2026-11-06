package dio.budgeting.assistant.application;


import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Propostas que as ferramentas do assistente geraram durante UMA resposta. O controller cria um por mensagem,
 * entrega às ferramentas pelo contexto e depois devolve ao app o que ficou registrado, para ele mostrar o cartão
 * de confirmação. Se o modelo preparar duas vezes na mesma resposta, vale a última.
 */
public final class PropostasDoTurno {
    private final AtomicReference<PropostaDeTransferencia> transferencia = new AtomicReference<>();

    public void registrar(PropostaDeTransferencia proposta) {
        transferencia.set(proposta);
    }

    public Optional<PropostaDeTransferencia> transferencia() {
        return Optional.ofNullable(transferencia.get());
    }
}
