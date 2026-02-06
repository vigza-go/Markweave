package com.vigza.markweave.core.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vigza.markweave.common.Constants;
import com.vigza.markweave.common.Result;
import com.vigza.markweave.common.util.IdGenerator;
import com.vigza.markweave.common.util.JwtUtil;
import com.vigza.markweave.common.util.TextOperation;
import com.vigza.markweave.infrastructure.config.RabbitMqConfig;
import com.vigza.markweave.infrastructure.persistence.entity.Collaboration;
import com.vigza.markweave.infrastructure.persistence.entity.User;
import com.vigza.markweave.infrastructure.persistence.mapper.CollaborationMapper;
import com.vigza.markweave.infrastructure.persistence.mapper.FsNodeMapper;
import com.vigza.markweave.infrastructure.persistence.mapper.UserMapper;
import com.vigza.markweave.infrastructure.service.RedisService;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CollaborationServiceImpl implements CollaborationService {

    @Autowired
    private CollaborationMapper collaborationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisService redisService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void updatePermission(Long userId, Long docId, Integer permission) {
        Collaboration collaboration = Collaboration.builder()
                .id(userId.toString() + "_" + docId.toString())
                .userId(userId)
                .docId(docId)
                .permission(permission)
                .updateTime(LocalDateTime.now())
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
    public Result<List<User>> selectCollaboratorsByDocId(String token, Long docId) {
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

        List<Long> userIds = collaborations.stream()
                .map(Collaboration::getUserId)
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return Result.success(java.util.Collections.emptyList());
        }

        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(User::getId, userIds));

        users.forEach(u -> {
            u.setPassword(null);
            u.setSalt(null);
        });

        return Result.success(users);
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
                .updateTime(LocalDateTime.now())
                .build();
        collaborationMapper.insert(collaboration);
        return Result.success();
    }

    @Override
    public void processOperation(Long docId, JSONObject clientMsg) {

        String lockKey = "lock:doc:" + docId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(3, TimeUnit.SECONDS);
            if (isLocked) {
                Long clientVer = clientMsg.getLong("version");
                TextOperation clientOp = null;
                Long currentVersion = redisService.getVersion(docId);
                if (clientVer < currentVersion) {
                    // 我们变换的时候，需要取出client的op，然后和历史op，进行transform，然后获得op'
                    clientOp = new TextOperation().fromJSON(clientMsg.getJSONArray("op").toString());

                    // 我们广播的version的就是它的操作clientaVer，所以clientVer执行过，这里应该从 + 1 开始
                    Long size = redisService.getHistoryListSize(docId);
                    List<String> historyList = redisService
                            .getHistoryRange(docId, size - (currentVersion - clientVer), size - 1)
                            .stream()
                            .map(obj -> obj.toString()).collect(Collectors.toList());
                    for (String history : historyList) {
                        JSONObject hisObj = JSONUtil.parseObj(history);

                        if (hisObj.getStr("clientId").compareTo(clientMsg.getStr("clientId")) < 0) {
                            TextOperation historyOp = new TextOperation()
                                    .fromJSON(hisObj.getJSONArray("op").toString());
                            TextOperation[] transformed = TextOperation.transform(historyOp, clientOp);
                            clientOp = transformed[1];
                        } else {
                            TextOperation historyOp = new TextOperation()
                                    .fromJSON(hisObj.getJSONArray("op").toString());
                            TextOperation[] transformed = TextOperation.transform(clientOp, historyOp);
                            clientOp = transformed[0];
                        }

                    }
                    clientMsg.set("op", clientOp.toJSON());
                }

                currentVersion = redisService.getAndIncrementVersion(docId);
                clientMsg.set("version", currentVersion);
                redisService.pushHistory(docId, clientMsg.toString());
                clientOp = new TextOperation().fromJSON(clientMsg.getStr("op"));
                // 我们拿着这个op然后apply到fullText上
                String fullText = redisService.getFullText(docId);
                fullText = clientOp.apply(fullText.toString());
                redisService.setFullText(docId, fullText);
                String response = JSONUtil.toJsonStr(clientMsg);
                rabbitTemplate.convertAndSend(RabbitMqConfig.COLLABORATION_EXCHANGE, "", response);
            } else {
                log.warn("文档 {} 竞争激烈，转发至重试队列", docId);
                Integer count = clientMsg.getInt("retryCount");
                if (count == null) {
                    count = 0;
                }
                Integer retryCount = count + 1;
                clientMsg.set("retryCount", retryCount);
                rabbitTemplate.convertAndSend(RabbitMqConfig.RETRY_EXCHANGE, RabbitMqConfig.RETRY_ROUTING_KEY,
                        clientMsg, msg -> {
                            Long backoff = computeBackoff(retryCount);
                            msg.getMessageProperties().setExpiration(backoff.toString());
                            return msg;
                        });
            }
        } catch (InterruptedException e) {
            log.error("获取文档 {} 锁失败: {}", docId, e.getMessage());
        } finally {
            if (isLocked) {
                lock.unlock();
            }
        }
    }

    private Long computeBackoff(Integer retryCount) {
        int shift = Math.min(retryCount - 1, 16);
        return (1L << shift);
    }
}
