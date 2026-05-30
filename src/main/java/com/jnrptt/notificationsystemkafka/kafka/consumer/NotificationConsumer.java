package com.jnrptt.notificationsystemkafka.kafka.consumer;

import com.jnrptt.notificationsystemkafka.kafka.event.UserRegisteredEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationConsumer {

    @KafkaListener(topics = "user-registered", groupId = "notification-group")
    public void handleUserRegistered( UserRegisteredEvent event) {
        System.out.println("Evento recibido:" + event.getName() + " - " + event.getEmail());
    }
}
