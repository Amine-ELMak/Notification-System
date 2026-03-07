package org.processing.system.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NotificationRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "channel is required")
    @Pattern(regexp = "EMAIL|SMS", message = "channel must be EMAIL or SMS")
    private String channel;

    @NotBlank(message = "recipient is required")
    private String recipient;

    private String subject;

    @NotBlank(message = "message is required")
    private String message;

}