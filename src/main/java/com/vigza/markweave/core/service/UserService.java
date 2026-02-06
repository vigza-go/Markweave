package com.vigza.markweave.core.service;

import com.vigza.markweave.api.dto.Auth.AuthResponse;
import com.vigza.markweave.api.dto.Auth.LoginRequest;
import com.vigza.markweave.api.dto.Auth.RegisterRequest;
import com.vigza.markweave.common.Result;
import com.vigza.markweave.infrastructure.persistence.entity.User;

public interface UserService {

    Result<AuthResponse> register(RegisterRequest request);

    Result<AuthResponse> login(LoginRequest request);

    Result<?> logout(String token);
}
