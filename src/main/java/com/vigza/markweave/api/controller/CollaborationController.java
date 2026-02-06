package com.vigza.markweave.api.controller;

import java.security.Permission;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vigza.markweave.api.dto.Collaboration.CollaboratorVo;
import com.vigza.markweave.api.dto.Collaboration.InviteRequest;
import com.vigza.markweave.api.dto.Collaboration.PermissionRequest;
import com.vigza.markweave.common.Constants;
import com.vigza.markweave.common.Result;
import com.vigza.markweave.common.util.JwtUtil;
import com.vigza.markweave.core.service.CollaborationService;
import com.vigza.markweave.infrastructure.persistence.entity.User;

@RestController
@RequestMapping("/api/collaboration")
public class CollaborationController {
    @Autowired
    CollaborationService collaborationService;

    @Autowired
    JwtUtil jwtUtil;

    @PostMapping("/invite")
    public Result<String> createInvitation(@RequestHeader("Authorization") String token,
            @Valid @RequestBody InviteRequest request) {
        Long docId = request.getDocId();
        Integer permission = request.getPermission();
        Integer expTime = request.getExpTime();
        if (expTime > 0 && jwtUtil.validateToken(token) && Constants.CollaborationPermission.isValid(permission)) {
            return collaborationService.createInvitation(token, docId, permission, expTime);
        } else {
            return Result.error(403, "校验不通过");
        }
    }

    @PostMapping("/invite/accept")
    public Result<?> acceptInvitation(@RequestHeader("Authorization") String token, @RequestBody String invToken) {
        return collaborationService.acceptInvitation(token, invToken);
    }


    @PostMapping("/permission}")
    public Result<?> updatePermission(@RequestHeader("Authorization") String token ,@Valid @RequestBody PermissionRequest request){
        Long docId = request.getDocId();
        Long targetUserId = request.getTargetUserId();
        Integer permission = request.getPermission();
        if( jwtUtil.validateToken(token) && Constants.CollaborationPermission.isValid(permission)){
            return collaborationService.updatePermission( token, targetUserId, docId, permission);
        }else{
            return Result.error(403,"校验不通过");
        }
    }

    @GetMapping("/docs/{docId}/collaborators")
    public Result<List<CollaboratorVo>> getCollaborators(@RequestHeader("Authorization") String token, @PathVariable Long docId) {
        if (jwtUtil.validateToken(token)) {
            return collaborationService.selectCollaboratorsByDocId(token, docId);
        } else {
            return Result.error(403, "校验不通过");
        }
    }



}
