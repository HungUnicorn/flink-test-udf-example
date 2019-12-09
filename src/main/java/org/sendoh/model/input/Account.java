package org.sendoh.model.input;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    public final static String DEFAULT_ACCOUNT_ID = "default-account-id";

    @JsonIgnore
    private String accountId;

    @JsonProperty("active-card")
    private boolean activeCard;

    // Better to use BigDecimal, but both the input and output are not
    @JsonProperty("available-limit")
    private int availableLimit;

    public Account(boolean activeCard, int availableLimit) {
        this.accountId = DEFAULT_ACCOUNT_ID;
        this.activeCard = activeCard;
        this.availableLimit = availableLimit;
    }

    public String getAccountId() {
        if (accountId == null) {
            return DEFAULT_ACCOUNT_ID;
        }
        return accountId;
    }
}
