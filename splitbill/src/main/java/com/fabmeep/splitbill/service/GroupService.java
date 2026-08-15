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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class GroupService {

    private final BillGroupRepository groupRepository;
    private final ParticipantRepository participantRepository;

    public GroupService(BillGroupRepository groupRepository, ParticipantRepository participantRepository) {
        this.groupRepository = groupRepository;
        this.participantRepository = participantRepository;
    }

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request) {
        if (request.participants() == null || request.participants().size() < 2) {
            throw new BadRequestException("At least 2 participants are required to create a bill group");
        }

        // Validate distinct participant names (case-insensitive)
        Set<String> uniqueNames = new HashSet<>();
        for (String name : request.participants()) {
            if (name == null || name.trim().isEmpty()) {
                throw new BadRequestException("Participant name cannot be blank");
            }
            String trimmedLower = name.trim().toLowerCase();
            if (!uniqueNames.add(trimmedLower)) {
                throw new BadRequestException("Duplicate participant name in group: " + name.trim());
            }
        }

        BillGroup group = new BillGroup(request.name().trim(), request.description() != null ? request.description().trim() : null);

        for (String participantName : request.participants()) {
            Participant participant = new Participant(participantName.trim());
            group.addParticipant(participant);
        }

        BillGroup savedGroup = groupRepository.save(group);
        return GroupResponse.fromEntity(savedGroup);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(UUID groupId) {
        BillGroup group = groupRepository.findByIdWithParticipants(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Bill group not found with id: " + groupId));
        return GroupResponse.fromEntity(group);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> listGroups() {
        return groupRepository.findAll().stream()
            .map(GroupResponse::fromEntity)
            .toList();
    }

    @Transactional
    public ParticipantResponse addParticipant(UUID groupId, AddParticipantRequest request) {
        BillGroup group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Bill group not found with id: " + groupId));

        String name = request.name().trim();
        if (participantRepository.existsByGroupIdAndNameIgnoreCase(groupId, name)) {
            throw new BadRequestException("Participant with name '" + name + "' already exists in this group");
        }

        Participant participant = new Participant(name, group);
        Participant saved = participantRepository.save(participant);
        return ParticipantResponse.fromEntity(saved);
    }
}
