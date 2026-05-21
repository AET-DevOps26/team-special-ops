package com.tso.userprogress.service;

import com.tso.userprogress.entity.User;
import com.tso.userprogress.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Find user by email.
   *
   * @param email the email
   * @return Optional containing the user if found
   */
  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  /**
   * Find user by ID.
   *
   * @param userId the user ID
   * @return Optional containing the user if found
   */
  public Optional<User> findById(UUID userId) {
    return userRepository.findById(userId);
  }

  /**
   * Check if email exists.
   *
   * @param email the email
   * @return true if exists, false otherwise
   */
  public boolean emailExists(String email) {
    return userRepository.existsByEmail(email);
  }

  /**
   * Create a new user.
   *
   * @param email the email
   * @param password the plain password (will be hashed)
   * @return the created user
   */
  public User createUser(String email, String password) {
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(password));
    return userRepository.save(user);
  }

  /**
   * Verify password against stored hash.
   *
   * @param rawPassword the plain password
   * @param hashedPassword the hashed password
   * @return true if password matches, false otherwise
   */
  public boolean verifyPassword(String rawPassword, String hashedPassword) {
    return passwordEncoder.matches(rawPassword, hashedPassword);
  }
}
