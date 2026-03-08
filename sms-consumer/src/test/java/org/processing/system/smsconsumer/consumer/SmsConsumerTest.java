package org.processing.system.smsconsumer.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.processing.system.smsconsumer.model.NotificationEvent;
import org.processing.system.smsconsumer.sender.SmsSender;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;


@ExtendWith(MockitoExtension.class)
class SmsConsumerTest {

    @Mock
    private SmsSender smsSender;

    @InjectMocks
    private SmsConsumer smsConsumer;

    // happy path
    @Test
    void shouldCallEmailSenderForSmsChannel() {
        NotificationEvent event = buildEvent("SMS");
        ConsumerRecord<String, NotificationEvent> record =
                new ConsumerRecord<>("notification.events", 0, 0L, "key", event);

        smsConsumer.consume(record);

        verify(smsSender, times(1)).send(event);
    }

    // channel filter
    @Test
    void shouldSkipNonSmsEvents() {
        NotificationEvent event = buildEvent("EMAIL");
        ConsumerRecord<String, NotificationEvent> record =
                new ConsumerRecord<>("notification.events", 0, 0L, "key", event);

        smsConsumer.consume(record);

        verify(smsSender, never()).send(any());
    }

    // exception propagation
    @Test
    void shouldPropagateExceptionSoDefaultErrorHandlerCanRetry() {
        NotificationEvent event = buildEvent("SMS");
        ConsumerRecord<String, NotificationEvent> record =
                new ConsumerRecord<>("notification.events", 0, 0L, "key", event);

        doThrow(new RuntimeException("SMS timeout")).when(smsSender).send(any());

        assertThrows(RuntimeException.class, () -> smsConsumer.consume(record));
    }

    private NotificationEvent buildEvent(String channel) {
        NotificationEvent event = new NotificationEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setUserId("user-123");
        event.setChannel(channel);
        event.setRecipient("+1234567890");
        event.setMessage("Hello");
        return event;
    }
}