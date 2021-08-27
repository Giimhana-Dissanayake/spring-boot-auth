package com.example.springbootauth.service.impl;

import com.example.springbootauth.constant.UserImplConstant;
import com.example.springbootauth.domain.User;
import com.example.springbootauth.domain.UserPrincipal;
import com.example.springbootauth.exception.domain.EmailExistException;
import com.example.springbootauth.exception.domain.UserNameExistException;
import com.example.springbootauth.repository.UserRepository;
import com.example.springbootauth.service.LoginAttemptService;
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

import static com.example.springbootauth.constant.UserImplConstant.*;
import static com.example.springbootauth.enumaration.Role.ROLE_USER;

@Service
@Transactional
@Qualifier("userDetailsService")
public class UserServiceImpl implements UserService, UserDetailsService {

    private Logger LOGGER = LoggerFactory.getLogger(getClass());
    private UserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private LoginAttemptService loginAttemptService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByUserName(username);

        if (user == null) {
            LOGGER.error(NO_USER_FOUND_BY_USERNAME + username);
            throw new UsernameNotFoundException(NO_USER_FOUND_BY_USERNAME + username);
        } else {
            validateLoginAttempt(user);
            user.setLastLoginDateDisplay(user.getLastLoginDate());
            user.setLastLoginDate(new Date());
            userRepository.save(user);
            UserPrincipal userPrincipal = new UserPrincipal(user);
            LOGGER.info(FOUND_USER_BY_USERNAME + username);
            return userPrincipal;
        }

    }

    private void validateLoginAttempt(User user) {
        if(user.isNotLocked()){
            if(loginAttemptService.hasExceededMaxAttempt(user.getUserName())){
                user.setNotLocked(false);
            }else{
                user.setNotLocked(true);
            }
        }else{
            loginAttemptService.evictUserFromLoginAttemptCache(user.getUserName());
        }
    }

    @Override
    public User register(String firstName, String lastName, String userName, String email) throws EmailExistException, UserNameExistException,UsernameNotFoundException {
        validateNewUserNameAndEmail(StringUtils.EMPTY, userName, email);
        User user = new User();
        user.setUserId(generateUserId());
        String password = generatePassword();
        String encodedPassword = encodePassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUserName(userName);
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
        return user;
    }

    private String getTemporaryProfileImageUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(DEFAULT_USER_IMAGE_PATH).toUriString();
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

        User userByNewUserName = findUserByUserName(newUserName);
        User userByNewEmail = findUserByEmail(newEmail);


        if (StringUtils.isNotBlank(currentUserName)) {
            User currentUser = findUserByUserName(currentUserName);
            if (currentUser == null) {
                throw new UsernameNotFoundException(NO_USER_FOUND_BY_USERNAME + currentUserName);
            }

            if (userByNewUserName != null && !currentUser.getId().equals(userByNewUserName.getId())) {
                throw new UserNameExistException(UserImplConstant.USERNAME_ALREADY_EXISTS);
            }

            if (userByNewEmail != null && !currentUser.getId().equals(userByNewEmail.getId())) {
                throw new EmailExistException(EMAIL_ALREADY_EXISTS);
            }
            return currentUser;
        } else {

            if (userByNewUserName != null) {
                throw new UserNameExistException(USERNAME_ALREADY_EXISTS);
            }

            if (userByNewEmail != null) {
                throw new EmailExistException(EMAIL_ALREADY_EXISTS);
            }
            return null;
        }
    }

    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public User findUserByUserName(String username) {
        return userRepository.findUserByUserName(username);
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findUserByEmail(email);
    }
}
