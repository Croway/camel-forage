package io.kaoto.forage.jms;

import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.citrusframework.spi.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import io.kaoto.forage.integration.tests.ForageIntegrationTest;
import io.kaoto.forage.integration.tests.ForageTestCaseRunner;
import io.kaoto.forage.integration.tests.IntegrationTestSetupExtension;
import io.kaoto.forage.integration.tests.PropertiesTemplateHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;

@CitrusSupport
@ExtendWith(IntegrationTestSetupExtension.class)
@DisabledOnOs(OS.MAC)
public class JmsIbmMqTest implements ForageIntegrationTest {
    private static final Logger LOG = LoggerFactory.getLogger(JmsIbmMqTest.class);

    public static final String INTEGRATION_NAME = "jms-routes";
    public static final String PRODUCER_INTEGRATION_NAME = "jms-producer";

    @Override
    public String runBeforeAll(ForageTestCaseRunner runner, Consumer<AutoCloseable> afterAll) {
        // create the queues; they may already exist when a previous suite ran against the
        // shared broker, in which case createQueue is a no-op
        GenericContainer<?> ibmmq = JmsContainers.ibmmq();
        IBMMQDestinations destinations = new IBMMQDestinations(
                ibmmq.getHost(), ibmmq.getMappedPort(JmsContainers.IBMMQ_PORT), JmsContainers.IBMMQ_QUEUE_MANAGER_NAME);
        destinations.createQueue("input.queue");
        destinations.createQueue("output.queue");
        destinations.createQueue("DLQ");

        // Load template properties and replace testcontainer-specific values
        String brokerUrl = "mq://%s:%d/%s/%s"
                .formatted(
                        ibmmq.getHost(),
                        ibmmq.getMappedPort(JmsContainers.IBMMQ_PORT),
                        JmsContainers.IBMMQ_MESSAGING_CHANNEL,
                        JmsContainers.IBMMQ_QUEUE_MANAGER_NAME);
        Map<String, String> replacements = Map.of(
                "forage\\.jms\\.broker\\.url=.*", Matcher.quoteReplacement("forage.jms.broker.url=" + brokerUrl));
        Resource consumerProperties = PropertiesTemplateHelper.createFromTemplate(
                classResource("forage-connectionfactory.properties.template"), replacements, afterAll);
        Resource producerProperties = PropertiesTemplateHelper.createFromTemplate(
                classResource("producer/forage-connectionfactory.properties.template"), replacements, afterAll);
        // must be in the working directory (not passed as a resource) to act as camel-jbang run
        // config: it moves the producer's Quarkus HTTP server off 8080, which the consumer app owns
        PropertiesTemplateHelper.copyIntoSameDirectory(
                producerProperties, classResource("producer/application.properties"), afterAll);

        // consumer application (the system under test): XA transactions enabled
        runner.when(camel().jbang()
                .custom("forage", "run")
                .processName(INTEGRATION_NAME)
                .addResource(consumerProperties)
                .addResource(classResource("route-ibm.camel.yaml"))
                .dumpIntegrationOutput(true));
        registerIntegrationCleanup(runner, INTEGRATION_NAME, afterAll);

        // producer application: an independent process with a plain (non-XA) connection
        // factory, modeling an external system publishing to the broker (#427)
        runner.when(camel().jbang()
                .custom("forage", "run")
                .processName(PRODUCER_INTEGRATION_NAME)
                .addResource(producerProperties)
                .addResource(classResource("producer/route-ibm-producer.camel.yaml"))
                .dumpIntegrationOutput(true));
        registerIntegrationCleanup(runner, PRODUCER_INTEGRATION_NAME, afterAll);

        return INTEGRATION_NAME;
    }

    @Test
    @CitrusTest()
    public void ibmMqTransactional(ForageTestCaseRunner runner) {
        // the successful message commits through to the output queue via XA
        runner.then(camel().jbang()
                .verify()
                .integration(INTEGRATION_NAME)
                .waitForLogMessage("Successfully processed message: Transactional message"));

        // the failing messages are dead-lettered to DLQ within the XA transaction
        runner.then(camel().jbang()
                .verify()
                .integration(INTEGRATION_NAME)
                .waitForLogMessage("Message sent to DLQ after max redeliveries"));
    }
}
