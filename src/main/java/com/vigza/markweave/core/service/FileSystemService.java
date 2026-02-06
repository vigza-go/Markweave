
package com.vigza.markweave.core.service;

import java.util.List;

import com.vigza.markweave.api.dto.FileSystem.FsNodeVo;
import com.vigza.markweave.api.dto.FileSystem.RecentDocVO;
import com.vigza.markweave.common.Result;
import com.vigza.markweave.infrastructure.persistence.entity.FsNode;

public interface FileSystemService {

    Result<FsNode> createNode(String fileName,Long nodeId, Long faId, Integer fileType, String token);

    // 应当由CollaborationService 的 acceptInvation 或者 前端创建快捷方式调用
    Result<FsNode> createPtrNode(Long faId,Long srcNodeId,String token);

    Result<?> rename(Long nodeId, String newName, String token);

    Result<?> move(Long fromId, Long toId, String token);

    Result<?> recycle(Long nodeId, String token);

    Result<List<FsNodeVo>> listFiles(Long faId, String token);

    // 在 websocket 中调用
    void updateDocContent(Long docId, String content);

    // 应当在用户打开文档时，关闭文章后调用
    Result<?> updateViewTime(Long nodeId,String token);

    Result<List<RecentDocVO>> selectRecentDocList(String token);

    void initUserNodes(String token);

}
