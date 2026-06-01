package com.jnrptt.notificationsystemkafka.kafka.producer;


import com.jnrptt.notificationsystemkafka.kafka.event.BudgetExceededEvent;
import com.jnrptt.notificationsystemkafka.kafka.event.ExpenseCreatedEvent;
import com.jnrptt.notificationsystemkafka.kafka.event.UserRegisteredEvent;
import com.jnrptt.notificationsystemkafka.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendUserRegistered(UserRegisteredEvent event) {
        kafkaTemplate.send(KafkaTopics.USER_REGISTERED, event);
    }

    public void sendExpenseCreated(ExpenseCreatedEvent event) {
        kafkaTemplate.send(KafkaTopics.EXPENSE_CREATED, event);
    }

    public void sendBudgetExceeded(BudgetExceededEvent event) {
        kafkaTemplate.send(KafkaTopics.BUDGET_EXCEEDED, event);
    }
}
