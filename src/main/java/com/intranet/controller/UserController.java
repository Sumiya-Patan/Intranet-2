package com.intranet.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.dto.UserSDTO;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class UserController {

    // @GetMapping("/me")
    // public UserDTO getUserInfo(@CurrentUser UserDTO user) {
    //     return user;
    // }

    @GetMapping("/users")
    public static List<UserSDTO> getAllMockUsers() {
        return Arrays.asList(
        new UserSDTO(1L, "Ajay Kumar", "ajay@example.com"),
        new UserSDTO(2L, "Sonal Mehta", "sonal@example.com"),
        new UserSDTO(3L, "Rahul Sharma", "rahul@example.com"),
        new UserSDTO(4L, "Nikita Das", "nikita@example.com"),
        new UserSDTO(101L, "Pankaj Kumar", "pankaj@example.com"),
        new UserSDTO(102L, "Amit Kumar", "amit@example.com"),
        new UserSDTO(103L, "Rohit Sharma", "rohit@example.com"),
        new UserSDTO(110L, "Alice Johnson", "alice.johnson@example.com"),
        new UserSDTO(112L, "Bob Smith", "bob.smith@example.com"),
        new UserSDTO(113L, "Carol Williams", "carol.williams@example.com"),
        new UserSDTO(114L, "David Lee", "david.lee@example.com"),
        new UserSDTO(115L, "Eva Brown", "eva.brown@example.com"),
        new UserSDTO(116L, "John Doe", "john.doe@example.com")
        );
    }
    
}
