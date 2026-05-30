package com.jnrptt.notificationsystemkafka.kafka.consumer;

import com.jnrptt.notificationsystemkafka.kafka.event.UserRegisteredEvent;
import com.jnrptt.notificationsystemkafka.notifications.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final EmailNotificationService emailNotificationService;

    @KafkaListener(topics = "user-registered", groupId = "notification-group")
    public void handleUserRegistered( UserRegisteredEvent event) {
        emailNotificationService.sendWelcomeEmail(event.getEmail(), event.getName());
//        System.out.println("Evento recibido:" + event.getName() + " - " + event.getEmail());
    }
}
