package dio.budgeting.account.application;

import dio.budgeting.account.domain.AccountId;


/** Conta do usuário autenticado na requisição atual. Os casos de uso nunca recebem a conta por parâmetro. */
public interface CurrentAccountProvider {
    AccountId current();
}
