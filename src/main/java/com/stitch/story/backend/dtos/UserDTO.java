package com.stitch.story.backend.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stitch.story.backend.entities.enums.AuthProvider;
import com.stitch.story.backend.entities.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private Long id;
    private String username;
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String contact;
    private String address;
    private String shopName;
    private Role role;
    private AuthProvider authProvider;
}
