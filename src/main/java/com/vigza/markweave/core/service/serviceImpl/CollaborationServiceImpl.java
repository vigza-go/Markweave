package com.vigza.markweave.core.service.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.vigza.markweave.api.dto.RecentDocVO;
import com.vigza.markweave.common.Constants;
import com.vigza.markweave.common.util.IdGenerator;
import com.vigza.markweave.common.util.JwtUtil;
import com.vigza.markweave.core.service.CollaborationService;
import com.vigza.markweave.infrastructure.persistence.entity.Collaboration;
import com.vigza.markweave.infrastructure.persistence.entity.FsNode;
import com.vigza.markweave.infrastructure.persistence.entity.User;
import com.vigza.markweave.infrastructure.persistence.mapper.CollaborationMapper;
import com.vigza.markweave.infrastructure.persistence.mapper.FsNodeMapper;
import com.vigza.markweave.infrastructure.persistence.mapper.UserMapper;


@Service
public class CollaborationServiceImpl implements CollaborationService{
    @Autowired
    private CollaborationMapper collaborationMapper;

    @Autowired
    private FsNodeMapper fsNodeMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public void insert(Collaboration collaboration) {
        collaborationMapper.insert(collaboration);
    }

    public Integer checkPermission(Long userId, Long docId) {
        LambdaQueryWrapper<Collaboration> lambdaQueryWrapper = new LambdaQueryWrapper<Collaboration>()
                .eq(Collaboration::getDocId, docId)
                .eq(Collaboration::getUserId, userId);
        Collaboration collaboration = collaborationMapper.selectOne(lambdaQueryWrapper);
        return collaboration != null ? collaboration.getRole() : 0;
    }

    @Override
    public Boolean canRead(String token, Long docId) {
        User user = jwtUtil.getUserFromToken(token);
        Long userId = user.getId();
        Integer value = checkPermission(userId, docId);
        if (value > 3 || value < 1) {
            return false;
        }
        return true;
    }

    @Override
    public Boolean canWrite(String token, Long docId) {
        User user = jwtUtil.getUserFromToken(token);
        Long userId = user.getId();
        Integer value = checkPermission(userId, docId);
        return value == Constants.CollaborationRole.CREATOR || value == Constants.CollaborationRole.READ_WRITE;
    }

    public void updateViewTime(Long userId, Long docId) {
        LambdaUpdateWrapper<Collaboration> lambdaUpdateWrapper = new LambdaUpdateWrapper<Collaboration>()
                .eq(Collaboration::getDocId, docId)
                .eq(Collaboration::getUserId, userId)
                .set(Collaboration::getLastViewTime, LocalDateTime.now());
        collaborationMapper.update(lambdaUpdateWrapper);
    }

    public List<Long> selectCollaboratorsByDoc(String token, Long docId) {
        Long userId = jwtUtil.getUserFromToken(token).getId();
        if (!canRead(userId, docId)) {
            throw new RuntimeException("没有权限查看该文档的协作者");
        }
        LambdaQueryWrapper<Collaboration> queryWrapper = new LambdaQueryWrapper<Collaboration>()
                .eq(Collaboration::getDocId, docId)
                .select(Collaboration::getUserId);
        return collaborationMapper.selectList(queryWrapper).stream()
                .map(Collaboration::getUserId)
                .collect(Collectors.toList());
    }

    public List<RecentDocVO> selectRecentDocs(String token, int limit) {
        Long userId = jwtUtil.getUserFromToken(token).getId();
        return selectRecentDocsByUserId(userId, limit);
    }

