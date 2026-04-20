package com.taskapi.repository;

import com.taskapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data gera o SQL automaticamente pelo nome do método
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
