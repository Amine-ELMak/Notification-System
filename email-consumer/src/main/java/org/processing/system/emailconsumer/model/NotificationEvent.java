package org.processing.system.emailconsumer.model;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class NotificationEvent {

    private String eventId;
    private String userId;
    private String channel;
    private String recipient;
    private String subject;
    private String message;
    private LocalDateTime createdAt;

}