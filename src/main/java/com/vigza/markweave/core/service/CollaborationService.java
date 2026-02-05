package com.vigza.markweave.core.service;

import java.util.List;

import com.vigza.markweave.common.Result;
import com.vigza.markweave.infrastructure.persistence.entity.User;

public interface CollaborationService {


    Integer getPermission(Long userId,Long docId);
    
    Boolean canRead(String token, Long docId);
    
    Boolean canWrite(String token, Long docId);

    void updatePermission(Long userId,Long docId,Integer permission);
    
    Result<?> updatePermission(String token, Long targetUserId,Long docId,Integer permission);
    
    Result<List<User>> selectCollaboratorsByDocId(String token, Long docId);

    Result<String> createInvitation(String token, Long docId, Integer permission,Integer expTime);

    Result<?> acceptInvitation(String userToken, String invToken);
}
