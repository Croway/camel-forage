package io.kaoto.forage.jms;

import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.citrusframework.spi.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.activemq.ArtemisContainer;
import io.kaoto.forage.integration.tests.ForageIntegrationTest;
import io.kaoto.forage.integration.tests.ForageTestCaseRunner;
import io.kaoto.forage.integration.tests.IntegrationTestSetupExtension;
import io.kaoto.forage.integration.tests.PropertiesTemplateHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@CitrusSupport
@ExtendWith(IntegrationTestSetupExtension.class)
public class JmsArtemisTest implements ForageIntegrationTest {
    private static final Logger LOG = LoggerFactory.getLogger(JmsArtemisTest.class);

    public static final String INTEGRATION_NAME = "jms-routes";
    public static final String PRODUCER_INTEGRATION_NAME = "jms-producer";

    @Override
    public String runBeforeAll(ForageTestCaseRunner runner, Consumer<AutoCloseable> afterAll) {
        // Load template properties and replace testcontainer-specific values
        ArtemisContainer artemis = JmsContainers.artemis();
        String brokerUrl = "tcp://" + artemis.getHost() + ":" + artemis.getMappedPort(61616);
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
                .addResource(classResource("route-artemis.camel.yaml"))
                .dumpIntegrationOutput(true));
        registerIntegrationCleanup(runner, INTEGRATION_NAME, afterAll);

        // producer application: an independent process with a plain (non-XA) connection
        // factory, modeling an external system publishing to the broker (#427)
        runner.when(camel().jbang()
                .custom("forage", "run")
                .processName(PRODUCER_INTEGRATION_NAME)
                .addResource(producerProperties)
                .addResource(classResource("producer/route-artemis-producer.camel.yaml"))
                .dumpIntegrationOutput(true));
        registerIntegrationCleanup(runner, PRODUCER_INTEGRATION_NAME, afterAll);

        return INTEGRATION_NAME;
    }

    /**
     * Test based on <a href="https://github.com/megacamelus/forage-examples/tree/main/jms/transactional">JMS transactional example</a>.
     */
    @Test
    @CitrusTest()
    public void artemisTransactional(ForageTestCaseRunner runner) {

        // the successful message commits through to the output queue
        runner.then(camel().jbang()
                .verify()
                .integration(INTEGRATION_NAME)
                .waitForLogMessage("Successfully processed message: Transactional message"));

        // the failing messages are dead-lettered to DLQ within the XA transaction
        runner.then(camel().jbang()
                .verify()
                .integration(INTEGRATION_NAME)
                .waitForLogMessage("Message sent to DLQ after max redeliveries"));

        // an XA rollback returns the message to the broker: the broker redelivers it with
        // JMSRedelivered=true; with broken XA enlistment the message would be lost (#427)
        runner.then(camel().jbang()
                .verify()
                .integration(INTEGRATION_NAME)
                .waitForLogMessage("Message redelivered after XA rollback"));
    }
}
