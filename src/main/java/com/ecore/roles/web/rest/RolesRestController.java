package com.ecore.roles.web.rest;

import com.ecore.roles.model.Role;
import com.ecore.roles.service.RolesService;
import com.ecore.roles.web.RolesApi;
import com.ecore.roles.web.dto.RoleDto;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.ecore.roles.web.dto.RoleDto.fromModel;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/v1/roles")
public class RolesRestController implements RolesApi {

    private final RolesService rolesService;

    @Override
    @PostMapping(
            consumes = {"application/json"},
            produces = {"application/json"})
    public ResponseEntity<RoleDto> createRole(
            @Valid @RequestBody RoleDto role) {
        return ResponseEntity
                .status(201)
                .body(fromModel(rolesService.createRole(role.toModel())));
    }

    @Override
    @GetMapping(
            produces = {"application/json"})
    public ResponseEntity<List<RoleDto>> getRoles() {

        List<Role> getRoles = rolesService.getRoles();

        List<RoleDto> roleDtoList = new ArrayList<>();

        for (Role role : getRoles) {
            RoleDto roleDto = fromModel(role);
            roleDtoList.add(roleDto);
        }

        return ResponseEntity
                .status(200)
                .body(roleDtoList);
    }

    @Override
    @GetMapping(
            path = "/{roleId}",
            produces = {"application/json"})
    public ResponseEntity<RoleDto> getRole(
            @PathVariable UUID roleId) {
        return ResponseEntity
                .status(200)
                .body(fromModel(rolesService.getRole(roleId)));
    }

    @Override
    @GetMapping(
            path = "/search",
            produces = {"application/json"})
    public ResponseEntity<RoleDto> getRoleByUserIdAndTeamId(
            @RequestParam UUID teamMemberId,
            @RequestParam UUID teamId) {
        return ResponseEntity
                .status(200)
                .body(fromModel(rolesService.getRoleByUserIdAndTeamId(teamMemberId, teamId)));
    }

    @Override
    @GetMapping(
            path = "/filter",
            produces = {"application/json"})
    public ResponseEntity<List<RoleDto>> getRolesByFilters(
            @RequestParam(required = false) UUID teamMemberId,
            @RequestParam(required = false) UUID teamId) {

        List<Role> roles = rolesService.getRolesByFilters(teamMemberId, teamId);

        List<RoleDto> roleDtos = new ArrayList<>();

        for (Role role : roles) {
            RoleDto roleDto = fromModel(role);
            roleDtos.add(roleDto);
        }

        return ResponseEntity
                .status(200)
                .body(roleDtos);
    }
}
