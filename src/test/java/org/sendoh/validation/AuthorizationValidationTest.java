package org.sendoh.validation;

import org.junit.Test;
import org.sendoh.model.input.Account;
import org.sendoh.model.input.AuthorizationInput;
import org.sendoh.model.input.Transaction;
import org.sendoh.model.violation.AuthorizationViolation;

import java.time.Instant;
import java.util.EnumSet;

import static org.junit.Assert.assertTrue;

public class AuthorizationValidationTest {

    @Test
    public void isInitialized_givenInitialized_violation() {
        Account account = new Account(true, 1);
        final AuthorizationInput input = new AuthorizationInput(account, null);
        AuthorizationValidation validation = AuthorizationValidation.isAccountExisted(account);

        final EnumSet<AuthorizationViolation> apply = validation.apply(input);

        assertTrue(apply.contains(AuthorizationViolation.ACCOUNT_ALREADY_INITIALIZED));
    }

    @Test
    public void isInitialized_givenNotInitialized_empty() {
        Account account = new Account(true, 1);
        final AuthorizationInput input = new AuthorizationInput(account, null);
        AuthorizationValidation validation = AuthorizationValidation.isAccountExisted(null);

        final EnumSet<AuthorizationViolation> apply = validation.apply(input);

        assertTrue(apply.isEmpty());
    }

    @Test
    public void isAccountInitialized_givenInitialized_empty() {
        Account account = new Account(true, 1);
        AuthorizationValidation validation = AuthorizationValidation.isAccountInitialized(account);
        Transaction transaction = new Transaction("test", 1, Instant.now());
        final AuthorizationInput input = new AuthorizationInput(null, transaction);
        final EnumSet<AuthorizationViolation> apply = validation.apply(input);

        assertTrue(apply.isEmpty());
    }

    @Test
    public void isAccountInitialized_givenNotInitialized_violation() {
        Transaction transaction = new Transaction("test", 1, Instant.now());
        final AuthorizationInput input = new AuthorizationInput(null, transaction);
        AuthorizationValidation validation = AuthorizationValidation.isAccountInitialized(null);

        final EnumSet<AuthorizationViolation> apply = validation.apply(input);

        assertTrue(apply.contains(AuthorizationViolation.ACCOUNT_NOT_INITIALIZED));
    }

    @Test
    public void isCardActivated_givenActiveCard_empty() {
        Account account = new Account(true, 1);
        Transaction transaction = new Transaction("test", 1, Instant.now());
        final AuthorizationInput input = new AuthorizationInput(null, transaction);
        AuthorizationValidation validation = AuthorizationValidation.isCardActivated(account);

        final EnumSet<AuthorizationViolation> apply = validation.apply(input);

        assertTrue(apply.isEmpty());
    }

    @Test
    public void isCardActivated_givenNoActiveCard_violation() {
        Account account = new Account(false, 1);
        Transaction transaction = new Transaction("test", 1, Instant.now());
        final AuthorizationInput input = new AuthorizationInput(null, transaction);
        AuthorizationValidation validation = AuthorizationValidation.isCardActivated(account);

        final EnumSet<AuthorizationViolation> apply = validation.apply(input);

        assertTrue(apply.contains(AuthorizationViolation.CARD_NOT_ACTIVE));
    }

    @Test
    public void isSufficientLimit_givenSufficientLimit_empty() {
        Account account = new Account(false, 1);
        AuthorizationValidation validation = AuthorizationValidation.isSufficientLimit(account);
        Transaction transaction = new Transaction("test", 1, Instant.now());
        final AuthorizationInput input = new AuthorizationInput(null, transaction);

        final EnumSet<AuthorizationViolation> apply = validation.apply(input);

        assertTrue(apply.isEmpty());
    }

    @Test
    public void isSufficientLimit_givenInSufficientLimit_violation() {
        Account account = new Account(false, 1);
        AuthorizationValidation validation = AuthorizationValidation.isSufficientLimit(account);
        Transaction transaction = new Transaction("test", 10, Instant.now());
        final AuthorizationInput input = new AuthorizationInput(null, transaction);

        final EnumSet<AuthorizationViolation> apply = validation.apply(input);

        assertTrue(apply.contains(AuthorizationViolation.INSUFFICIENT_LIMIT));
    }

