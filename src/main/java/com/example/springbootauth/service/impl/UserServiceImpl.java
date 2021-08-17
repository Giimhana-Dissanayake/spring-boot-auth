package com.example.springbootauth.service.impl;

import com.example.springbootauth.domain.User;
import com.example.springbootauth.domain.UserPrincipal;
import com.example.springbootauth.exception.domain.EmailExistException;
import com.example.springbootauth.exception.domain.UserNameExistException;
import com.example.springbootauth.repository.UserRepository;
import com.example.springbootauth.service.UserService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

import static com.example.springbootauth.enumaration.Role.ROLE_USER;

@Service
@Transactional
@Qualifier("userDetailsService")
public class UserServiceImpl implements UserService, UserDetailsService {

    private Logger LOGGER = LoggerFactory.getLogger(getClass());
    private UserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByUserName(username);

        if (user == null) {
            LOGGER.error("User not found by username: " + username);
            throw new UsernameNotFoundException("User not found by username: " + username);
        } else {
            user.setLastLoginDateDisplay(user.getLastLoginDate());
            user.setLastLoginDate(new Date());
            userRepository.save(user);
            UserPrincipal userPrincipal = new UserPrincipal(user);
            LOGGER.info("Returning found user by username: " + username);
            return userPrincipal;
        }

    }

    @Override
    public User register(String firstName, String lastName, String userName, String email) throws EmailExistException, UserNameExistException {
        validateNewUserNameAndEmail(StringUtils.EMPTY, userName, email);
        User user = new User();
        user.setUserId(generateUserId());
        String password = generatePassword();
        String encodedPassword = encodePassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setJoinDate(new Date());
        user.setPassword(encodedPassword);
        user.setActive(true);
        user.setNotLocked(true);
        user.setRole(ROLE_USER.name());
        user.setAuthorities(ROLE_USER.getAuthorities());
        user.setProfileImageUrl(getTemporaryProfileImageUrl());
        userRepository.save(user);
        LOGGER.info("New user password: " + password);
        return null;
    }

    private String getTemporaryProfileImageUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/image/profile/temp").toUriString();
    }

    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    private String generatePassword() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    private String generateUserId() {
        return RandomStringUtils.randomNumeric(10);
    }

    private User validateNewUserNameAndEmail(String currentUserName, String newUserName, String newEmail) throws
            UsernameNotFoundException, UserNameExistException, EmailExistException {
        if (StringUtils.isNotBlank(currentUserName)) {
            User currentUser = findUserByUserName(currentUserName);
            if (currentUser == null) {
                throw new UsernameNotFoundException("No user found by username " + currentUserName);
            }
            User userByNewUserName = findUserByUserName(newUserName);
            if (userByNewUserName != null && !currentUser.getId().equals(userByNewUserName.getId())) {
                throw new UserNameExistException("Username already exists");
            }
            User userByNameEmail = findUserByEmail(newEmail);
            if (userByNameEmail != null && !currentUser.getId().equals(userByNameEmail.getId())) {
                throw new EmailExistException("Email already exists");
            }
            return currentUser;
        } else {
            User userByUserName = findUserByUserName(newUserName);
            if (userByUserName != null) {
                throw new UserNameExistException("Username already exists");
            }
            User userByEmail = findUserByEmail(newEmail);
            if (userByUserName != null) {
                throw new EmailExistException("Email already exists");
            }
            return null;
        }
    }

    @Override
    public List<User> getUsers() {
        return null;
    }

    @Override
    public User findUserByUserName(String username) {
        return null;
    }

    @Override
    public User findUserByEmail(String email) {
        return null;
    }
}
