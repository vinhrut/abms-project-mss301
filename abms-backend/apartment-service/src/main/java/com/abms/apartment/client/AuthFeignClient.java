package com.abms.apartment.client;

import com.abms.apartment.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service", url = "${services.auth-service.url:http://localhost:8081}")
public interface AuthFeignClient {

    @GetMapping("/api/v1/users/{userId}")
    UserResponse getUserById(@PathVariable("userId") String userId, @RequestHeader("Authorization") String authorizationHeader);
}
