package com.jnrptt.notificationsystemkafka.service;

import com.jnrptt.notificationsystemkafka.dto.UserRequestDTO;
import com.jnrptt.notificationsystemkafka.dto.UserResponseDTO;
import com.jnrptt.notificationsystemkafka.kafka.event.UserRegisteredEvent;
import com.jnrptt.notificationsystemkafka.kafka.producer.NotificationProducer;
import com.jnrptt.notificationsystemkafka.model.User;
import com.jnrptt.notificationsystemkafka.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final NotificationProducer notificationProducer;

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public Optional<UserResponseDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::toResponseDTO);
    }

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO dto) {
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        User saved = userRepository.save(user);

        try {
            notificationProducer.sendUserRegistered(new UserRegisteredEvent(
                    saved.getId(),
                    saved.getName(),
                    saved.getEmail(),
                    saved.getCreatedAt()
            ));
        } catch (Exception e) {
            log.error("Error enviando UserRegisteredEvent a Kafka. userId={}, email={}",
                    saved.getId(), saved.getEmail(), e);
            throw e;
        }
        return toResponseDTO(saved);
    }

    @Transactional
    public Optional<UserResponseDTO> updateUser(Long id, UserRequestDTO dto) {
        Optional<User> existingOpt = userRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }

        User existing = existingOpt.get();
        existing.setName(dto.getName());
        existing.setEmail(dto.getEmail());
        return Optional.of(toResponseDTO(userRepository.save(existing)));
    }

    @Transactional
    public boolean deleteUserById(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        userRepository.deleteById(id);
        return true;
    }

    private UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}
