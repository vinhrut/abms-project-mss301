package com.abms.apartment.client;

import com.abms.apartment.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "auth-service", url = "${services.auth-service.url:http://localhost:8081}")
public interface AuthFeignClient {

    @GetMapping("/api/v1/users/{userId}")
    UserResponse getUserById(@PathVariable("userId") UUID userId, @RequestHeader("Authorization") String authorizationHeader);
}
