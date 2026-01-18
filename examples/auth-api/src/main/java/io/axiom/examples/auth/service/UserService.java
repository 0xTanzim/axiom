package io.axiom.examples.auth.service;

import io.axiom.di.Service;
import io.axiom.examples.auth.domain.*;
import io.axiom.examples.auth.repository.UserRepository;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

/**
 * User service for CRUD operations.
 *
 * <p>Demonstrates data retrieval patterns with Axiom.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    @Inject
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns all users (without password hashes).
     */
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
            .map(User::toResponse)
            .toList();
    }

    /**
     * Finds user by ID.
     */
    public Optional<UserResponse> findById(long id) {
        return userRepository.findById(id)
            .map(User::toResponse);
    }

    /**
     * Finds user by username.
     */
    public Optional<UserResponse> findByUsername(String username) {
        return userRepository.findByUsername(username)
            .map(User::toResponse);
    }

    /**
     * Returns user count.
     */
    public long count() {
        return userRepository.count();
    }

    /**
     * Deletes user by ID.
     */
    public boolean deleteById(long id) {
        if (userRepository.findById(id).isEmpty()) {
            return false;
        }
        userRepository.deleteById(id);
        return true;
    }
}
