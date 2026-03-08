package org.processing.system.smsconsumer.sender;

import lombok.extern.slf4j.Slf4j;
import org.processing.system.smsconsumer.model.NotificationEvent;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SmsSender {

    public void send(NotificationEvent event) {
        log.info("Sending sms | to={}",
                event.getRecipient());

        // to simulate failure and trigger the full retry + DLQ flow
        // throw new RuntimeException("SMTP connection timeout");
    }
}