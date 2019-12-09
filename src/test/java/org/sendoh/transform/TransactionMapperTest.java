package org.sendoh.transform;

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.operators.StreamFlatMap;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness;
import org.apache.flink.streaming.util.TestHarnessUtil;
import org.junit.Before;
import org.junit.Test;
import org.sendoh.model.input.Account;
import org.sendoh.model.input.AuthorizationInput;
import org.sendoh.model.input.Transaction;
import org.sendoh.model.validation.ValidationResult;
import org.sendoh.model.violation.AuthorizationViolation;

import java.time.Instant;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentLinkedQueue;


public class TransactionMapperTest {

    private KeyedOneInputStreamOperatorTestHarness<String, AuthorizationInput, ValidationResult> testHarness;
    private AuthorizationMapper statefulFlatMapFunction;

    @Before
    public void setupTestHarness() throws Exception {

        //instantiate user-defined function
        statefulFlatMapFunction = new AuthorizationMapper();

        // wrap user defined function into a the corresponding operator
        testHarness = new KeyedOneInputStreamOperatorTestHarness<>(
                new StreamFlatMap<>(statefulFlatMapFunction),
                (KeySelector<AuthorizationInput, String>) AuthorizationInput::getAuthorizationId,
                Types.STRING);

        // open the test harness (will also call open() on RichFunctions)
        testHarness.open();
    }

    @Test
    public void testTransactionMapper_givenValidAuthorization() throws Exception {
        final Transaction transaction = new Transaction("test", 1, Instant.now());
        final Account account = new Account(true, 1);
        final AuthorizationInput input1 = new AuthorizationInput(account, null);
        final AuthorizationInput input2 = new AuthorizationInput(null, transaction);

        testHarness.processElement(new StreamRecord<>(input1));
        testHarness.processElement(new StreamRecord<>(input2));

        final ConcurrentLinkedQueue<Object> expected = new ConcurrentLinkedQueue<>();
        expected.add(new StreamRecord<>(new ValidationResult(account,
                EnumSet.noneOf(AuthorizationViolation.class))));

        expected.add(new StreamRecord<>(new ValidationResult(account,
                EnumSet.noneOf(AuthorizationViolation.class))));

        TestHarnessUtil.assertOutputEquals("Output was not correct", expected, testHarness.getOutput());
    }

    @Test
    public void testTransactionMapper_givenTransactionBeforeAccount() throws Exception {
        final Account account = new Account(true, 1);
        final Transaction transaction = new Transaction("test", 10, Instant.now());

        final AuthorizationInput input1 = new AuthorizationInput(null, transaction);
        final AuthorizationInput input2 = new AuthorizationInput(account, null);

        testHarness.processElement(new StreamRecord<>(input1));
        testHarness.processElement(new StreamRecord<>(input2));

        final ConcurrentLinkedQueue<Object> expected = new ConcurrentLinkedQueue<>();
        expected.add(new StreamRecord<>(new ValidationResult(null,
                EnumSet.of(AuthorizationViolation.ACCOUNT_NOT_INITIALIZED))));
        expected.add(new StreamRecord<>(new ValidationResult(account,
                EnumSet.noneOf(AuthorizationViolation.class))));

        TestHarnessUtil.assertOutputEquals("Output was not correct", expected, testHarness.getOutput());
    }

    @Test
    public void testTransactionMapper_givenInvalidAccountInfoAndDoubleTransaction() throws Exception {
        final Transaction transaction = new Transaction("test", 10, Instant.now());
        final Account account = new Account(false, 1);
        final AuthorizationInput input1 = new AuthorizationInput(account, null);
        final AuthorizationInput input2 = new AuthorizationInput(null, transaction);
        final AuthorizationInput input3 = new AuthorizationInput(null, transaction);

        testHarness.processElement(new StreamRecord<>(input1));
        testHarness.processElement(new StreamRecord<>(input2));
        testHarness.processElement(new StreamRecord<>(input3));

        final ConcurrentLinkedQueue<Object> expected = new ConcurrentLinkedQueue<>();
        expected.add(new StreamRecord<>(new ValidationResult(account,
                EnumSet.noneOf(AuthorizationViolation.class))));
        expected.add(new StreamRecord<>(new ValidationResult(account,
                EnumSet.of(AuthorizationViolation.CARD_NOT_ACTIVE, AuthorizationViolation.INSUFFICIENT_LIMIT))));
        expected.add(new StreamRecord<>(new ValidationResult(account,
                EnumSet.of(AuthorizationViolation.CARD_NOT_ACTIVE, AuthorizationViolation.INSUFFICIENT_LIMIT,
                        AuthorizationViolation.DOUBLED_TRANSACTION))));

        TestHarnessUtil.assertOutputEquals("Output was not correct", expected, testHarness.getOutput());
    }

