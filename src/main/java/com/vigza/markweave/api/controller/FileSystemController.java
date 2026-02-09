package com.vigza.markweave.api.controller;

import java.io.File;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vigza.markweave.api.dto.FileSystem.CreateFileRequest;
import com.vigza.markweave.api.dto.FileSystem.CreateShortcutRequest;
import com.vigza.markweave.api.dto.FileSystem.FsNodeVo;
import com.vigza.markweave.api.dto.FileSystem.MoveRequest;
import com.vigza.markweave.api.dto.FileSystem.RecentDocVO;
import com.vigza.markweave.api.dto.FileSystem.RenameRequest;
import com.vigza.markweave.common.Constants;
import com.vigza.markweave.common.Result;
import com.vigza.markweave.common.util.IdGenerator;
import com.vigza.markweave.common.util.JwtUtil;
import com.vigza.markweave.core.service.FileSystemService;
import com.vigza.markweave.infrastructure.persistence.entity.FsNode;

@Validated
@RestController
@RequestMapping("/api/fs")
public class FileSystemController {
    @Autowired
    FileSystemService fsService;

    @Autowired
    JwtUtil jwtUtil;

    @PostMapping("/file/create")
    Result<FsNode> createCommonFile(@RequestHeader("Authorization") String token,
            @Valid @RequestBody CreateFileRequest request) {
        Long faId = request.getFaId();
        String fileName = request.getFileName();
        Integer fileType = request.getFileType();
        if (jwtUtil.validateToken(token) && Constants.FsNodeType.isCommonType(fileType)) {
            Long id = IdGenerator.nextId();
            return fsService.createNode(fileName, id, faId, fileType, token);
        } else {
            return Result.error(403, "校验不通过");
        }
    }

    @PostMapping("/file/shortcut/create")
    Result<FsNode> createShortcut(@RequestHeader("Authorization") String token,
            @Valid @RequestBody CreateShortcutRequest request) {
        Long faId = request.getFaId();
        Long srcNodeId = request.getSrcNodeId();
        if (jwtUtil.validateToken(token)) {
            return fsService.createPtrNode(faId, srcNodeId, token);
        } else {
            return Result.error(403, "校验不通过");
        }
    }

    @PostMapping("/file/rename")
    Result<?> rename(@RequestHeader("Authorization") String token, @Valid @RequestBody RenameRequest request) {
        Long nodeId = request.getNodeId();
        String newName = request.getNewName();
        if (jwtUtil.validateToken(token)) {
            return fsService.rename(nodeId, newName, token);
        } else {
            return Result.error(403, "校验不通过");
        }
    }

    @PostMapping("/file/move")
    Result<?> move(@RequestHeader("Authorization") String token, @Valid @RequestBody MoveRequest request) {
        Long nodeId = request.getNodeId();
        Long targetFolderId = request.getTargetFolderId();
        if (jwtUtil.validateToken(token)) {
            return fsService.move(nodeId, targetFolderId, token);
        } else {
            return Result.error(403, "校验不通过");
        }
    }

    @PostMapping("/file/recycle")
    Result<?> recycle(@RequestHeader("Authorization") String token, @Valid @NotNull Long nodeId) {
        if (jwtUtil.validateToken(token)) {
            return fsService.recycle(nodeId, token);
        } else {
            return Result.error(403, "校验不通过");
        }
    }

    @GetMapping("/files/{faId}/list")
    Result<List<FsNodeVo>> listFiles(@RequestHeader("Authorization") String token, @PathVariable Long faId) {
        if (jwtUtil.validateToken(token)) {
            return fsService.listFiles(faId, token);
        } else {
            return Result.error(403, "校验不通过");
        }
    }

    @PostMapping("/file/viewtime/update")
    Result<?> updateViewTime(@RequestHeader("Authorization") String token, @Valid @NotNull Long nodeId) {
        if (jwtUtil.validateToken(token)) {
            return fsService.updateViewTime(nodeId, token);
        } else {
            return Result.error(403, "校验不通过");
        }
    }

    @GetMapping("/docs/recent")
    Result<List<FsNodeVo>> selectRecentDocList(@RequestHeader("Authorization") String token) {
        if (jwtUtil.validateToken(token)) {
            return fsService.selectRecentDocList(token);
        } else {
            return Result.error(403, "校验不通过");
        }
    }

    @GetMapping("/files/recycled/list")
    Result<List<FsNodeVo>> getRecycledFiles(@RequestHeader("Authorization") String token) {
        if (jwtUtil.validateToken(token)) {
            return fsService.getRecycledFiles(token);
        } else {
            return Result.error(403, "校验不通过");
        }
    }

    @PostMapping("/file/restore")
    Result<?> restoreFile(@RequestHeader("Authorization") String token, @Valid @NotNull Long nodeId) {
        if (jwtUtil.validateToken(token)) {
            return fsService.restoreFile(nodeId, token);
        } else {
            return Result.error(403, "校验不通过");
        }
    }

    @PostMapping("/file/delete/permanent")
    Result<?> permanentlyDelete(@RequestHeader("Authorization") String token, @Valid @NotNull Long nodeId) {
        if (jwtUtil.validateToken(token)) {
            return fsService.permanentlyDelete(nodeId, token);
        } else {
            return Result.error(403, "校验不通过");
        }
    }
}
