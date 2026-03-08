package org.processing.system.emailconsumer.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.processing.system.emailconsumer.model.NotificationEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DlqMonitor {
    @KafkaListener(
            topics = "${kafka.topics.dlq}",
            groupId = "dlq-monitor-group"
    )
    public void monitor(ConsumerRecord<String, NotificationEvent> record) {
        NotificationEvent event = record.value();

        log.error("Message reached DLQ | eventId={} channel={} recipient={}",
                event.getEventId(), event.getChannel(), event.getRecipient());
    }
}