package com.fabmeep.splitbill.controller;

import com.fabmeep.splitbill.dto.AddParticipantRequest;
import com.fabmeep.splitbill.dto.ApiResponse;
import com.fabmeep.splitbill.dto.CreateGroupRequest;
import com.fabmeep.splitbill.dto.GroupResponse;
import com.fabmeep.splitbill.dto.ParticipantResponse;
import com.fabmeep.splitbill.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        GroupResponse response = groupService.createGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Group created successfully", response));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroup(@PathVariable UUID groupId) {
        GroupResponse response = groupService.getGroup(groupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GroupResponse>>> listGroups() {
        List<GroupResponse> responses = groupService.listGroups();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/{groupId}/participants")
    public ResponseEntity<ApiResponse<ParticipantResponse>> addParticipant(
        @PathVariable UUID groupId,
        @Valid @RequestBody AddParticipantRequest request
    ) {
        ParticipantResponse response = groupService.addParticipant(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Participant added successfully", response));
    }
}
