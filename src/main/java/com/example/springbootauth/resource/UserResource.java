package com.example.springbootauth.resource;

import com.example.springbootauth.domain.User;
import com.example.springbootauth.exception.ExceptionHandling;
import com.example.springbootauth.exception.domain.EmailExistException;
import com.example.springbootauth.exception.domain.UserNameExistException;
import com.example.springbootauth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/", "/user"})
public class UserResource extends ExceptionHandling {

    private UserService userService;

    @Autowired
    public UserResource(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) throws EmailExistException, UserNameExistException {
        User newUser = userService.register(user.getFirstName(), user.getLastName(), user.getUserName(), user.getEmail());
        return new ResponseEntity<>(newUser, HttpStatus.OK);
    }


}
