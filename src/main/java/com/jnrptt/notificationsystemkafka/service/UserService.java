package com.jnrptt.notificationsystemkafka.service;

import com.jnrptt.notificationsystemkafka.dto.UserRequestDTO;
import com.jnrptt.notificationsystemkafka.dto.UserResponseDTO;
import com.jnrptt.notificationsystemkafka.model.User;
import com.jnrptt.notificationsystemkafka.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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
        user.setCreatedAt(LocalDateTime.now());
        return toResponseDTO(userRepository.save(user));
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
        // createdAt is intentionally left untouched.
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
