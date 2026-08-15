package com.fabmeep.splitbill.controller;

import com.fabmeep.splitbill.dto.AddParticipantRequest;
import com.fabmeep.splitbill.dto.CreateGroupRequest;
import com.fabmeep.splitbill.dto.GroupResponse;
import com.fabmeep.splitbill.dto.ParticipantResponse;
import com.fabmeep.splitbill.exception.GlobalExceptionHandler;
import com.fabmeep.splitbill.exception.ResourceNotFoundException;
import com.fabmeep.splitbill.service.GroupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
@Import(GlobalExceptionHandler.class)
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GroupService groupService;

    @Test
    @DisplayName("POST /api/v1/groups - 201 Created on valid request")
    void testCreateGroup() throws Exception {
        UUID groupId = UUID.randomUUID();
        CreateGroupRequest request = new CreateGroupRequest("Bali Trip", "Holiday", List.of("Alice", "Bob"));
        GroupResponse response = new GroupResponse(
            groupId,
            "Bali Trip",
            "Holiday",
            List.of(new ParticipantResponse(UUID.randomUUID(), "Alice"), new ParticipantResponse(UUID.randomUUID(), "Bob")),
            Instant.now()
        );

        when(groupService.createGroup(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(groupId.toString()))
            .andExpect(jsonPath("$.data.name").value("Bali Trip"))
            .andExpect(jsonPath("$.data.participants").isArray())
            .andExpect(jsonPath("$.data.participants.length()").value(2));
    }

    @Test
    @DisplayName("POST /api/v1/groups - 400 Bad Request on validation failure")
    void testCreateGroupValidationFailure() throws Exception {
        CreateGroupRequest invalidRequest = new CreateGroupRequest("", null, List.of("Alice"));

        mockMvc.perform(post("/api/v1/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/groups/{groupId} - 200 OK when found")
    void testGetGroupSuccess() throws Exception {
        UUID groupId = UUID.randomUUID();
        GroupResponse response = new GroupResponse(groupId, "Bali Trip", "Holiday", List.of(), Instant.now());

        when(groupService.getGroup(groupId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/groups/{groupId}", groupId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(groupId.toString()))
            .andExpect(jsonPath("$.data.name").value("Bali Trip"));
    }

    @Test
    @DisplayName("GET /api/v1/groups/{groupId} - 404 Not Found when missing")
    void testGetGroupNotFound() throws Exception {
        UUID groupId = UUID.randomUUID();
        when(groupService.getGroup(groupId)).thenThrow(new ResourceNotFoundException("Bill group not found with id: " + groupId));

        mockMvc.perform(get("/api/v1/groups/{groupId}", groupId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Bill group not found with id: " + groupId));
    }

    @Test
    @DisplayName("GET /api/v1/groups - 200 OK")
    void testListGroups() throws Exception {
        UUID groupId = UUID.randomUUID();
        GroupResponse response = new GroupResponse(groupId, "Bali Trip", "Holiday", List.of(), Instant.now());

        when(groupService.listGroups()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/groups"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/groups/{groupId}/participants - 201 Created")
    void testAddParticipant() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        AddParticipantRequest request = new AddParticipantRequest("Charlie");
        ParticipantResponse response = new ParticipantResponse(participantId, "Charlie");

        when(groupService.addParticipant(eq(groupId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/groups/{groupId}/participants", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(participantId.toString()))
            .andExpect(jsonPath("$.data.name").value("Charlie"));
    }
}
