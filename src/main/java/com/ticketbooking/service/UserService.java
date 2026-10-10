package com.ticketbooking.service;

import com.ticketbooking.dto.UserRequest;
import com.ticketbooking.dto.UserResponse;
import com.ticketbooking.entity.User;
import com.ticketbooking.exception.ConflictException;
import com.ticketbooking.exception.ResourceNotFoundException;
import com.ticketbooking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("A user with email " + email + " already exists");
        }
        User user = userRepository.save(new User(request.name().trim(), email));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return UserResponse.from(findUser(id));
    }

    User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
