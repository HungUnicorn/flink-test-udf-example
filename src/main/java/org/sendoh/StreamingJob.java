/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sendoh;

import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.sendoh.model.input.AuthorizationInput;
import org.sendoh.model.validation.ValidationResult;
import org.sendoh.transform.AuthorizationMapper;
import org.sendoh.transform.StringToAuthorization;

/**
 * A flink job that demonstrates validation using states
 * <p>
 * - Read string from terminal, by nc -lk 9999
 * - Transform string to POJO
 * - Validate based on rules with states
 * - Transform POJO to string for json in string
 */
public class StreamingJob {
    private static final int INPUT_PORT = 9999;

    public static void main(String[] args) throws Exception {
        // set up the local execution env and web ui is available at port 8081
        Configuration conf = new Configuration();
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(conf);

        final DataStream<AuthorizationInput> inputStream = env.socketTextStream("localhost", INPUT_PORT)
                .flatMap(new StringToAuthorization());

        final DataStream<ValidationResult> validationResultStream = inputStream
                .keyBy((KeySelector<AuthorizationInput, String>) AuthorizationInput::getAuthorizationId)
                .flatMap(new AuthorizationMapper());

        validationResultStream.print();

        // execute program
        env.execute("flink-test-stateful-udf");
    }
}
