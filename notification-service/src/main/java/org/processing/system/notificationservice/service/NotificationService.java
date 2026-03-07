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


        MDC.put("eventId", event.getEventId());
        MDC.put("userId", event.getUserId());
        MDC.put("channel", event.getChannel());

        log.info("Publishing event | eventId={} channel={} userId={}",
                event.getEventId(), event.getChannel(), event.getUserId());

        MDC.clear();


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