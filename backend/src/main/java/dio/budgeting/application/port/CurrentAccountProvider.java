package dio.budgeting.application.port;

import dio.budgeting.domain.AccountId;

/** Conta do usuário autenticado na requisição atual. Os casos de uso nunca recebem a conta por parâmetro. */
public interface CurrentAccountProvider {
    AccountId current();
}
