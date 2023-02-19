package com.ecore.roles.service.impl;

import com.ecore.roles.client.model.Team;
import com.ecore.roles.client.model.User;
import com.ecore.roles.exception.ResourceExistsException;
import com.ecore.roles.exception.ResourceNotFoundException;
import com.ecore.roles.model.Membership;
import com.ecore.roles.model.Role;
import com.ecore.roles.repository.MembershipRepository;
import com.ecore.roles.repository.RoleRepository;
import com.ecore.roles.service.RolesService;
import com.ecore.roles.service.TeamsService;
import com.ecore.roles.service.UsersService;

import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

@Log4j2
@Service
public class RolesServiceImpl implements RolesService {

    public static final String DEFAULT_ROLE = "Developer";

    private final RoleRepository roleRepository;
    private final MembershipRepository membershipRepository;
    private final TeamsService teamsService;
    private final UsersService usersService;

    @Autowired
    public RolesServiceImpl(
            RoleRepository roleRepository,
            MembershipRepository membershipRepository,
            TeamsService teamsService,
            UsersService usersService) {
        this.roleRepository = roleRepository;
        this.membershipRepository = membershipRepository;
        this.teamsService = teamsService;
        this.usersService = usersService;
    }

    @Override
    public Role createRole(@NonNull Role role) {
        if (roleRepository.findByName(role.getName()).isPresent()) {
            throw new ResourceExistsException(Role.class);
        }
        return roleRepository.save(role);
    }

    @Override
    public Role getRole(@NonNull UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Role.class, id));
    }

    @Override
    public List<Role> getRoles() {
        return roleRepository.findAll();
    }

    @Override
    public Role getRoleByUserIdAndTeamId(
            @NonNull UUID userId,
            @NonNull UUID teamId) {

        Optional.of(teamId)
                .map(teamsService::getTeam)
                .orElseThrow(() -> new ResourceNotFoundException(Team.class, teamId));
        Optional.of(userId)
                .map(usersService::getUser)
                .orElseThrow(() -> new ResourceNotFoundException(User.class, userId));

        Membership membership = membershipRepository.findByUserIdAndTeamId(userId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException(Membership.class,
                        String.format("userId:%s teamId:%s", userId, teamId)));

        return membership.getRole();
    }

    @Override
    public List<Role> getRolesByFilters(
            UUID userId,
            UUID teamId) {
        if (userId == null && teamId == null) {
            return getRoles();
        }
        if (userId != null && teamId != null) {
            Role role = getRoleByUserIdAndTeamId(userId, teamId);
            return List.of(role);
        }

        List<Membership> memberships = Collections.emptyList();

        if (userId != null) {
            Optional.of(userId)
                    .map(usersService::getUser)
                    .orElseThrow(() -> new ResourceNotFoundException(User.class, userId));
            memberships = membershipRepository.findByUserId(userId);
        } else {
            Optional.of(teamId)
                    .map(teamsService::getTeam)
                    .orElseThrow(() -> new ResourceNotFoundException(Team.class, teamId));
            memberships = membershipRepository.findByTeamId(teamId);
        }

        Set<UUID> rolesIds = new HashSet<>();
        List<Role> roles = new ArrayList<>();
        for (Membership membership : memberships) {
            if (rolesIds.contains(membership.getRole().getId())) {
                continue;
            }
            rolesIds.add(membership.getRole().getId());
            roles.add(membership.getRole());
        }
        return roles;
    }
}
