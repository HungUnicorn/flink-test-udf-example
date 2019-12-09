package org.sendoh.transform;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;
import org.sendoh.model.input.Account;
import org.sendoh.model.input.AuthorizationInput;
import org.sendoh.model.input.Transaction;
import org.sendoh.model.validation.ValidationResult;
import org.sendoh.model.violation.AuthorizationViolation;
import org.sendoh.validation.AuthorizationValidation;

import java.io.IOException;
import java.time.Instant;
import java.util.EnumSet;

import static org.sendoh.transform.State.ACCOUNT;
import static org.sendoh.transform.State.FIRST_TRANSACTION_TIME;
import static org.sendoh.transform.State.LAST_TRANSACTION;
import static org.sendoh.transform.State.TRANSACTION_COUNT;
import static org.sendoh.validation.AuthorizationValidation.SMALL_INTERVAL_SECONDS;

/**
 * Process authorization operation that contains either account or transaction using states,
 * and output validation result.
 * <p>
 * This includes:
 * - Initialize validations
 * - Initialize states
 * - Update states under conditions
 * <p>
 * Validation is done in the other class so business logic is separate from implementation and flink depenndency
 * as much as possible
 *
 * @see org.sendoh.validation.AuthorizationValidation
 **/
@Slf4j
public class AuthorizationMapper extends RichFlatMapFunction<AuthorizationInput,
        ValidationResult> {

    private transient ValueState<Account> accountState;
    private transient ValueState<Transaction> lastTransactionState;
    private transient ValueState<Long> transactionCountState;
    private transient ValueState<Instant> firstTimeIntervalState;

    /**
     * States initialization
     *
     * @param configuration Flink runtime configuration
     */
    @Override
    public void open(Configuration configuration) {
        final ValueStateDescriptor<Account> accountDescriptor =
                new ValueStateDescriptor<>(ACCOUNT, Account.class);

        final ValueStateDescriptor<Transaction> lastTransactionDescriptor =
                new ValueStateDescriptor<>(LAST_TRANSACTION, Transaction.class);

        final ValueStateDescriptor<Long> transactionCountDescriptor =
                new ValueStateDescriptor<>(FIRST_TRANSACTION_TIME, Long.class);

        final ValueStateDescriptor<Instant> firstTransactionTimeDescriptor =
                new ValueStateDescriptor<>(TRANSACTION_COUNT, Instant.class);

        accountState = getRuntimeContext().getState(accountDescriptor);
        lastTransactionState = getRuntimeContext().getState(lastTransactionDescriptor);
        transactionCountState = getRuntimeContext().getState(transactionCountDescriptor);
        firstTimeIntervalState = getRuntimeContext().getState(firstTransactionTimeDescriptor);
    }

    /**
     * Map to authorized input to validation output
     *
     * @param value contains either account or transaction for validation
     * @param out   collect output for next operator
     * @throws Exception exception when retrieving value from state
     */
    @Override
    public void flatMap(AuthorizationInput value, Collector<ValidationResult> out) throws Exception {
        final Account existedAccount = accountState.value();
        final Transaction lastTransaction = lastTransactionState.value();
        final Instant firstTimeInterval = firstTimeIntervalState.value();
        final Long transactionIntervalCount = transactionCountState.value();

        final AuthorizationValidation validation = AuthorizationValidation.isAccountExisted(existedAccount)
                .and(AuthorizationValidation.isAccountInitialized(existedAccount))
                .and(AuthorizationValidation.isCardActivated(existedAccount))
                .and(AuthorizationValidation.isSufficientLimit(existedAccount))
                .and(AuthorizationValidation.isHighFrequencySmallInterval(firstTimeInterval,
                        transactionIntervalCount))
                .and(AuthorizationValidation.isDoubleTransaction(lastTransaction));

        final EnumSet<AuthorizationViolation> apply = validation.apply(value);

        initialAccountState(value, existedAccount);
        updateLastTransactionState(value);
        updateOrInitializeIntervalAndCountState(value, firstTimeInterval, transactionIntervalCount);

        if (value.getAccount().isPresent()) {
            out.collect(new ValidationResult(value.getAccount().get(), apply));
        } else if (value.getTransaction().isPresent()) {
            out.collect(new ValidationResult(existedAccount, apply));
        } else {
            log.error("TransactionMapper has incorrect value: {}", value);
        }
    }

    /**
     * Update/Initialize interval and the count in the state.
     *
     * @param value                    value that either contains account or transaction for updating state
     * @param firstTimeInterval        the earliest timestamp should be considered for small interval.
     * @param transactionIntervalCount number of transactions in the interval
     * @throws IOException exception when manipulating state
     */
    private void updateOrInitializeIntervalAndCountState(AuthorizationInput value, Instant firstTimeInterval, Long transactionIntervalCount)
            throws IOException {
        if (value.getTransaction().isPresent()) {
            if (firstTimeInterval == null || transactionIntervalCount == null) {
                initializeIntervalCountState(value.getTransaction().get().getTime());
            } else {
                updateIntervalAndCountStateIfInsideInterval(value.getTransaction().get().getTime(),
                        firstTimeInterval, transactionIntervalCount);
            }
        }
    }

    /**
     * Update last transaction in the state and always update
     *
     * @param value value that either contains account or transaction for updating state
     * @throws IOException exception when manipulating state
     */
    private void updateLastTransactionState(AuthorizationInput value) throws IOException {
        if (value.getTransaction().isPresent()) {
            lastTransactionState.update(value.getTransaction().get());
        }
    }

    /**
     * Initial account in the state. No update
     *
     * @param value          value that either contains account or transaction for updating state
     * @param existedAccount account that already existed
     * @throws IOException exception when manipulating state
     */
    private void initialAccountState(AuthorizationInput value, Account existedAccount) throws IOException {
        if (existedAccount == null && value.getAccount().isPresent()) {
            accountState.update(value.getAccount().get());
        }
    }

    /**
     * Update interval and count. If interval passes, should set interval and count as Initial
     *
     * @param current                 timestamp of current element
     * @param firstTimeInterval       the earliest timestamp should be considered for small interval.
     * @param currentTransactionCount number of transactions in the interval
     * @throws IOException exception when manipulating state
     */
    private void updateIntervalAndCountStateIfInsideInterval(Instant current, Instant firstTimeInterval,
                                                             long currentTransactionCount) throws IOException {
        if (isInsideSmallInterval(current, firstTimeInterval)) {
            transactionCountState.update(currentTransactionCount + 1);

        } else {
            initializeIntervalCountState(current);
        }
    }

    /**
     * Check if current element is inside the interval
     *
     * @param current           timestamp of current element
     * @param firstTimeInterval the earliest timestamp should be considered for small interval.
     * @return true if current timestamp is in the interval
     */
    private boolean isInsideSmallInterval(Instant current, Instant firstTimeInterval) {
        return firstTimeInterval.plusSeconds(SMALL_INTERVAL_SECONDS).isAfter(current);
    }

    /**
     * Initialize interval and count state. The current element is considered so start with count 1
     *
     * @param time timestamp of current element
     * @throws IOException exception when manipulation state
     */
    private void initializeIntervalCountState(Instant time) throws IOException {
        firstTimeIntervalState.update(time);
        transactionCountState.update(1L);
    }
}
