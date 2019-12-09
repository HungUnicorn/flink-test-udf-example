# flink-test-UDF-example
- java, flink, lombok, jackson 


## Getting started
Run the project in IDE and execute unit tests, including:

- [TransactionMapperTest](https://github.com/HungUnicorn/flink-test-udf-example/blob/master/src/test/java/org/sendoh/transform/TransactionMapperTest.java)
demonstrates how to test stateful UDF.
  
For more information, please check
https://ci.apache.org/projects/flink/flink-docs-stable/dev/stream/testing.html#unit-testing-stateful-or-timely-udfs--custom-operators
to see required dependencies

- [StringToAuthorizationTest](https://github.com/HungUnicorn/flink-test-udf-example/blob/master/src/test/java/org/sendoh/transform/StringToAuthorizationTest.java) shows how to test stateless UDF
