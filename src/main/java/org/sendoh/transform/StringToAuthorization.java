package org.sendoh.transform;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;
import org.sendoh.model.input.AuthorizationInput;

/**
 * Transform from string to POJO to facilitate data manipulation later
 */

@Slf4j
public class StringToAuthorization implements FlatMapFunction<String, AuthorizationInput> {
    // cannot be injected
    private static final transient ObjectMapper objectMapper = new ObjectMapper();

    // format java8 instant in json requires jsr310
    static {
        objectMapper.findAndRegisterModules();
    }

    @Override
    public void flatMap(String value, Collector<AuthorizationInput> out) throws Exception {
        final AuthorizationInput input = objectMapper.readValue(value, AuthorizationInput.class);
        // TODO: validate against schema, and should not be collected if the output does not follow schema
        out.collect(input);
    }
}
