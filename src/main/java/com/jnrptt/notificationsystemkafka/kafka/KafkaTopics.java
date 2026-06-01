package com.jnrptt.notificationsystemkafka.kafka;

public final class KafkaTopics {
    private KafkaTopics() {
    }

    public static final String USER_REGISTERED = "user-registered";
    public static final String EXPENSE_CREATED = "expense-created";
    public static final String BUDGET_EXCEEDED = "budget-exceeded";
}
