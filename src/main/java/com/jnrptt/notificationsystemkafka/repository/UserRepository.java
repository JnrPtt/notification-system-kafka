package com.jnrptt.notificationsystemkafka.repository;

import com.jnrptt.notificationsystemkafka.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
