package com.vigza.markweave.core.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vigza.markweave.api.dto.Collaboration.CollaboratorVo;
import com.vigza.markweave.common.Constants;
import com.vigza.markweave.common.Result;
import com.vigza.markweave.common.util.JwtUtil;

import com.vigza.markweave.infrastructure.persistence.entity.Collaboration;
import com.vigza.markweave.infrastructure.persistence.entity.User;
import com.vigza.markweave.infrastructure.persistence.mapper.CollaborationMapper;
import com.vigza.markweave.infrastructure.persistence.mapper.UserMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CollaborationServiceImpl implements CollaborationService {

    @Autowired
    private CollaborationMapper collaborationMapper;

    @Autowired
    private UserMapper userMapper;


    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void updatePermission(Long userId, Long docId, Integer permission) {
        Collaboration collaboration = Collaboration.builder()
                .id(userId.toString() + "_" + docId.toString())
                .userId(userId)
                .docId(docId)
                .permission(permission)
                .build();
        collaborationMapper.insertOrUpdate(collaboration);
    }

    @Override
    public Result<?> updatePermission(String token, Long targetUserId, Long docId, Integer permission) {
        User user = jwtUtil.getUserFromToken(token);
        Integer role = getPermission(user.getId(), docId);
        if (role == null || !role.equals(Constants.CollaborationPermission.CREATOR)) {
            return Result.error(403, "您不是该文档的创建者，无权修改协作者权限");
        }
        if (targetUserId == null || docId == null || permission == null ||
                !Constants.CollaborationPermission.isValid(permission)) {
            return Result.error(400, "参数有误");
        }
        updatePermission(targetUserId, docId, permission);
        return Result.success();
    }

    @Override
    public Result<String> createInvitation(String token, Long docId, Integer permission, Integer expTime) {
        User user = jwtUtil.getUserFromToken(token);
        Integer role = getPermission(user.getId(), docId);
        if (role == null || !role.equals(Constants.CollaborationPermission.CREATOR)) {
            return Result.error(403, "您不是该文档的创建者，无权邀请他人协作");
        }
        String invToken = jwtUtil.generateInvitaionToken(docId, permission, expTime);
        return Result.success(invToken);
    }

    @Override
    public Boolean canRead(String token, Long docId) {
        User user = jwtUtil.getUserFromToken(token);
        if (user == null) {
            return false;
        }
        Integer permission = getPermission(user.getId(), docId);
        return permission != null && Constants.CollaborationPermission.isValid(permission);
    }

    @Override
    public Boolean canWrite(String token, Long docId) {
        User user = jwtUtil.getUserFromToken(token);
        if (user == null) {
            return false;
        }
        Integer permission = getPermission(user.getId(), docId);
        return permission != null && (Constants.CollaborationPermission.isValid(permission)
                && !permission.equals(Constants.CollaborationPermission.READ_ONLY));
    }

    public Integer getPermission(Long userId, Long docId) {
        LambdaQueryWrapper<Collaboration> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Collaboration::getUserId, userId)
                .eq(Collaboration::getDocId, docId);
        Collaboration collaboration = collaborationMapper.selectOne(queryWrapper);
        if (collaboration != null) {
            return collaboration.getPermission();
        }
        return null;

    }

    @Override
    public Result<List<CollaboratorVo>> selectCollaboratorsByDocId(String token, Long docId) {
        User user = jwtUtil.getUserFromToken(token);
        if (user == null) {
            return Result.error(401, "未登录");
        }

        if (!canRead(token, docId)) {
            return Result.error(403, "无权限访问此文档");
        }

        LambdaQueryWrapper<Collaboration> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Collaboration::getDocId, docId);
        List<Collaboration> collaborations = collaborationMapper.selectList(queryWrapper);

        Map<Long, Integer> userPermissionMap = new HashMap<>();
        List<Long> userIds = collaborations.stream()
                .map(c -> {
                    userPermissionMap.put(c.getUserId(), c.getPermission());
                    return c.getUserId();
                })
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return Result.success(java.util.Collections.emptyList());
        }

        List<CollaboratorVo> collaborators = userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(User::getId, userIds))
                .stream()
                .map(u -> {
                    CollaboratorVo vo = new CollaboratorVo();
                    vo.setUserId(u.getId());
                    vo.setNickName(u.getNickName());
                    vo.setHeadUrl(u.getHeadUrl());
                    vo.setPermission(userPermissionMap.get(u.getId()));
                    return vo;
                })
                .collect(Collectors.toList());

        return Result.success(collaborators);
    }

    @Override
    public Result<?> acceptInvitation(String userToken, String invToken) {
        User user = jwtUtil.getUserFromToken(userToken);
        if (user == null) {
            return Result.error(401, "未登录");
        }

        if (jwtUtil.validateToken(invToken) == false) {
            return Result.error(400, "邀请链接无效或已过期");
        }

        Long docId = jwtUtil.getDocIdFromInvToken(invToken);
        Integer permission = jwtUtil.getPermissionFromInvToken(invToken);

        LambdaQueryWrapper<Collaboration> existQuery = new LambdaQueryWrapper<>();
        existQuery.eq(Collaboration::getDocId, docId)
                .eq(Collaboration::getUserId, user.getId());
        Collaboration existingCollaboration = collaborationMapper.selectOne(existQuery);
        if (existingCollaboration != null) {
            if (existingCollaboration.getPermission().equals(Constants.CollaborationPermission.READ_ONLY)) {
                updatePermission(user.getId(), docId, permission);
            }
            return Result.success();
        }
        Collaboration collaboration = Collaboration.builder()
                .id(user.getId().toString() + "_" + docId.toString())
                .userId(user.getId())
                .docId(docId)
                .permission(permission)
                .build();
        collaborationMapper.insert(collaboration);
        return Result.success();
    }


}
