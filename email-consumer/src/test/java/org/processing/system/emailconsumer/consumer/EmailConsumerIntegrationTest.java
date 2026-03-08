package org.processing.system.emailconsumer.consumer;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.processing.system.emailconsumer.model.NotificationEvent;
import org.processing.system.emailconsumer.sender.EmailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
class EmailConsumerIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void overrideKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @MockBean
    private EmailSender emailSender;

    @Test
    void shouldDeliverEmailEventToConsumer() throws Exception {
        NotificationEvent event = buildEvent("EMAIL", "test@example.com");

        kafkaTemplate.send("notification.events", event.getUserId(), event).get();

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                        verify(emailSender, times(1)).send(
                                argThat(e -> e.getEventId().equals(event.getEventId()))
                        )
                );
    }

    @Test
    void shouldNotDeliverSmsEventToEmailConsumer() throws Exception {
        NotificationEvent event = buildEvent("SMS", "+1234567890");

        kafkaTemplate.send("notification.events", event.getUserId(), event).get();

        Awaitility.await()
                .during(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(4))
                .untilAsserted(() -> verify(emailSender, never()).send(any()));
    }

    private NotificationEvent buildEvent(String channel, String recipient) {
        NotificationEvent event = new NotificationEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setUserId("user-integration-test");
        event.setChannel(channel);
        event.setRecipient(recipient);
        event.setSubject("Integration test");
        event.setMessage("Hello from Testcontainers");
        return event;
    }
}