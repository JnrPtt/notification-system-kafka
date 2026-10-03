package com.jnrptt.notificationsystemkafka.kafka.consumer;

import com.jnrptt.notificationsystemkafka.kafka.event.BudgetExceededEvent;
import com.jnrptt.notificationsystemkafka.kafka.event.UserRegisteredEvent;
import com.jnrptt.notificationsystemkafka.notifications.EmailNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private EmailNotificationService emailNotificationService;

    @InjectMocks
    private NotificationConsumer consumer;

    private final UserRegisteredEvent userEvent =
            new UserRegisteredEvent(1L, "Juan", "juan@example.com", LocalDateTime.now());
    private final BudgetExceededEvent budgetEvent = new BudgetExceededEvent(
            1L, "juan@example.com", "food", new BigDecimal("100"), new BigDecimal("150"), "2026-06");

    @Test
    void handleUserRegisteredSendsWelcomeEmail() {
        consumer.handleUserRegistered(userEvent);

        verify(emailNotificationService).sendWelcomeEmail("juan@example.com", "Juan");
    }

    @Test
    void handleBudgetExceededSendsBudgetEmail() {
        consumer.handleBudgetExceeded(budgetEvent);

        verify(emailNotificationService).sendBudgetExceededEmail("juan@example.com");
    }

    @Test
    void emailFailuresPropagateSoRetryableTopicCanRetry() {
        doThrow(new IllegalStateException("smtp down"))
                .when(emailNotificationService).sendWelcomeEmail("juan@example.com", "Juan");

        assertThatThrownBy(() -> consumer.handleUserRegistered(userEvent))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dltHandlersOnlyLogAndDoNotSendEmails() {
        assertThatCode(() -> {
            consumer.handleUserRegisteredDlt(userEvent);
            consumer.handleBudgetExceededDlt(budgetEvent);
        }).doesNotThrowAnyException();

        verifyNoInteractions(emailNotificationService);
    }
}
