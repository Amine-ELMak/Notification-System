package org.processing.system.emailconsumer.sender;

import lombok.extern.slf4j.Slf4j;
import org.processing.system.emailconsumer.model.NotificationEvent;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailSender {

    public void send(NotificationEvent event) {
        log.info("Sending email | to={} subject={} eventId={}",
                event.getRecipient(), event.getSubject(), event.getEventId());

        // to simulate failure and trigger the full retry + DLQ flow
        // throw new RuntimeException("SMTP connection timeout");
    }
}