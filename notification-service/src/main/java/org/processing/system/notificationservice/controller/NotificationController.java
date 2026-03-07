package org.processing.system.notificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.processing.system.notificationservice.dto.NotificationRequest;
import org.processing.system.notificationservice.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Send notifications via email or SMS")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @Operation(summary = "Send a notification", description = "Publishes a notification event to Kafka")
    @ApiResponse(responseCode = "202", description = "Event accepted for processing")
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
    public ResponseEntity<Void> send(@Valid @RequestBody NotificationRequest request) {
        notificationService.send(request);
        return ResponseEntity.accepted().build();
    }
}