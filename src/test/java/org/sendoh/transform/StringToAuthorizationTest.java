package org.sendoh.transform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.util.Collector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sendoh.model.input.Account;
import org.sendoh.model.input.AuthorizationInput;
import org.sendoh.model.input.Transaction;

import java.time.Instant;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class StringToAuthorizationTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private StringToAuthorization transformer;

    // format java8 instant in json requires jsr310
    static {
        objectMapper.findAndRegisterModules();
    }

    @Mock
    private Collector<AuthorizationInput> collector;

    @Test
    public void flatMap_account() throws Exception {
        String input = "{\"account\": {\"active-card\": true, \"available-limit\": 100}}";

        transformer = new StringToAuthorization();
        transformer.flatMap(input, collector);
        transformer.flatMap(input, collector);

        AuthorizationInput expected = new AuthorizationInput(new Account(true, 100),
                null);

        verify(collector, times(2)).collect(expected);
    }

    @Test
    public void flatMap_transaction() throws Exception {
        String input = "{\"transaction\": {\"merchant\": \"Burger King\", \"amount\": 20, \"time\": \"2019-02-13T10:00:00.000Z\"}}";

        transformer = new StringToAuthorization();
        transformer.flatMap(input, collector);
        transformer.flatMap(input, collector);

        AuthorizationInput expected = new AuthorizationInput(null,
                new Transaction("Burger King", 20, Instant.parse("2019-02-13T10:00:00.000Z")));

        verify(collector, times(2)).collect(expected);
    }

    @Test
    public void flatMap_accountAndTransaction() throws Exception {
        String accountInput = "{\"account\": {\"active-card\": true, \"available-limit\": 100}}";
        String transactionInput = "{\"transaction\": {\"merchant\": \"Burger King\", \"amount\": 20, \"time\": \"2019-02-13T10:00:00.000Z\"}}";

        transformer = new StringToAuthorization();
        transformer.flatMap(accountInput, collector);
        transformer.flatMap(transactionInput, collector);

        AuthorizationInput expectedAccount = new AuthorizationInput(new Account(true, 100),
                null);
        AuthorizationInput expectedTransaction = new AuthorizationInput(null,
                new Transaction("Burger King", 20, Instant.parse("2019-02-13T10:00:00.000Z")));

        verify(collector, times(1)).collect(expectedAccount);
        verify(collector, times(1)).collect(expectedTransaction);
    }
}
