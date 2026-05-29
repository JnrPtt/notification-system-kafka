package com.jnrptt.notificationsystemkafka.service;

import com.jnrptt.notificationsystemkafka.dto.UserRequestDTO;
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

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User createUser(UserRequestDTO dto) {
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public Optional<User> updateUser(Long id, UserRequestDTO dto) {
        Optional<User> existingOpt = userRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }

        User existing = existingOpt.get();
        existing.setName(dto.getName());
        existing.setEmail(dto.getEmail());
        // createdAt is intentionally left untouched.
        return Optional.of(userRepository.save(existing));
    }

    @Transactional
    public boolean deleteUserById(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        userRepository.deleteById(id);
        return true;
    }
}
