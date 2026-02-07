package com.vigza.markweave.core.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.vigza.markweave.api.dto.FileSystem.FsNodeVo;
import com.vigza.markweave.api.dto.FileSystem.RecentDocVO;
import com.vigza.markweave.common.Constants;
import com.vigza.markweave.common.Result;
import com.vigza.markweave.common.util.IdGenerator;
import com.vigza.markweave.common.util.JwtUtil;
import com.vigza.markweave.infrastructure.persistence.entity.Collaboration;
import com.vigza.markweave.infrastructure.persistence.entity.Doc;
import com.vigza.markweave.infrastructure.persistence.entity.FsNode;
import com.vigza.markweave.infrastructure.persistence.entity.User;
import com.vigza.markweave.infrastructure.persistence.mapper.CollaborationMapper;
import com.vigza.markweave.infrastructure.persistence.mapper.DocMapper;
import com.vigza.markweave.infrastructure.persistence.mapper.FsNodeMapper;

@Service
public class FileSystemServiceImpl implements FileSystemService {
    @Autowired
    private FsNodeMapper fsNodeMapper;

    @Autowired
    private DocMapper docMapper;
    @Autowired
    private CollaborationService collaborationService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostConstruct
    public void init() {
        FsNode root = fsNodeMapper.selectById(0);
        if (root == null) {
            root = new FsNode();
            root.setId(0L);
            root.setUserId(-1L);
            root.setFaId(-1L);
            root.setPath("/");
            root.setType(Constants.FsNodeType.FOLDER);
            root.setRecycled(false);
            root.setCreateTime(LocalDateTime.now());
            fsNodeMapper.insert(root);
        }
    }

    @Transactional
    @Override
    public void initUserNodes(String token) {
        User user = jwtUtil.getUserFromToken(token);
        FsNode faNode = (FsNode) createNode("我的云盘", user.getUserSpaceNodeId(),0L, Constants.FsNodeType.FOLDER, token).getData();
        Long faId = faNode.getId();
        createNode("我的共享", user.getUserShareSpaceNodeId(),faId, Constants.FsNodeType.FOLDER, token);
    }

    // 记得参数校验filename不   能包含特殊字符
    @Transactional
    @Override
    public Result<FsNode> createNode(String fileName,Long nodeId, Long faId, Integer fileType, String token) {
        Result<FsNode> preResult = getAccess(faId, token, false);
        if (!preResult.getCode().equals(200))
            return preResult;
        if (fileName == null || fileName.isEmpty()) {
            return Result.error(400, "文件名不能为空");
        }
        FsNode faNode = (FsNode) preResult.getData();
        if (faNode.getType().equals(Constants.FsNodeType.FOLDER) == false) {
            return Result.error(409, "父节点不是一个文件夹");
        }

        if (!Constants.FsNodeType.isValid(fileType)) {
            return Result.error(400, "文件类型不合法");
        }

        User user = jwtUtil.getUserFromToken(token);
        if(nodeId == null){
            nodeId = IdGenerator.nextId();
        }
        FsNode node = FsNode.builder()
                .id(nodeId)
                .userId(user.getId())
                .docOwner(user.getNickName())
                .name(fileName)
                .faId(faId)
                .path(faNode.getPath() + "/" + fileName)
                .type(fileType)
                .recycled(false)
                .createTime(LocalDateTime.now())
                .build();
        if (fileType.equals(Constants.FsNodeType.FILE)) {
            Long docId = IdGenerator.nextId();
            Doc doc = new Doc(docId, "");
            docMapper.insert(doc);

            node.setDocId(docId);
            node.setSize(0L);

            collaborationService.updatePermission(user.getId(), docId, Constants.CollaborationPermission.CREATOR);
        }
        fsNodeMapper.insert(node);
        return Result.success(node);
    }

    @Transactional
    @Override
    public Result<?> rename(Long nodeId, String newName, String token) {
        Result<?> preResult = getAccess(nodeId, token, true);
        if (!preResult.getCode().equals(200))
            return preResult;
        if (newName == null || newName.isEmpty()) {
            return Result.error(400, "文件名不能为空");
        }

        FsNode node = (FsNode) preResult.getData();
        node.setName(newName);

        fsNodeMapper.updateById(node);
        return Result.success();
    }

