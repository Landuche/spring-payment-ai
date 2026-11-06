package dio.budgeting.infrastructure.persistence.entity;

import dio.budgeting.domain.Account;
import dio.budgeting.domain.AccountId;
import dio.budgeting.domain.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AccountEntity {
    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false, unique = true)
    private UUID ownerId;

    @Column(name = "account_number", nullable = false, unique = true, length = 10)
    private String number;

    public static AccountEntity from(Account account) {
        return new AccountEntity(account.id().uuid(), account.ownerId().uuid(), account.number());
    }

    public Account toDomain() {
        return new Account(new AccountId(id), new UserId(ownerId), number);
    }
}
