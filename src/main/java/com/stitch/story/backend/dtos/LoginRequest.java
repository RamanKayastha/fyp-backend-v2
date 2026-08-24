package com.stitch.story.backend.dtos;

import lombok.Data;

@Data

public class LoginRequest {
    public String email;

    public String password;
}