    @Transactional
    @Override
    public Result<?> move(Long fromId, Long toId, String token) {
        Result<?> preResult1 = getAccess(fromId, token, true);
        if (!preResult1.getCode().equals(200))
            return preResult1;
        Result<?> preResult2 = getAccess(toId, token, true);
        if (!preResult2.getCode().equals(200))
            return preResult2;

        FsNode node = (FsNode) preResult1.getData();
        FsNode targetNode = (FsNode) preResult2.getData();
        if (targetNode.getType() != Constants.FsNodeType.FOLDER) {
            return Result.error(409, "目标节点不是一个文件夹");
        }

        // 感谢ai提醒：禁止将文件夹移动到它自己的子文件夹中。
        if (targetNode.getPath().startsWith(node.getPath())) {
            return Result.error(400, "禁止将文件夹移动到它自己的子文件夹中");
        }

        node.setFaId(toId);
        String fromPath = node.getPath() + "/";
        String toPath = targetNode.getPath() + "/" + node.getName() + "/";
        node.setPath(toPath);
        fsNodeMapper.updateById(node);

        if (node.getType().equals(Constants.FsNodeType.FOLDER)) {
            fsNodeMapper.updateChildPaths(fromPath, toPath);
        }
        return Result.success();
    }

    @Transactional
    @Override
    public Result<?> recycle(Long nodeId, String token) {
        Result<?> pResult = getAccess(nodeId, token, true);
        if (!pResult.getCode().equals(200))
            return pResult;
        FsNode node = (FsNode) pResult.getData();
        node.setRecycled(true);
        fsNodeMapper.updateById(node);

        if (node.getType().equals(Constants.FsNodeType.FOLDER)) {
            fsNodeMapper.recycleChildPaths(node.getPath() + "/");

        }

        return Result.success();
    }

    @Override
    public Result<List<FsNodeVo>> listFiles(Long faId, String token) {
        Result<?> preResult = getAccess(faId, token, true);
        if (!preResult.getCode().equals(200))
            return (Result<List<FsNodeVo>>) preResult;
        FsNode faNode = (FsNode) preResult.getData();

        List<FsNode> nodes = fsNodeMapper.selectList(new LambdaQueryWrapper<FsNode>()
                .eq(FsNode::getFaId, faId)
                .eq(FsNode::getRecycled, false)
                .orderByAsc(FsNode::getName));

        Set<Long> ptIds = nodes.stream()
                .filter(node -> node.getType().equals(Constants.FsNodeType.SHORTCUT) && node.getPtId() != null)
                .map(node -> node.getPtId())
                .collect(Collectors.toSet());

        List<FsNode> ptrList = ptIds.isEmpty() ? new ArrayList<FsNode>() : fsNodeMapper.selectList(new LambdaQueryWrapper<FsNode>()
                .in(FsNode::getId, ptIds));

        Map<Long, FsNode> targetMap = ptrList.stream().collect(Collectors.toMap(FsNode::getId, n -> n));

        List<FsNodeVo> result = nodes.stream().map(node -> {
            FsNodeVo vo = new FsNodeVo();
            copyBaseProperties(node, vo);
            if (node.getType().equals(Constants.FsNodeType.SHORTCUT) && node.getPtId() != null) {
                copyBaseProperties(targetMap.get(node.getPtId()), vo);
                vo.setPtId(node.getPtId());

            }
            return vo;

        }).collect(Collectors.toList());

        return Result.success(result);
    }

    private void copyBaseProperties(FsNode source, FsNodeVo target) {
        target.setId(source.getId());
        target.setType(source.getType());
        target.setName(source.getName());
        target.setSize(source.getSize());
        target.setUpdateTime(source.getUpdateTime());
        target.setCreateTime(source.getCreateTime());
        target.setOwnerName(source.getDocOwner());
    }

    // 不严格条件下，允许在根节点下创建个人云盘节点
    private Result<FsNode> getAccess(Long nodeId, String token, Boolean strict) {
        User user = jwtUtil.getUserFromToken(token);
        FsNode node = fsNodeMapper.selectById(nodeId);
        if (node == null) {
            return Result.error(404, "请求文件不存在");
        }
        Boolean allowAccess = (user.getType().equals(Constants.UserType.ADMIN)) ||
                (node.getUserId().equals(user.getId())) ||
                (!strict && nodeId.equals(0L));
        if (!allowAccess) {
            return Result.error(403, "未获得操作权限");
        }
        return Result.success(node);
    }

