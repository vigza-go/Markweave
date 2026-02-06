package com.vigza.markweave.api.controller;

import com.vigza.markweave.api.dto.Auth.AuthResponse;
import com.vigza.markweave.api.dto.Auth.LoginRequest;
import com.vigza.markweave.api.dto.Auth.RegisterRequest;
import com.vigza.markweave.common.Result;
import com.vigza.markweave.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController{

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request){
        return userService.register(request);
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request){
        return userService.login(request);
    }

    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader("Authorization") String token){
        return userService.logout(token);
    }



}