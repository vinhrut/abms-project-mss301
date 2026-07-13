package com.abms.auth.service;

import com.abms.auth.dto.CreateManagerRequest;
import com.abms.auth.dto.CreateUserRequest;
import com.abms.auth.dto.UserResponse;
import java.util.List;
import java.util.UUID;

public interface UserManagementService {

    UserResponse createManager(String authorizationHeader, CreateManagerRequest request);

    UserResponse createUser(String authorizationHeader, CreateUserRequest request);

    List<UserResponse> getUsers(String authorizationHeader);

    UserResponse getUserById(String authorizationHeader, UUID userId);

    UserResponse lockUser(String authorizationHeader, UUID userId);

    UserResponse unlockUser(String authorizationHeader, UUID userId);
}