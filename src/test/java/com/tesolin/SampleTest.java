package com.tesolin;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterTier;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterType;
import com.amazonaws.services.simplesystemsmanagement.model.PutParameterRequest;

import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class SampleTest {

    private static final Logger LOGGER  = LoggerFactory.getLogger(SampleTest.class);

    private LocalStackContainer localstack;

    @Test
    public void testInitScript() {
        Instant now = Instant.now();
        localstack = new LocalStackContainer("0.11.2")
                .withServices(LocalStackContainer.Service.SSM).withClasspathResourceMapping("/localstack/init.sh",
                        "/docker-entrypoint-initaws.d/init.sh", BindMode.READ_ONLY)
                .waitingFor(Wait.forLogMessage(".*End loading\\.\n", 1));
        localstack.start();
        Duration duration = Duration.between(now, Instant.now());
        LOGGER.info("Init script process time : {}", duration);
    }

    @Test
    public void testSdkInit() {
        Instant now = Instant.now();
        localstack = new LocalStackContainer("0.11.2").withServices(LocalStackContainer.Service.SSM);

        localstack.start();
        Map<String, String> parameters = IntStream.range(0, 60).boxed()
                .collect(Collectors.toMap(i -> "key " + i, i -> "value " + i));
        // Initialize LocalStack
        AWSSimpleSystemsManagement ssm = AWSSimpleSystemsManagementClientBuilder
                .standard()
                .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.SSM))
                .withCredentials(localstack.getDefaultCredentialsProvider())
                .build();
        parameters.forEach((k,v) -> ssm.putParameter(
                new PutParameterRequest()
                        .withType(ParameterType.String)
                        .withName(k)
                        .withTier(ParameterTier.Standard)
                        .withValue(v)
                        .withOverwrite(true)
        ));
        Duration duration = Duration.between(now, Instant.now());
        LOGGER.info("Sdk init process time : {}", duration);
    }

    @After
    public void after() {
        localstack.stop();
    }
}