    @Test
    public void isHighFrequencySmallInterval_givenHighFrequencyLargeInterval_empty() {
        final Instant firstTimeInterval = Instant.now();
        AuthorizationValidation validation = AuthorizationValidation.
                isHighFrequencySmallInterval(firstTimeInterval, 3L);
        Transaction transaction = new Transaction("test", 10, firstTimeInterval
                .plusSeconds(60000));
        final AuthorizationInput input = new AuthorizationInput(null, transaction);

        final EnumSet<AuthorizationViolation> apply = validation.apply(input);
        assertTrue(apply.isEmpty());
    }

    @Test
    public void isHighFrequencySmallInterval_givenLowFrequencySmallInterval_empty() {
        final Instant firstTimeInterval = Instant.now();
        AuthorizationValidation validation = AuthorizationValidation.
                isHighFrequencySmallInterval(firstTimeInterval, 1L);
        Transaction transaction = new Transaction("test", 10, firstTimeInterval
                .plusMillis(1));
        final AuthorizationInput input = new AuthorizationInput(null, transaction);

        final EnumSet<AuthorizationViolation> apply = validation.apply(input);
        assertTrue(apply.isEmpty());
    }

    @Test
    public void isHighFrequencySmallInterval_givenHighFrequencySmallInterval_violation() {
        final Instant firstTimeInterval = Instant.now();
        Transaction transaction = new Transaction("test", 19, firstTimeInterval
                .plusMillis(1));
        final AuthorizationInput input = new AuthorizationInput(null, transaction);
        AuthorizationValidation validation = AuthorizationValidation.
                isHighFrequencySmallInterval(firstTimeInterval, 3L);

        final EnumSet<AuthorizationViolation> apply = validation.apply(input);
        assertTrue(apply.contains(AuthorizationViolation.HIGH_FREQUENCY_SMALL_INTERVAL));
    }

    @Test
    public void isDoubleTransaction_givenDoubleTransactionLargeInterval_empty() {
        final Instant firstTimeInterval = Instant.now();
        Transaction transaction1 = new Transaction("test", 10, firstTimeInterval);
        Transaction transaction2 = new Transaction("test", 10, firstTimeInterval
                .plusSeconds(60000));
        final AuthorizationInput input = new AuthorizationInput(null, transaction2);

        AuthorizationValidation validation = AuthorizationValidation
                .isDoubleTransaction(transaction1);

        final EnumSet<AuthorizationViolation> apply = validation.apply(input);
        assertTrue(apply.isEmpty());
    }

    @Test
    public void isDoubleTransaction_givenDoubleTransactionSmallInterval_violation() {
        final Instant firstTimeInterval = Instant.now();
        Transaction transaction1 = new Transaction("test", 10, firstTimeInterval);
        Transaction transaction2 = new Transaction("test", 10, firstTimeInterval
                .plusMillis(1));
        final AuthorizationInput input = new AuthorizationInput(null, transaction2);

        AuthorizationValidation validation = AuthorizationValidation
                .isDoubleTransaction(transaction1);

        final EnumSet<AuthorizationViolation> apply = validation.apply(input);
        assertTrue(apply.contains(AuthorizationViolation.DOUBLED_TRANSACTION));
    }

    @Test
    public void and() {
        Account account = new Account(false, 1);
        final AuthorizationValidation and = AuthorizationValidation.isCardActivated(account)
                .and(AuthorizationValidation.isSufficientLimit(account));
        Transaction transaction = new Transaction("test", 10, Instant.now());
        final AuthorizationInput input = new AuthorizationInput(null, transaction);
        final EnumSet<AuthorizationViolation> apply = and.apply(input);

        assertTrue(apply.contains(AuthorizationViolation.CARD_NOT_ACTIVE));
        assertTrue(apply.contains(AuthorizationViolation.INSUFFICIENT_LIMIT));
    }
}
