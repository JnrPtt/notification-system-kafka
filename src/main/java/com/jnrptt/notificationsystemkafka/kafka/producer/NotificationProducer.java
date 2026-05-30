package com.jnrptt.notificationsystemkafka.kafka.producer;


import com.jnrptt.notificationsystemkafka.kafka.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    public void sendUserRegistered(UserRegisteredEvent event) {
        kafkaTemplate.send("user-registered", event);
    }
}
