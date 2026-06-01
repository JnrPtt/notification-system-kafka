package com.jnrptt.notificationsystemkafka.service;

import com.jnrptt.notificationsystemkafka.dto.UserRequestDTO;
import com.jnrptt.notificationsystemkafka.exception.DuplicateResourceException;
import com.jnrptt.notificationsystemkafka.kafka.producer.NotificationProducer;
import com.jnrptt.notificationsystemkafka.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationProducer notificationProducer;

    @InjectMocks
    private UserService userService;

    @Test
    void createUserRejectsDuplicateEmail() {
        UserRequestDTO dto = new UserRequestDTO("Juan", "juan@example.com");
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }
}