    @Test
    public void testTransactionMapper_givenInvalidAccountInfoAndDoubleTransactionAndHighFrequencySmallInterval()
            throws Exception {
        final Transaction transaction = new Transaction("test", 19, Instant.now());
        final Account account = new Account(false, 1);
        final AuthorizationInput input1 = new AuthorizationInput(account, null);
        final AuthorizationInput input2 = new AuthorizationInput(null, transaction);

        testHarness.processElement(new StreamRecord<>(input1));
        testHarness.processElement(new StreamRecord<>(input2));
        testHarness.processElement(new StreamRecord<>(input2));
        testHarness.processElement(new StreamRecord<>(input2));
        testHarness.processElement(new StreamRecord<>(input2));

        final ConcurrentLinkedQueue<Object> expected = new ConcurrentLinkedQueue<>();
        expected.add(new StreamRecord<>(new ValidationResult(account,
                EnumSet.noneOf(AuthorizationViolation.class))));
        expected.add(new StreamRecord<>(new ValidationResult(account,
                EnumSet.of(AuthorizationViolation.CARD_NOT_ACTIVE, AuthorizationViolation.INSUFFICIENT_LIMIT))));
        expected.add(new StreamRecord<>(new ValidationResult(account,
                EnumSet.of(AuthorizationViolation.CARD_NOT_ACTIVE, AuthorizationViolation.INSUFFICIENT_LIMIT,
                        AuthorizationViolation.DOUBLED_TRANSACTION))));
        expected.add(new StreamRecord<>(new ValidationResult(account,
                EnumSet.of(AuthorizationViolation.CARD_NOT_ACTIVE, AuthorizationViolation.INSUFFICIENT_LIMIT,
                        AuthorizationViolation.DOUBLED_TRANSACTION))));
        expected.add(new StreamRecord<>(new ValidationResult(account,
                EnumSet.of(AuthorizationViolation.CARD_NOT_ACTIVE, AuthorizationViolation.INSUFFICIENT_LIMIT,
                        AuthorizationViolation.DOUBLED_TRANSACTION,
                        AuthorizationViolation.HIGH_FREQUENCY_SMALL_INTERVAL))));

        TestHarnessUtil.assertOutputEquals("Output was not correct", expected, testHarness.getOutput());
    }

    @Test
    public void testTransactionMapper_givenAccountTwice_AccountAlreadyInitialized()
            throws Exception {
        final Transaction transaction = new Transaction("test", 10, Instant.now());
        final Account account = new Account(true, 10);
        final AuthorizationInput input1 = new AuthorizationInput(account, null);
        final AuthorizationInput input2 = new AuthorizationInput(null, transaction);

        testHarness.processElement(new StreamRecord<>(input1));
        testHarness.processElement(new StreamRecord<>(input2));
        testHarness.processElement(new StreamRecord<>(input1));

        final ConcurrentLinkedQueue<Object> expected = new ConcurrentLinkedQueue<>();
        expected.add(new StreamRecord<>(new ValidationResult(account,
                EnumSet.noneOf(AuthorizationViolation.class))));
        expected.add(new StreamRecord<>(new ValidationResult(account,
                EnumSet.noneOf(AuthorizationViolation.class))));
        expected.add(new StreamRecord<>(new ValidationResult(account,
                EnumSet.of(AuthorizationViolation.ACCOUNT_ALREADY_INITIALIZED))));

        TestHarnessUtil.assertOutputEquals("Output was not correct", expected, testHarness.getOutput());
    }
}