    @Transactional
    @Override
    public void updateDocContent(Long docId, String content) {
        docMapper.updateContent(docId, content);

        long newSize = content.getBytes(StandardCharsets.UTF_8).length;
        LambdaUpdateWrapper<FsNode> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FsNode::getDocId, docId)
                .set(FsNode::getSize, newSize)
                .set(FsNode::getUpdateTime, LocalDateTime.now());
        fsNodeMapper.update(updateWrapper);
    }

    @Override
    public Result<?> updateViewTime(Long nodeId, String token) {
        Result<?> preResult = getAccess(nodeId, token, true);
        if (!preResult.getCode().equals(200))
            return preResult;
        FsNode node = fsNodeMapper.selectOne(new LambdaQueryWrapper<FsNode>().eq(FsNode::getId, nodeId));
        if (node != null) {
            node.setLastViewTime(LocalDateTime.now());
            fsNodeMapper.updateById(node);
            return Result.success();
        }
        return Result.error(404, "未找到该文件");
    }

    @Override
    public Result<List<RecentDocVO>> selectRecentDocList(String token) {
        User user = jwtUtil.getUserFromToken(token);
        Long userId = user.getId();

        List<FsNode> fsNodes = fsNodeMapper.selectList(new LambdaQueryWrapper<FsNode>()
                .eq(FsNode::getUserId, userId)
                .ne(FsNode::getType, Constants.FsNodeType.FOLDER)
                .orderByDesc(FsNode::getLastViewTime)
                .last("limit 50"));
        if (fsNodes == null)
            fsNodes = new ArrayList<FsNode>();

        List<Long> ptIds = fsNodes.stream().filter(n -> n.getType().equals(Constants.FsNodeType.SHORTCUT))
                .map(n -> n.getPtId()).collect(Collectors.toList());

        Map<Long, FsNode> map = ptIds.isEmpty() ? new HashMap<Long,FsNode>() : fsNodeMapper.selectList(new LambdaQueryWrapper<FsNode>()
                .in(FsNode::getId, ptIds)).stream().collect(Collectors.toMap(FsNode::getId,n -> n));

        List<RecentDocVO> result = fsNodes.stream()
                .map(fsNode -> {
                    FsNode targetNode = fsNode;
                    if(fsNode.getType().equals(Constants.FsNodeType.SHORTCUT)){
                        targetNode = map.get(fsNode.getPtId());
                    }
                    if(targetNode == null) return null;
                    return RecentDocVO.builder()
                        .docId(targetNode.getDocId())
                        .docName(targetNode.getName())
                        .ownerId(targetNode.getUserId())
                        .size(targetNode.getSize())
                        .lastViewTime(fsNode.getLastViewTime()) // 依然用当前用户的查看时间
                        .build();
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());

        return Result.success(result);
    }

    @Override
    public Result<FsNode> createPtrNode(Long faId, Long srcNodeId, String token) {
        User user = jwtUtil.getUserFromToken(token);
        Result<?> preResult = getAccess(srcNodeId, token, true);
        if (!preResult.getCode().equals(200))
            return (Result<FsNode>) preResult;
        Result<?> faResult = getAccess(faId, token, true);
        if (!faResult.getCode().equals(200))
            return (Result<FsNode>) faResult;
        FsNode srcNode = (FsNode) preResult.getData();
        FsNode faNode = (FsNode) faResult.getData();
        FsNode node = FsNode.builder()
        .id(IdGenerator.nextId())
        .userId(user.getId())
        .docOwner(srcNode.getDocOwner())
        .name(srcNode.getName() + "_快捷方式")
        .faId(faId)
        .path(faNode.getPath() + "/" + srcNode.getName() + "_快捷方式")
        .type(Constants.FsNodeType.SHORTCUT)
        .ptId(srcNodeId)
        .recycled(false)
        .size(0L)
        .createTime(LocalDateTime.now())
        .build();
        fsNodeMapper.insert(node);
        return Result.success(node);

    }

    @Override
    public Result<List<FsNodeVo>> getRecycledFiles(String token) {
        User user = jwtUtil.getUserFromToken(token);
        List<FsNode> recycledNodes = fsNodeMapper.selectRecycledFiles(user.getId());

        List<FsNodeVo> result = recycledNodes.stream().map(node -> {
            FsNodeVo vo = new FsNodeVo();
            copyBaseProperties(node, vo);
            return vo;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @Override
    public Result<?> restoreFile(Long nodeId, String token) {
        // Result<?> preResult = getAccess(nodeId, token, true);
        // if (!preResult.getCode().equals(200))
        //     return preResult;

        // FsNode node = (FsNode) preResult.getData();
        // node.setRecycled(false);
        // fsNodeMapper.updateById(node);

        // if (node.getType().equals(Constants.FsNodeType.FOLDER)) {
        //     String pathPrefix = node.getPath() + "/";
        //     String newPathPrefix = node.getPath().replaceFirst("/[^/]+$", "") + "/";
        //     fsNodeMapper.update(null, new LambdaQueryWrapper<FsNode>()
        //             .likeRight(FsNode::getPath, pathPrefix)
        //             .set(FsNode::getRecycled, false));
        // }

        return Result.success();
    }

    @Override
    public Result<?> permanentlyDelete(Long nodeId, String token) {
        Result<?> preResult = getAccess(nodeId, token, true);
        if (!preResult.getCode().equals(200))
            return preResult;

        FsNode node = (FsNode) preResult.getData();

        if (node.getType().equals(Constants.FsNodeType.FOLDER)) {
            fsNodeMapper.delete(new LambdaQueryWrapper<FsNode>()
                    .likeRight(FsNode::getPath, node.getPath() + "/"));
        }
        fsNodeMapper.deleteById(nodeId);

        return Result.success();
    }

}
