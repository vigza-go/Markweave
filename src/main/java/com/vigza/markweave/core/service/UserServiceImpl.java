package com.vigza.markweave.core.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vigza.markweave.api.dto.Auth.AuthResponse;
import com.vigza.markweave.api.dto.Auth.LoginRequest;
import com.vigza.markweave.api.dto.Auth.RegisterRequest;
import com.vigza.markweave.common.Constants;
import com.vigza.markweave.common.Result;
import com.vigza.markweave.common.util.IdGenerator;
import com.vigza.markweave.common.util.JwtUtil;
import com.vigza.markweave.infrastructure.persistence.entity.User;
import com.vigza.markweave.infrastructure.persistence.mapper.UserMapper;
import com.vigza.markweave.infrastructure.service.RedisService;

import cn.hutool.crypto.digest.BCrypt;


@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private FileSystemService fileSystemService;

    @Autowired
    private JwtUtil jwtUtils;

    private AuthResponse generateAuthResponse(User user) {
        user.setPassword(null);
        user.setSalt(null);
        String token = jwtUtils.generateToken(user);


        AuthResponse.UserDTO userDTO = AuthResponse.UserDTO.builder()
                .id(user.getId())
                .account(user.getAccount())
                .nickname(user.getNickName())
                .headUrl(user.getHeadUrl())
                .type(user.getType())
                .userShareSpaceNodeId(user.getUserShareSpaceNodeId())
                .userSpaceNodeId(user.getUserSpaceNodeId())
                .build();
        return AuthResponse.builder().token(token).user(userDTO).build();
    }

    @Override
    public Result<AuthResponse> register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return Result.error(400, "两次输入密码不一致");
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getAccount, request.getAccount());
        if (userMapper.selectCount(queryWrapper) > 0) {
            return Result.error(409, "账号已存在");
        }
        User user = new User();
        user.setAccount(request.getAccount());
        String salt = BCrypt.gensalt();
        user.setPassword(BCrypt.hashpw(request.getPassword(), salt ));
        user.setSalt(salt);
        user.setNickName(request.getNickname());
        user.setCreateTime(LocalDateTime.now());
        user.setType(Constants.UserType.NORMAL);
        user.setId(IdGenerator.nextId());
        user.setUserSpaceNodeId(IdGenerator.nextId());
        user.setUserShareSpaceNodeId(IdGenerator.nextId());
        userMapper.insert(user);
        AuthResponse authResponse = generateAuthResponse(user);
        fileSystemService.initUserNodes(authResponse.getToken());
        return Result.success(authResponse);
    }

    @Override
    public Result<AuthResponse> login(LoginRequest request) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getAccount, request.getAccount());
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            return Result.error(500, "账号不存在");
        }
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            return Result.error(500, "密码错误");
        }
        return Result.success(generateAuthResponse(user));
    }


    @Override
    public Result<?> logout(String token){
        if(token != null && jwtUtils.validateToken(token)){
            redisService.addToBlacklist(token);
        }
        return Result.success();
    }
}
