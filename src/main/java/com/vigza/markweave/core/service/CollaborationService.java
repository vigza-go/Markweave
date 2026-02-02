package com.vigza.markweave.core.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.vigza.markweave.infrastructure.persistence.entity.Collaboration;
import com.vigza.markweave.infrastructure.persistence.mapper.CollaborationMapper;

@Service
public class CollaborationService {
    @Autowired
    private CollaborationMapper collaborationMapper;

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

    public boolean canRead(Long userId, Long docId) {
        Integer value = checkPermission(userId, docId);
        if (value > 3 || value < 1) {
            return false;
        }
        return true;
    }

    // 尚未调用
    public void updateViewTime(Long userId, Long docId) {
        LambdaUpdateWrapper<Collaboration> lambdaUpdateWrapper = new LambdaUpdateWrapper<Collaboration>()
                .eq(Collaboration::getDocId, docId)
                .eq(Collaboration::getUserId, userId)
                .set(Collaboration::getLastViewTime, LocalDateTime.now());
        collaborationMapper.update(lambdaUpdateWrapper);
    }
    // 返回协作者id
    List<Long> selectCollaboratorsByDoc(Long docId);

    List<RecentDocVO> selectRecentDocs(Long userId, int limit);


    // 生成一个jwt链接,包含doc信息 ，写入数据库
    String createInvitaion(Long userId,Long docId,Integer permission，Integer expTime){

    }

    void 

}
