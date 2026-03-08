package org.processing.system.smsconsumer.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.processing.system.smsconsumer.model.NotificationEvent;
import org.processing.system.smsconsumer.sender.SmsSender;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SmsConsumer {

    private final SmsSender smsSender;

    public SmsConsumer(SmsSender smsSender) {
        this.smsSender = smsSender;
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

            if (!"SMS".equals(event.getChannel())) {
                log.debug("Skipping non-sms event");
                return;
            }

            log.info("Processing sms event | recipient={}", event.getRecipient());

            smsSender.send(event);

            log.info("sms delivered successfully");

        } finally {
            MDC.clear();
        }
    }
}
