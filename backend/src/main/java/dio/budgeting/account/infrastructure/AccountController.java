package dio.budgeting.account.infrastructure;

import dio.budgeting.account.application.AccountOutput;
import dio.budgeting.account.application.GetAccountUseCase;
import dio.budgeting.shared.domain.BusinessRuleViolationException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AccountController {
    private final GetAccountUseCase getAccount;

    public AccountController(GetAccountUseCase getAccount) {
        this.getAccount = getAccount;
    }

    @GetMapping("/account")
    public AccountOutput myAccount() {
        return getAccount.myAccount();
    }

    /** Busca o destinatário por número OU e-mail, para confirmação antes da transferência. */
    @GetMapping("/accounts/lookup")
    public AccountOutput lookup(@RequestParam(required = false) String number,
                                @RequestParam(required = false) String email) {
        if ((number == null) == (email == null)) {
            throw new BusinessRuleViolationException("Informe number ou email (apenas um)");
        }
        return number != null ? getAccount.findByNumber(number) : getAccount.findByEmail(email);
    }
}
