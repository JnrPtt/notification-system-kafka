package com.jnrptt.notificationsystemkafka.kafka.consumer;

import com.jnrptt.notificationsystemkafka.kafka.event.BudgetExceededEvent;
import com.jnrptt.notificationsystemkafka.kafka.event.UserRegisteredEvent;
import com.jnrptt.notificationsystemkafka.notifications.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private final EmailNotificationService emailNotificationService;

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 2000), dltStrategy = DltStrategy.FAIL_ON_ERROR)
    @KafkaListener(topics = "user-registered", groupId = "notification-group")
    public void handleUserRegistered(UserRegisteredEvent event) {
        emailNotificationService.sendWelcomeEmail(event.getEmail(), event.getName());
    }

    @DltHandler
    public void handleUserRegisteredDlt(UserRegisteredEvent event) {
        log.error("Mensaje fallido en DLT user-registered: {}", event.getEmail());
    }

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 2000), dltStrategy = DltStrategy.FAIL_ON_ERROR)
    @KafkaListener(topics = "budget-exceeded", groupId = "notification-group")
    public void handleBudgetExceeded(BudgetExceededEvent event) {
        emailNotificationService.sendBudgetExceededEmail(event.getUserEmail());
    }

    @DltHandler
    public void handleBudgetExceededDlt(BudgetExceededEvent event) {
        log.error("Mensaje fallido en DLT budget-exceeded: {}", event.getUserEmail());
    }
}