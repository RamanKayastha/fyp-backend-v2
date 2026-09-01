package com.stitch.story.backend.mapper;

import com.stitch.story.backend.dtos.UserDTO;
import com.stitch.story.backend.entities.User;

public class UserMapper {

    //user entity to User DTO conversion
    public static UserDTO toDTO (User user){
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getContact(),
                user.getAddress(),
                user.getShopName(),
                user.getRole(),
                user.getAuthProvider()
        );
    }

    //user DTO to user entity conversion
    public static User toEntity(UserDTO userDTO){
        return new User(
                userDTO.getId(),
                userDTO.getUsername(),
                userDTO.getEmail(),
                userDTO.getPassword(),
                userDTO.getContact(),
                userDTO.getAddress(),
                userDTO.getShopName(),
                userDTO.getRole(),
                userDTO.getAuthProvider()
        );
    }
}
