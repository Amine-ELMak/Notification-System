package org.processing.system.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.processing.system.notificationservice.dto.NotificationRequest;
import org.processing.system.notificationservice.entity.NotificationLog;
import org.processing.system.notificationservice.model.NotificationEvent;
import org.processing.system.notificationservice.repository.NotificationLogRepository;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class NotificationService {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final NotificationLogRepository logRepository;

    @Value("${kafka.topics.notification-events}")
    private String topic;

    public NotificationService(KafkaTemplate<String, NotificationEvent> kafkaTemplate,
                               NotificationLogRepository logRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.logRepository = logRepository;
    }

    public void send(NotificationRequest request) {

        NotificationEvent event = new NotificationEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setUserId(request.getUserId());
        event.setChannel(request.getChannel());
        event.setRecipient(request.getRecipient());
        event.setSubject(request.getSubject());
        event.setMessage(request.getMessage());
        event.setCreatedAt(LocalDateTime.now());

        // MDC = Mapped Diagnostic Context. A thread-local key-value store that
        // Logback automatically includes in every log line produced by this thread.
        // This means every log line below will include eventId, userId, channel
        // without us passing them explicitly to every log.info() call.
        // We clear it before the async send because whenComplete() may run on a
        // different thread where this MDC context doesn't exist.
        MDC.put("eventId", event.getEventId());
        MDC.put("userId", event.getUserId());
        MDC.put("channel", event.getChannel());

        log.info("Publishing event | eventId={} channel={} userId={}",
                event.getEventId(), event.getChannel(), event.getUserId());

        MDC.clear();

        // send() is non-blocking — it returns immediately and the actual network call
        // happens asynchronously. whenComplete() is the callback that fires when
        // Kafka confirms receipt (or fails). This is why we log "Event published"
        // after the fact, not immediately after calling send().
        // Key = userId ensures all events for the same user go to the same partition,
        // preserving message ordering per user.
        kafkaTemplate.send(topic, event.getUserId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event | eventId={} error={}",
                                event.getEventId(), ex.getMessage());
                        persistLog(event, "FAILED");
                    } else {
                        log.info("Event published | eventId={} partition={} offset={}",
                                event.getEventId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                        persistLog(event, "PUBLISHED");
                    }
                });
    }

    private void persistLog(NotificationEvent event, String status) {
        NotificationLog auditLog = new NotificationLog();
        auditLog.setEventId(event.getEventId());
        auditLog.setUserId(event.getUserId());
        auditLog.setChannel(event.getChannel());
        auditLog.setRecipient(event.getRecipient());
        auditLog.setStatus(status);
        auditLog.setCreatedAt(event.getCreatedAt());
        logRepository.save(auditLog);
    }
}