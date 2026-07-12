package com.abms.auth.service;

import com.abms.auth.dto.AuthResponse;
import com.abms.auth.dto.LoginRequest;
import com.abms.auth.dto.RegisterRequest;
import java.util.List;
import java.util.UUID;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    List<AuthResponse> getPendingResidents();

    AuthResponse approveResident(UUID userId);

    AuthResponse rejectResident(UUID userId);
}