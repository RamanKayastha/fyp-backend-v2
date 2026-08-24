package com.stitch.story.backend.services;

import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;


@Service
public class OtpService {
    public String generateOTP(){
        return String.valueOf(
                ThreadLocalRandom.current().nextInt(100000, 1000000)
        );
    }
}

