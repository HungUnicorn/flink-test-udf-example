package org.sendoh.transform;

/**
 * State name to be used in AuthorizationMapper
 *
 * @see org.sendoh.transform.AuthorizationMapper
 **/
public class State {
    public static String ACCOUNT = "account-info";
    public static String LAST_TRANSACTION = "last-transaction";
    public static String FIRST_TRANSACTION_TIME = "first-transaction-time";
    public static String TRANSACTION_COUNT = "transaction-count";
}
