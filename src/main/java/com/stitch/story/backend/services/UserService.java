package com.stitch.story.backend.services;

import com.stitch.story.backend.dtos.UserDTO;

import java.util.List;

public interface UserService {
    //create user
    UserDTO createUser(UserDTO userDTO);

    //get user by id
    UserDTO getUserByID(Long id);

    //update user by id
    UserDTO updateUser(UserDTO userDTO, Long id);

    //delete user
    void deleteUser(Long id);

    //get all users
    List<UserDTO> getALlUsers();
}
