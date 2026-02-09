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
import com.vigza.markweave.infrastructure.service.RedisService;

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

    @Autowired
    private RedisService redisService;

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
        FsNode faNode = (FsNode) createNode("我的云盘", user.getUserSpaceNodeId(), 0L, Constants.FsNodeType.FOLDER, token,
                false)
                .getData();
        Long faId = faNode.getId();
        createNode("我的共享", user.getUserShareSpaceNodeId(), faId, Constants.FsNodeType.FOLDER, token, false);
    }

    @Override
    public Result<FsNode> createNode(String fileName, Long nodeId, Long faId, Integer fileType, String token) {
        return createNode(fileName, nodeId, faId, fileType, token, true);
    }

    // 记得参数校验filename不 能包含特殊字符
    @Transactional
    public Result<FsNode> createNode(String fileName, Long nodeId, Long faId, Integer fileType, String token,
            boolean strict) {
        Result<FsNode> preResult = getAccess(faId, token, strict);
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
        if (nodeId == null) {
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
            Doc doc = new Doc(docId, "",false);
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
        String fromPath = node.getPath();

        String toPath = fromPath.substring(0, fromPath.length() - node.getName().length()) + "/" + newName;
        node.setPath(toPath);
        node.setName(newName);
        fsNodeMapper.updateById(node);
        fromPath += "/";
        toPath += "/";
        if (node.getType().equals(Constants.FsNodeType.FOLDER)) {
            fsNodeMapper.updateChildPaths(fromPath, toPath);
        }
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
        if(node.getType().equals(Constants.FsNodeType.FILE)){
            Doc doc = docMapper.selectById(node.getDocId());
            if(doc == null){
                return Result.error(404, "文档不存在");
            }
            doc.setRecycled(true);
            docMapper.updateById(doc);
        }
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
        List<FsNode> nodes = fsNodeMapper.selectList(new LambdaQueryWrapper<FsNode>()
                .eq(FsNode::getFaId, faId)
                .eq(FsNode::getRecycled, false)
                .orderByAsc(FsNode::getName));

        Set<Long> ptIds = nodes.stream()
                .filter(node -> node.getType().equals(Constants.FsNodeType.SHORTCUT) && node.getPtId() != null)
                .map(node -> node.getPtId())
                .collect(Collectors.toSet());

        List<FsNodeVo> result = nodes.stream().map(node -> {
            FsNodeVo vo = new FsNodeVo();
            copyBaseProperties(node, vo);
            return vo;

        }).collect(Collectors.toList());

        return Result.success(result);
    }

    private void copyBaseProperties(FsNode source, FsNodeVo target) {
        target.setId(source.getId());
        target.setDocId(source.getDocId());
        target.setName(source.getName());
        target.setPath(source.getPath());
        target.setType(source.getType());
        target.setOwnerName(source.getDocOwner());
        target.setSize(source.getSize());
        target.setUpdateTime(source.getUpdateTime());
        target.setCreateTime(source.getCreateTime());
        target.setPtId(source.getPtId());
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
    public Result<List<FsNodeVo>> selectRecentDocList(String token) {
        User user = jwtUtil.getUserFromToken(token);
        Long userId = user.getId();

        List<FsNode> fsNodes = fsNodeMapper.selectList(new LambdaQueryWrapper<FsNode>()
                .eq(FsNode::getUserId, userId)
                .ne(FsNode::getType, Constants.FsNodeType.FOLDER)
                .eq(FsNode::getRecycled, false)
                .orderByDesc(FsNode::getLastViewTime)
                .last("limit 50"));
        if (fsNodes == null)
            fsNodes = new ArrayList<FsNode>();

        List<FsNodeVo> result = fsNodes.stream()
                .map(fsNode -> {
                    return FsNodeVo.builder()
                            .id(fsNode.getId())
                            .userId(fsNode.getUserId())
                            .docOwner(fsNode.getDocOwner())
                            .ownerName(fsNode.getDocOwner())
                            .docId(fsNode.getDocId())
                            .name(fsNode.getName())
                            .faId(fsNode.getFaId())
                            .path(fsNode.getPath())
                            .type(fsNode.getType())
                            .ptId(fsNode.getPtId())
                            .recycled(fsNode.getRecycled())
                            .size(fsNode.getSize())
                            .updateTime(fsNode.getUpdateTime())
                            .createTime(fsNode.getCreateTime())
                            .lastViewTime(fsNode.getLastViewTime())
                            .build();
                })
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
        // return preResult;

        // FsNode node = (FsNode) preResult.getData();
        // node.setRecycled(false);
        // fsNodeMapper.updateById(node);

        // if (node.getType().equals(Constants.FsNodeType.FOLDER)) {
        // String pathPrefix = node.getPath() + "/";
        // String newPathPrefix = node.getPath().replaceFirst("/[^/]+$", "") + "/";
        // fsNodeMapper.update(null, new LambdaQueryWrapper<FsNode>()
        // .likeRight(FsNode::getPath, pathPrefix)
        // .set(FsNode::getRecycled, false));
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

    @Override
    public String getDocContent(Long docId) {
        String content = redisService.getFullText(docId);
        if (content == null) {
            content = docMapper.selectById(docId).getContent();
            if (content == null) {
                content = "";
            }
            redisService.setFullText(docId, content);
        }
        return content;
    }

}
