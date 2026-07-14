package io.kaoto.forage.jms;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

/**
 * Publishes the test messages that a separate producer application used to send (#434). A plain
 * JMS client in the test JVM stays fully decoupled from the consumer application's XA transaction,
 * exactly like the external producer process it replaces (#427), without paying a full
 * {@code camel forage run} (export + Maven build on Quarkus/Spring Boot) per class per suite.
 */
final class TestJmsProducer {

    private TestJmsProducer() {}

    /**
     * Sends the three "Transactional message N" messages with counter properties 1..3 to
     * input.queue: the consumer dead-letters counters 1 and 2 after max redeliveries and commits
     * counter 3 through to the output queue.
     */
    static void sendInputMessages(ConnectionFactory connectionFactory, String username, String password) {
        try (Connection connection = connectionFactory.createConnection(username, password);
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            Queue queue = session.createQueue("input.queue");
            MessageProducer producer = session.createProducer(queue);
            for (long counter = 1; counter <= 3; counter++) {
                TextMessage message = session.createTextMessage("Transactional message " + counter);
                message.setLongProperty("counter", counter);
                producer.send(message);
            }
        } catch (JMSException e) {
            throw new RuntimeException("Unable to send test messages to input.queue", e);
        }
    }
}
