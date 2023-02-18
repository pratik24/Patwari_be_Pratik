package com.ecore.roles.service.impl;

import com.ecore.roles.client.model.Team;
import com.ecore.roles.client.model.User;
import com.ecore.roles.exception.InvalidArgumentException;
import com.ecore.roles.exception.ResourceExistsException;
import com.ecore.roles.exception.ResourceNotFoundException;
import com.ecore.roles.model.Membership;
import com.ecore.roles.model.Role;
import com.ecore.roles.repository.MembershipRepository;
import com.ecore.roles.repository.RoleRepository;
import com.ecore.roles.service.MembershipsService;
import com.ecore.roles.service.TeamsService;
import com.ecore.roles.service.UsersService;

import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static java.util.Optional.ofNullable;

@Log4j2
@Service
public class MembershipsServiceImpl implements MembershipsService {

    private final MembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final TeamsService teamsService;
    private final UsersService usersService;

    @Autowired
    public MembershipsServiceImpl(
            MembershipRepository membershipRepository,
            RoleRepository roleRepository,
            TeamsService teamsService,
            UsersService usersService) {
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.teamsService = teamsService;
        this.usersService = usersService;
    }

    @Override
    public Membership assignRoleToMembership(@NonNull Membership membership) {

        UUID roleId = ofNullable(membership.getRole()).map(Role::getId)
                .orElseThrow(() -> new InvalidArgumentException(Role.class));

        if (membershipRepository.findByUserIdAndTeamId(membership.getUserId(), membership.getTeamId())
                .isPresent()) {
            throw new ResourceExistsException(Membership.class);
        }

        roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException(Role.class, roleId));
        
        Team team = teamsService.getTeam(membership.getTeamId());
        if (team == null) {
            throw new ResourceNotFoundException(Team.class, membership.getTeamId());
        }
        if(!isUserInTeam(membership.getUserId(), team)){
            throw new InvalidArgumentException(Membership.class, "The provided user doesn't belong to the provided team.");
        }
        User user = usersService.getUser(membership.getUserId());
        if (user == null) {
            throw new ResourceNotFoundException(User.class, membership.getUserId());
        }
        return membershipRepository.save(membership);
    }

    @Override
    public List<Membership> getMembershipsByRoleId(@NonNull UUID roleId) {
        return membershipRepository.findByRoleId(roleId);
    }

    private boolean isUserInTeam(UUID userId, Team team) {
        if(userId.equals(team.getTeamLeadId())){
            return true;
        }
        if(team.getTeamMemberIds() == null || team.getTeamMemberIds().size() == 0){
            return false;
        }
        return team.getTeamMemberIds().contains(userId);
    }
}
