package org.sendoh.model.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationInput {
    private Account account;
    private Transaction transaction;

    public String getAuthorizationId() {
        if (account != null) {
            return account.getAccountId();
        }
        return transaction.getAccountId();
    }

    public Optional<Account> getAccount() {
        return Optional.ofNullable(account);
    }

    public Optional<Transaction> getTransaction() {
        return Optional.ofNullable(transaction);
    }
}
