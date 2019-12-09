package org.sendoh.model.violation;

public enum AuthorizationViolation {
    /**  Once created, the account should not be updated or recreated **/
    ACCOUNT_ALREADY_INITIALIZED,

    /** No transaction should be accepted without a properly initialized account **/
    ACCOUNT_NOT_INITIALIZED,

    /** No transaction should be accepted when the card is not active **/
    CARD_NOT_ACTIVE,

    /** The transaction amount should not exceed available limit **/
    INSUFFICIENT_LIMIT,

    /** There should not be more than 3 transactions on a 2 minute interval: `high-frequency-small-interval` **/
    HIGH_FREQUENCY_SMALL_INTERVAL,

    /** There should not be more than 1 similar transactions (same amount and merchant) in a 2 minutes interval **/
    DOUBLED_TRANSACTION
}
