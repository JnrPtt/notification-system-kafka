package com.jnrptt.notificationsystemkafka.kafka.producer;


import com.jnrptt.notificationsystemkafka.kafka.event.BudgetExceededEvent;
import com.jnrptt.notificationsystemkafka.kafka.event.ExpenseCreatedEvent;
import com.jnrptt.notificationsystemkafka.kafka.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendUserRegistered(UserRegisteredEvent event) {
        kafkaTemplate.send("user-registered", event);
    }
    public void sendExpenseCreated(ExpenseCreatedEvent event) {
        kafkaTemplate.send("expense-created", event);
    }
    public void sendBudgetExceeded(BudgetExceededEvent event) {
        kafkaTemplate.send("budget-exceeded", event);
    }
}
