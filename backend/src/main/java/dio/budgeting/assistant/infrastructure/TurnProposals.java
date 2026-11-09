package dio.budgeting.assistant.infrastructure;

import dio.budgeting.transfer.application.TransferProposal;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Propostas que as ferramentas do assistente geraram durante uma resposta.
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
