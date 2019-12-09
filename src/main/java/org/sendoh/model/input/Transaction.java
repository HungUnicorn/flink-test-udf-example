package org.sendoh.model.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

import static org.sendoh.model.input.Account.DEFAULT_ACCOUNT_ID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    public final static String DEFAULT_TRANSACTION_ID = "default-transaction-id";

    private String transactionId;
    private String accountId;
    private String merchant;

    // Better to use BigDecimal, but both the input and output are not
    private int amount;

    // When comparing transaction, time is ignored
    @EqualsAndHashCode.Exclude
    private Instant time;

    public Transaction(String merchant, int amount, Instant time) {
        this.accountId = DEFAULT_ACCOUNT_ID;
        this.transactionId = DEFAULT_TRANSACTION_ID;
        this.merchant = merchant;
        this.amount = amount;
        this.time = time;
    }

    // Jackson serialization
    public String getTransactionId() {
        if (transactionId == null) {
            return DEFAULT_TRANSACTION_ID;
        }
        return transactionId;
    }

    // Jackson serialization
    public String getAccountId() {
        if (accountId == null) {
            return DEFAULT_ACCOUNT_ID;
        }
        return accountId;
    }
}
