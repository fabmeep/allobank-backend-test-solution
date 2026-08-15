package com.fabmeep.splitbill.service;

import com.fabmeep.splitbill.dto.AddParticipantRequest;
import com.fabmeep.splitbill.dto.CreateGroupRequest;
import com.fabmeep.splitbill.dto.GroupResponse;
import com.fabmeep.splitbill.dto.ParticipantResponse;
import com.fabmeep.splitbill.entity.BillGroup;
import com.fabmeep.splitbill.entity.Participant;
import com.fabmeep.splitbill.exception.BadRequestException;
import com.fabmeep.splitbill.exception.ResourceNotFoundException;
import com.fabmeep.splitbill.repository.BillGroupRepository;
import com.fabmeep.splitbill.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private BillGroupRepository groupRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @InjectMocks
    private GroupService groupService;

    private UUID groupId;
    private BillGroup group;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        group = new BillGroup("Trip to Bali", "Summer vacation");
        group.setId(groupId);

        Participant p1 = new Participant("Alice", group);
        p1.setId(UUID.randomUUID());
        Participant p2 = new Participant("Bob", group);
        p2.setId(UUID.randomUUID());

        group.setParticipants(List.of(p1, p2));
    }

    @Test
    @DisplayName("Should create group with valid participants")
    void testCreateGroupSuccess() {
        CreateGroupRequest request = new CreateGroupRequest("Trip to Bali", "Summer vacation", List.of("Alice", "Bob"));

        when(groupRepository.save(any(BillGroup.class))).thenAnswer(invocation -> {
            BillGroup bg = invocation.getArgument(0);
            bg.setId(groupId);
            return bg;
        });

        GroupResponse response = groupService.createGroup(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(groupId);
        assertThat(response.name()).isEqualTo("Trip to Bali");
        assertThat(response.participants()).hasSize(2);
        verify(groupRepository).save(any(BillGroup.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException if fewer than 2 participants")
    void testCreateGroupLessThanTwoParticipants() {
        CreateGroupRequest request = new CreateGroupRequest("Solo Trip", null, List.of("Alice"));

        assertThatThrownBy(() -> groupService.createGroup(request))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("At least 2 participants are required");
    }

    @Test
    @DisplayName("Should throw BadRequestException if duplicate participant names")
    void testCreateGroupDuplicateParticipants() {
        CreateGroupRequest request = new CreateGroupRequest("Trip", null, List.of("Alice", "alice"));

        assertThatThrownBy(() -> groupService.createGroup(request))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Duplicate participant name in group");
    }

    @Test
    @DisplayName("Should get group by id successfully")
    void testGetGroupSuccess() {
        when(groupRepository.findByIdWithParticipants(groupId)).thenReturn(Optional.of(group));

        GroupResponse response = groupService.getGroup(groupId);

        assertThat(response.id()).isEqualTo(groupId);
        assertThat(response.name()).isEqualTo("Trip to Bali");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when group does not exist")
    void testGetGroupNotFound() {
        when(groupRepository.findByIdWithParticipants(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getGroup(groupId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Bill group not found");
    }

    @Test
    @DisplayName("Should list all groups")
    void testListGroups() {
        when(groupRepository.findAll()).thenReturn(List.of(group));

        List<GroupResponse> responses = groupService.listGroups();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("Trip to Bali");
    }

    @Test
    @DisplayName("Should add participant to existing group")
    void testAddParticipantSuccess() {
        AddParticipantRequest req = new AddParticipantRequest("Charlie");
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(participantRepository.existsByGroupIdAndNameIgnoreCase(groupId, "Charlie")).thenReturn(false);
        when(participantRepository.save(any(Participant.class))).thenAnswer(inv -> {
            Participant p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        ParticipantResponse resp = groupService.addParticipant(groupId, req);

        assertThat(resp.name()).isEqualTo("Charlie");
        verify(participantRepository).save(any(Participant.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when adding duplicate participant")
    void testAddDuplicateParticipant() {
        AddParticipantRequest req = new AddParticipantRequest("Alice");
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(participantRepository.existsByGroupIdAndNameIgnoreCase(groupId, "Alice")).thenReturn(true);

        assertThatThrownBy(() -> groupService.addParticipant(groupId, req))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("already exists in this group");
    }
}
