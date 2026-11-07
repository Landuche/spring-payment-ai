package dio.budgeting.assistant.infrastructure;

import dio.budgeting.transfer.application.TransferProposal;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Propostas que as ferramentas do assistente geraram durante UMA resposta. O {@link AssistantChat} cria um por mensagem,
 * entrega às ferramentas pelo contexto e depois devolve ao app o que ficou registrado, para ele mostrar o cartão
 * de confirmação. Se o modelo preparar duas vezes na mesma resposta, vale a última.
 */
public final class TurnProposals {
    private final AtomicReference<TransferProposal> transfer = new AtomicReference<>();

    public void register(TransferProposal proposal) {
        transfer.set(proposal);
    }

    public Optional<TransferProposal> transfer() {
        return Optional.ofNullable(transfer.get());
    }
}
