package com.example.springbootauth.service;

import com.example.springbootauth.domain.User;
import com.example.springbootauth.exception.domain.EmailExistException;
import com.example.springbootauth.exception.domain.UserNameExistException;

import java.util.List;

public interface UserService {

    User register(String firstName, String lastName, String username, String email) throws EmailExistException, UserNameExistException;

    List<User> getUsers();

    User findUserByUserName(String username);

    User findUserByEmail(String email);

}