    private List<RecentDocVO> selectRecentDocsByUserId(Long userId, int limit) {
        LambdaQueryWrapper<Collaboration> queryWrapper = new LambdaQueryWrapper<Collaboration>()
                .eq(Collaboration::getUserId, userId)
                .orderByDesc(Collaboration::getLastViewTime)
                .last("LIMIT " + limit);
        List<Collaboration> collaborations = collaborationMapper.selectList(queryWrapper);

        List<Long> docIds = collaborations.stream()
                .map(Collaboration::getDocId)
                .collect(Collectors.toList());

        if (docIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        LambdaQueryWrapper<FsNode> fsQueryWrapper = new LambdaQueryWrapper<FsNode>()
                .in(FsNode::getDocId, docIds);
        List<FsNode> fsNodes = fsNodeMapper.selectList(fsQueryWrapper);
        Map<Long, FsNode> fsNodeMap = fsNodes.stream()
                .collect(Collectors.toMap(FsNode::getDocId, n -> n));

        List<Long> ownerIds = fsNodes.stream()
                .map(FsNode::getUserId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<User>()
                .in(User::getId, ownerIds);
        List<User> users = userMapper.selectList(userQueryWrapper);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, n -> n));

        return collaborations.stream()
                .map(collab -> {
                    FsNode fsNode = fsNodeMap.get(collab.getDocId());
                    User owner = fsNode != null ? userMap.get(fsNode.getUserId()) : null;
                    return RecentDocVO.builder()
                            .docId(collab.getDocId())
                            .docName(fsNode != null ? fsNode.getName() : "Unknown")
                            .ownerId(fsNode != null ? fsNode.getUserId() : null)
                            .ownerName(owner != null ? owner.getNickname() : "Unknown")
                            .role(collab.getRole())
                            .lastViewTime(collab.getLastViewTime())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public String createInvitation(String token, Long docId, Integer permission, Integer expTime) {
        Long userId = jwtUtil.getUserFromToken(token).getId();
        Integer currentRole = checkPermission(userId, docId);
        if (currentRole != Constants.CollaborationRole.CREATOR) {
            throw new RuntimeException("只有创建者可以生成邀请链接");
        }

        if (!Constants.CollaborationRole.isValid(permission)) {
            throw new RuntimeException("无效的权限值");
        }

        long expirationMillis = expTime * 3600 * 1000L;
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("docId", docId);
        payload.put("permission", permission);
        payload.put("exp", System.currentTimeMillis() + expirationMillis);
        payload.put("type", "invitation");

        String invToken = jwtUtil.generateInvitationToken(payload);
        return baseUrl + "/invitation/accept?token=" + invToken;
    }

    public void acceptInvitation(String userToken, String invToken) {
        Long userId = jwtUtil.getUserFromToken(userToken).getId();
        Map<String, Object> payload = jwtUtil.getInvitationPayload(invToken);
        if (payload == null) {
            throw new RuntimeException("邀请链接无效或已过期");
        }

        Long docId = (Long) payload.get("docId");
        Integer permission = (Integer) payload.get("permission");

        LambdaQueryWrapper<Collaboration> queryWrapper = new LambdaQueryWrapper<Collaboration>()
                .eq(Collaboration::getUserId, userId)
                .eq(Collaboration::getDocId, docId);

        if (collaborationMapper.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("您已经是该文档的协作者");
        }

        Collaboration collaboration = Collaboration.builder()
                .id(IdGenerator.nextId())
                .userId(userId)
                .docId(docId)
                .role(permission)
                .createTime(LocalDateTime.now())
                .build();
        collaborationMapper.insert(collaboration);
    }

    public void removeCollaborator(Long operatorId, Long docId, Long targetUserId) {
        Integer operatorRole = checkPermission(operatorId, docId);
        if (operatorRole != Constants.CollaborationRole.CREATOR) {
            throw new RuntimeException("只有创建者可以移除协作者");
        }

        if (targetUserId.equals(operatorId)) {
            throw new RuntimeException("不能移除自己");
        }

        LambdaQueryWrapper<Collaboration> queryWrapper = new LambdaQueryWrapper<Collaboration>()
                .eq(Collaboration::getDocId, docId)
                .eq(Collaboration::getUserId, targetUserId);
        collaborationMapper.delete(queryWrapper);
    }

    public void updatePermission(String token, Long docId, Long targetUserId, Integer permission) {
        Long operatorId = jwtUtil.getUserFromToken(token).getId();
        updateCollaboratorRole(operatorId, docId, targetUserId, permission);
    }

    public void updateCollaboratorRole(Long operatorId, Long docId, Long targetUserId, Integer newRole) {
        Integer operatorRole = checkPermission(operatorId, docId);
        if (operatorRole != Constants.CollaborationRole.CREATOR) {
            throw new RuntimeException("只有创建者可以修改权限");
        }

        if (!Constants.CollaborationRole.isValid(newRole)) {
            throw new RuntimeException("无效的权限值");
        }

        if (targetUserId.equals(operatorId)) {
            throw new RuntimeException("不能修改自己的权限");
        }

        LambdaUpdateWrapper<Collaboration> updateWrapper = new LambdaUpdateWrapper<Collaboration>()
                .eq(Collaboration::getDocId, docId)
                .eq(Collaboration::getUserId, targetUserId)
                .set(Collaboration::getRole, newRole);
        collaborationMapper.update(updateWrapper);
    }
}
