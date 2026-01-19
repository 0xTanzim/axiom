package playground.di.service;

import io.axiom.di.Service;
import jakarta.inject.Inject;
import playground.di.domain.*;
import playground.di.repository.UserRepository;

import java.util.*;

/**
 * User service for CRUD operations.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    @Inject
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
            .map(User::toResponse)
            .toList();
    }

    public Optional<UserResponse> findById(long id) {
        return userRepository.findById(id)
            .map(User::toResponse);
    }

    public long count() {
        return userRepository.count();
    }
}
