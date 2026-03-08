package org.processing.system.emailconsumer.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.processing.system.emailconsumer.model.NotificationEvent;
import org.processing.system.emailconsumer.sender.EmailSender;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailConsumerTest {

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private EmailConsumer emailConsumer;

    // happy path
    @Test
    void shouldCallEmailSenderForEmailChannel() {
        NotificationEvent event = buildEvent("EMAIL");
        ConsumerRecord<String, NotificationEvent> record =
                new ConsumerRecord<>("notification.events", 0, 0L, "key", event);

        emailConsumer.consume(record);

        verify(emailSender, times(1)).send(event);
    }

    // channel filter
    @Test
    void shouldSkipNonEmailEvents() {
        NotificationEvent event = buildEvent("SMS");
        ConsumerRecord<String, NotificationEvent> record =
                new ConsumerRecord<>("notification.events", 0, 0L, "key", event);

        emailConsumer.consume(record);

        verify(emailSender, never()).send(any());
    }

    // exception propagation
    @Test
    void shouldPropagateExceptionSoDefaultErrorHandlerCanRetry() {
        NotificationEvent event = buildEvent("EMAIL");
        ConsumerRecord<String, NotificationEvent> record =
                new ConsumerRecord<>("notification.events", 0, 0L, "key", event);

        doThrow(new RuntimeException("SMTP timeout")).when(emailSender).send(any());

        assertThrows(RuntimeException.class, () -> emailConsumer.consume(record));
    }

    private NotificationEvent buildEvent(String channel) {
        NotificationEvent event = new NotificationEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setUserId("user-123");
        event.setChannel(channel);
        event.setRecipient("test@example.com");
        event.setSubject("Test");
        event.setMessage("Hello");
        return event;
    }
}