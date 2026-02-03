
package com.vigza.markweave.core.service.serviceImpl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.vigza.markweave.api.dto.FsNodeVo;
import com.vigza.markweave.api.dto.RecentDocVO;
import com.vigza.markweave.common.Constants;
import com.vigza.markweave.common.Result;
import com.vigza.markweave.common.util.IdGenerator;
import com.vigza.markweave.common.util.JwtUtil;
import com.vigza.markweave.core.service.CollaborationService;
import com.vigza.markweave.infrastructure.persistence.entity.Collaboration;
import com.vigza.markweave.infrastructure.persistence.entity.Doc;
import com.vigza.markweave.infrastructure.persistence.entity.FsNode;
import com.vigza.markweave.infrastructure.persistence.entity.User;
import com.vigza.markweave.infrastructure.persistence.mapper.CollaborationMapper;
import com.vigza.markweave.infrastructure.persistence.mapper.DocMapper;
import com.vigza.markweave.infrastructure.persistence.mapper.FsNodeMapper;

public interface FileSystemService {

    Result<FsNode> createNode(String fileName, Long faId, Integer fileType, String token);

    Result<?> rename(Long nodeId, String newName, String token);

    Result<?> move(Long fromId, Long toId, String token);

    Result<?> recycle(Long nodeId, String token);

    Result<List<FsNodeVo>> listFiles(Long faId, String token);

    void updateDocContent(Long docId, String content);

    Result<List<RecentDocVO>> selectRecentDocList(String token);

}
