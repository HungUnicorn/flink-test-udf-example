package org.sendoh.validation;

import org.sendoh.model.input.Account;
import org.sendoh.model.input.AuthorizationInput;
import org.sendoh.model.input.Transaction;
import org.sendoh.model.violation.AuthorizationViolation;

import java.time.Instant;
import java.util.EnumSet;
import java.util.function.Function;

/**
 * Implementation fo business rules. Validate against AuthorizationInput which contains either transaction or account,
 * and generates AuthorizationViolation
 *
 * @see org.sendoh.model.input.AuthorizationInput
 * @see org.sendoh.model.violation.AuthorizationViolation
 **/

public interface AuthorizationValidation extends Function<AuthorizationInput, EnumSet<AuthorizationViolation>> {
    int SMALL_INTERVAL_SECONDS = 120;
    long HIGH_FREQUENCY_COUNT = 2;

    static AuthorizationValidation isAccountExisted(Account existed) {
        return input -> {
            if (input.getAccount().isPresent() && existed != null) {
                return EnumSet.of(AuthorizationViolation.ACCOUNT_ALREADY_INITIALIZED);
            }
            return EnumSet.noneOf(AuthorizationViolation.class);
        };
    }

    static AuthorizationValidation isAccountInitialized(Account existed) {
        return input -> {
            if (input.getTransaction().isPresent() && existed == null) {
                return EnumSet.of(AuthorizationViolation.ACCOUNT_NOT_INITIALIZED);
            }
            return EnumSet.noneOf(AuthorizationViolation.class);
        };
    }

    static AuthorizationValidation isCardActivated(Account account) {
        return input -> {
            if (input.getTransaction().isPresent() && account != null && !account.isActiveCard()) {
                return EnumSet.of(AuthorizationViolation.CARD_NOT_ACTIVE);
            }
            return EnumSet.noneOf(AuthorizationViolation.class);
        };
    }

    static AuthorizationValidation isSufficientLimit(Account account) {
        return input -> {
            if (input.getTransaction().isPresent()) {
                Transaction transaction = input.getTransaction().get();

                if (account != null && account.getAvailableLimit() - transaction.getAmount() < 0) {
                    return EnumSet.of(AuthorizationViolation.INSUFFICIENT_LIMIT);
                }
            }
            return EnumSet.noneOf(AuthorizationViolation.class);
        };
    }

    static AuthorizationValidation isHighFrequencySmallInterval(Instant firstTimeInterval, Long count) {
        return input -> {
            if (input.getTransaction().isPresent() && firstTimeInterval != null && count != null) {
                Transaction transaction = input.getTransaction().get();

                if (firstTimeInterval.plusSeconds(SMALL_INTERVAL_SECONDS).isAfter(transaction.getTime()) &&
                        count > HIGH_FREQUENCY_COUNT) {
                    return EnumSet.of(AuthorizationViolation.HIGH_FREQUENCY_SMALL_INTERVAL);
                }
            }
            return EnumSet.noneOf(AuthorizationViolation.class);
        };
    }

    static AuthorizationValidation isDoubleTransaction(Transaction lastTransaction) {
        return input -> {
            if (input.getTransaction().isPresent() && lastTransaction != null) {
                Transaction transaction = input.getTransaction().get();

                if (lastTransaction.getTime().plusSeconds(SMALL_INTERVAL_SECONDS).isAfter(transaction.getTime())
                        && lastTransaction.equals(transaction)) {
                    return EnumSet.of(AuthorizationViolation.DOUBLED_TRANSACTION);
                }
            }
            return EnumSet.noneOf(AuthorizationViolation.class);
        };
    }

    default AuthorizationValidation and(AuthorizationValidation other) {
        return it -> {
            final EnumSet<AuthorizationViolation> apply = this.apply(it);
            apply.addAll(other.apply(it));
            return apply;
        };
    }
}
