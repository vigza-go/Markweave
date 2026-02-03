package com.vigza.markweave.core.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.vigza.markweave.api.dto.RecentDocVO;
import com.vigza.markweave.common.Result;
import com.vigza.markweave.infrastructure.persistence.entity.User;

public interface CollaborationService {

    Boolean canRead(String token, Long docId);

    Boolean canWrite(String token, Long docId);

    Result<?> updatePermission(String token, Long userId, Integer permission);

    Result<List<User>> selectCollaboratorsByDocId(String token, Long docId);

    Result<String> createInvitation(String token, Long docId, Integer permission);

    Result<?> acceptInvitation(String userToken, String invToken);
}
