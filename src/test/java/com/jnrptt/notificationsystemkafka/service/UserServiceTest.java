package com.jnrptt.notificationsystemkafka.service;

import com.jnrptt.notificationsystemkafka.dto.UserRequestDTO;
import com.jnrptt.notificationsystemkafka.dto.UserResponseDTO;
import com.jnrptt.notificationsystemkafka.exception.DuplicateResourceException;
import com.jnrptt.notificationsystemkafka.kafka.event.UserRegisteredEvent;
import com.jnrptt.notificationsystemkafka.kafka.producer.NotificationProducer;
import com.jnrptt.notificationsystemkafka.model.User;
import com.jnrptt.notificationsystemkafka.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationProducer notificationProducer;

    @InjectMocks
    private UserService userService;

    // ---------- create ----------

    @Test
    void createUserRejectsDuplicateEmail() {
        UserRequestDTO dto = new UserRequestDTO("Juan", "juan@example.com");
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserNormalizesEmailAndTrimsName() {
        UserRequestDTO dto = new UserRequestDTO("  Juan  ", "  Juan@MAIL.com ");
        when(userRepository.existsByEmailIgnoreCase("juan@mail.com")).thenReturn(false);
        givenSaveAssignsId(1L);

        UserResponseDTO response = userService.createUser(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Juan");
        assertThat(captor.getValue().getEmail()).isEqualTo("juan@mail.com");
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("juan@mail.com");
    }

    @Test
    void createUserPublishesUserRegisteredEvent() {
        UserRequestDTO dto = new UserRequestDTO("Juan", "Juan@Example.com");
        when(userRepository.existsByEmailIgnoreCase("juan@example.com")).thenReturn(false);
        givenSaveAssignsId(1L);

        userService.createUser(dto);

        ArgumentCaptor<UserRegisteredEvent> captor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(notificationProducer).sendUserRegistered(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getName()).isEqualTo("Juan");
        assertThat(captor.getValue().getEmail()).isEqualTo("juan@example.com");
    }

    @Test
    void createUserRethrowsWhenKafkaFails() {
        UserRequestDTO dto = new UserRequestDTO("Juan", "juan@example.com");
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        givenSaveAssignsId(1L);
        doThrow(new IllegalStateException("kafka down")).when(notificationProducer).sendUserRegistered(any());

        assertThatThrownBy(() -> userService.createUser(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("kafka down");
    }

    // ---------- update ----------

    @Test
    void updateUserReturnsEmptyWhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(userService.updateUser(1L, new UserRequestDTO("Juan", "juan@example.com"))).isEmpty();

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserNormalizesAndSavesChanges() {
        User existing = new User(1L, "Old", "old@example.com", LocalDateTime.of(2026, 6, 1, 10, 0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO response = userService.updateUser(1L, new UserRequestDTO(" New ", " NEW@Example.com "))
                .orElseThrow();

        assertThat(response.getName()).isEqualTo("New");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 6, 1, 10, 0));
    }

    /**
     * Documents current behavior: update does not check for duplicate emails in the service;
     * uniqueness is enforced by the DB constraint (surfaced as 409 by GlobalExceptionHandler).
     */
    @Test
    void updateUserDoesNotCheckEmailUniquenessInService() {
        User existing = new User(1L, "Juan", "juan@example.com", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateUser(1L, new UserRequestDTO("Juan", "taken@example.com"));

        verify(userRepository, never()).existsByEmailIgnoreCase(anyString());
    }

    // ---------- delete ----------

    @Test
    void deleteUserRemovesExistingUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        assertThat(userService.deleteUserById(1L)).isTrue();

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUserReturnsFalseWhenMissing() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThat(userService.deleteUserById(1L)).isFalse();

        verify(userRepository, never()).deleteById(anyLong());
    }

    private void givenSaveAssignsId(Long id) {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(id);
            return user;
        });
    }
}
