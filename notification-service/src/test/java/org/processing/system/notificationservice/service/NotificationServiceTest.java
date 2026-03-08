package org.processing.system.notificationservice.service;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.processing.system.notificationservice.dto.NotificationRequest;
import org.processing.system.notificationservice.model.NotificationEvent;
import org.processing.system.notificationservice.repository.NotificationLogRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Mock
    private NotificationLogRepository logRepository;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "topic", "notification.events");
    }

    @Test
    void shouldPublishEventToKafkaWhenRequestIsValid() {
        // Arrange
        NotificationRequest request = new NotificationRequest();
        request.setUserId("user-123");
        request.setChannel("EMAIL");
        request.setRecipient("test@example.com");
        request.setSubject("Test");
        request.setMessage("Hello");

        CompletableFuture<SendResult<String, NotificationEvent>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        // Act
        notificationService.send(request);

        // Assert
        verify(kafkaTemplate, times(1)).send(
                eq("notification.events"),
                eq("user-123"),
                any(NotificationEvent.class)
        );
    }

    @Test
    void shouldPersistAuditLogAfterSuccessfulPublish() {
        // Arrange
        NotificationRequest request = new NotificationRequest();
        request.setUserId("user-123");
        request.setChannel("EMAIL");
        request.setRecipient("test@example.com");
        request.setMessage("Hello");

        SendResult<String, NotificationEvent> sendResult = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("notification.events", 0), 0L, 0, 0L, 0, 0);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        CompletableFuture<SendResult<String, NotificationEvent>> future =
                CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        // Act
        notificationService.send(request);

        // Assert
        verify(logRepository, times(1)).save(any());
    }
}