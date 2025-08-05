package com.intranet.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.UserDTO;
import com.intranet.dto.UserSDTO;
import com.intranet.security.CurrentUser;

// @CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class UserController {

    // @GetMapping("/me")
    // public UserDTO getUserInfo(@CurrentUser UserDTO user) {
    //     return user;
    // }

    @GetMapping("/users")
    public static List<UserSDTO> getAllMockUsers() {
        return Arrays.asList(
        new UserSDTO(1L, "Ajay Kumar"),
        new UserSDTO(2L, "Sonal Mehta"),
        new UserSDTO(3L, "Rahul Sharma"),
        new UserSDTO(4L, "Nikita Das"),
        new UserSDTO(101L, "Pankaj Kumar"),
        new UserSDTO(102L, "Amit Kumar"),
        new UserSDTO(103L, "Rohit Sharma"),
        new UserSDTO(110L, "Alice Johnson"),
        new UserSDTO(112L, "Bob Smith"),
        new UserSDTO(113L, "Carol Williams"),
        new UserSDTO(114L, "David Lee"),
        new UserSDTO(115L, "Eva Brown"),
        new UserSDTO(116L, "John Doe"),
        new UserSDTO(117L, "Jane Smith"),
        new UserSDTO(118L, "Mark Johnson"),
        new UserSDTO(119L, "Emily Davis"),
        new UserSDTO(120L, "Michael Wilson")
        );
    }

    @GetMapping("/me")
    public UserDTO getUserInfo(@CurrentUser UserDTO user) {
        return user;
    }
    
}
