package org.processing.system.emailconsumer.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.processing.system.emailconsumer.model.NotificationEvent;
import org.processing.system.emailconsumer.sender.EmailSender;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailConsumer {

    private final EmailSender emailSender;

    public EmailConsumer(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @KafkaListener(
            topics = "${kafka.topics.notification-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, NotificationEvent> record) {
        NotificationEvent event = record.value();

        MDC.put("eventId", event.getEventId());
        MDC.put("userId", event.getUserId());
        MDC.put("channel", event.getChannel());

        try {

            if (!"EMAIL".equals(event.getChannel())) {
                log.debug("Skipping non-email event | eventId={} channel={}",
                        event.getEventId(), event.getChannel());
                return;
            }

            log.info("Processing email event | eventId={} recipient={}",
                    event.getEventId(), event.getRecipient());

            emailSender.send(event);

            log.info("Email delivered successfully | eventId={}", event.getEventId());

        } finally {
            MDC.clear();
        }
    }
}
