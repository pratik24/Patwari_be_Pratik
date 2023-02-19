package com.ecore.roles.api;

import com.ecore.roles.model.Membership;
import com.ecore.roles.model.Role;
import com.ecore.roles.repository.MembershipRepository;
import com.ecore.roles.repository.RoleRepository;
import com.ecore.roles.utils.RestAssuredHelper;
import com.ecore.roles.web.dto.RoleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.ecore.roles.utils.MockUtils.mockGetTeamById;
import static com.ecore.roles.utils.MockUtils.mockGetUserById;
import static com.ecore.roles.utils.RestAssuredHelper.createMembership;
import static com.ecore.roles.utils.RestAssuredHelper.createRole;
import static com.ecore.roles.utils.RestAssuredHelper.getRole;
import static com.ecore.roles.utils.RestAssuredHelper.getRoles;
import static com.ecore.roles.utils.RestAssuredHelper.getRolesByFilters;
import static com.ecore.roles.utils.RestAssuredHelper.sendRequest;
import static com.ecore.roles.utils.TestData.DEFAULT_MEMBERSHIP;
import static com.ecore.roles.utils.TestData.DEVELOPER_ROLE;
import static com.ecore.roles.utils.TestData.DEVOPS_ROLE;
import static com.ecore.roles.utils.TestData.GIANNI_USER;
import static com.ecore.roles.utils.TestData.GIANNI_USER_UUID;
import static com.ecore.roles.utils.TestData.ORDINARY_CORAL_LYNX_TEAM;
import static com.ecore.roles.utils.TestData.ORDINARY_CORAL_LYNX_TEAM_UUID;
import static com.ecore.roles.utils.TestData.PRODUCT_OWNER_ROLE;
import static com.ecore.roles.utils.TestData.TESTER_ROLE;
import static com.ecore.roles.utils.TestData.UUID_1;
import static io.restassured.RestAssured.when;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RolesApiTest {

    private final RestTemplate restTemplate;
    private final RoleRepository roleRepository;
    private final MembershipRepository membershipRepository;

    private MockRestServiceServer mockServer;

    @LocalServerPort
    private int port;

    @Autowired
    public RolesApiTest(
            RestTemplate restTemplate,
            RoleRepository roleRepository,
            MembershipRepository membershipRepository) {
        this.restTemplate = restTemplate;
        this.roleRepository = roleRepository;
        this.membershipRepository = membershipRepository;
    }

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        RestAssuredHelper.setUp(port);
        Optional<Role> devOpsRole = roleRepository.findByName(DEVOPS_ROLE().getName());
        devOpsRole.ifPresent(roleRepository::delete);
        membershipRepository.deleteAll();
    }

    @Test
    void shouldFailWhenPathDoesNotExist() {
        sendRequest(when()
                .get("/v1/role")
                .then())
                        .validate(404, "Not Found");
    }

    @Test
    void shouldCreateNewRole() {
        Role expectedRole = DEVOPS_ROLE();

        RoleDto actualRole = createRole(expectedRole)
                .statusCode(201)
                .extract().as(RoleDto.class);

        assertThat(actualRole.getName()).isEqualTo(expectedRole.getName());
    }

    @Test
    void shouldFailToCreateNewRoleWhenNull() {
        createRole(null)
                .validate(400, "Bad Request");
    }

    @Test
    void shouldFailToCreateNewRoleWhenMissingName() {
        createRole(Role.builder().build())
                .validate(400, "Bad Request");
    }

    @Test
    void shouldFailToCreateNewRoleWhenBlankName() {
        createRole(Role.builder().name("").build())
                .validate(400, "Bad Request");
    }

    @Test
    void shouldFailToCreateNewRoleWhenNameAlreadyExists() {
        createRole(DEVELOPER_ROLE())
                .validate(400, "Role already exists");
    }

    @Test
    void shouldGetAllRoles() {
        RoleDto[] roles = getRoles()
                .extract().as(RoleDto[].class);

        assertThat(roles.length).isGreaterThanOrEqualTo(3);
        assertThat(roles).contains(RoleDto.fromModel(DEVELOPER_ROLE()));
        assertThat(roles).contains(RoleDto.fromModel(PRODUCT_OWNER_ROLE()));
        assertThat(roles).contains(RoleDto.fromModel(TESTER_ROLE()));
    }

    @Test
    void shouldGetRoleById() {
        Role expectedRole = DEVELOPER_ROLE();

        getRole(expectedRole.getId())
                .statusCode(200)
                .body("name", equalTo(expectedRole.getName()));
    }

    @Test
    void shouldFailToGetRoleById() {
        getRole(UUID_1)
                .validate(404, format("Role %s not found", UUID_1));
    }

    @Test
    void shouldGetRoleByUserIdAndTeamId() {
        Membership expectedMembership = DEFAULT_MEMBERSHIP();
        mockGetTeamById(mockServer, ORDINARY_CORAL_LYNX_TEAM_UUID, ORDINARY_CORAL_LYNX_TEAM());
        mockGetUserById(mockServer, GIANNI_USER_UUID, GIANNI_USER());
        createMembership(expectedMembership)
                .statusCode(201);

        getRole(expectedMembership.getUserId(), expectedMembership.getTeamId())
                .statusCode(200)
                .body("name", equalTo(expectedMembership.getRole().getName()));
    }

    @Test
    void shouldFailToGetRoleByUserIdAndTeamIdWhenMissingUserId() {
        getRole(null, ORDINARY_CORAL_LYNX_TEAM_UUID)
                .validate(400, "Bad Request");
    }

    @Test
    void shouldFailToGetRoleByUserIdAndTeamIdWhenMissingTeamId() {
        getRole(GIANNI_USER_UUID, null)
                .validate(400, "Bad Request");
    }

    @Test
    void shouldFailToGetRoleByUserIdAndTeamIdWhenItDoesNotExist() {
        mockGetTeamById(mockServer, UUID_1, null);
        getRole(GIANNI_USER_UUID, UUID_1)
                .validate(404, format("Team %s not found", UUID_1));
    }

    @Test
    void shouldGetRolesByFilter() {
        Membership expectedMembership = DEFAULT_MEMBERSHIP();
        mockGetTeamById(mockServer, ORDINARY_CORAL_LYNX_TEAM_UUID, ORDINARY_CORAL_LYNX_TEAM());
        mockGetUserById(mockServer, GIANNI_USER_UUID, GIANNI_USER());
        createMembership(expectedMembership)
                .statusCode(201);

        Map<String, UUID> params = new HashMap<>();
        params.put("teamMemberId", expectedMembership.getUserId());
        params.put("teamId", expectedMembership.getTeamId());

        RoleDto[] roles =
                getRolesByFilters(params)
                        .statusCode(200)
                        .extract().as(RoleDto[].class);

        assertThat(roles.length).isEqualTo(1);
        assertThat(roles).contains(RoleDto.fromModel(DEVELOPER_ROLE()));
    }

    @Test
    void shouldGetAllRolesByFilterWhenUserIdAndTeamIdNull() {
        Map<String, UUID> params = new HashMap<>();
        RoleDto[] roles =
                getRolesByFilters(params)
                        .statusCode(200)
                        .extract().as(RoleDto[].class);

        assertThat(roles.length).isGreaterThanOrEqualTo(3);
        assertThat(roles).contains(RoleDto.fromModel(DEVELOPER_ROLE()));
        assertThat(roles).contains(RoleDto.fromModel(PRODUCT_OWNER_ROLE()));
        assertThat(roles).contains(RoleDto.fromModel(TESTER_ROLE()));
    }

    @Test
    void shouldFailToGetRolesByFilterWhenMissingTeam() {
        mockGetTeamById(mockServer, UUID_1, null);
        mockGetUserById(mockServer, GIANNI_USER_UUID, GIANNI_USER());

        Map<String, UUID> params = new HashMap<>();
        params.put("teamMemberId", GIANNI_USER_UUID);
        params.put("teamId", UUID_1);

        getRolesByFilters(params)
                .validate(404, format("Team %s not found", UUID_1));
    }

    @Test
    void shouldFailToGetRolesByFilterWhenMissingUser() {
        mockGetTeamById(mockServer, ORDINARY_CORAL_LYNX_TEAM_UUID, ORDINARY_CORAL_LYNX_TEAM());
        mockGetUserById(mockServer, GIANNI_USER_UUID, null);

        Map<String, UUID> params = new HashMap<>();
        params.put("teamMemberId", GIANNI_USER_UUID);
        params.put("teamId", ORDINARY_CORAL_LYNX_TEAM_UUID);

        getRolesByFilters(params)
                .validate(404, format("User %s not found", GIANNI_USER_UUID));
    }

    @Test
    void shouldGetRolesByFilterWhenOnlyUserIdInParams() {
        Membership expectedMembership = DEFAULT_MEMBERSHIP();
        mockGetTeamById(mockServer, ORDINARY_CORAL_LYNX_TEAM_UUID, ORDINARY_CORAL_LYNX_TEAM());
        mockGetUserById(mockServer, GIANNI_USER_UUID, GIANNI_USER());
        createMembership(expectedMembership)
                .statusCode(201);

        Map<String, UUID> params = new HashMap<>();
        params.put("teamMemberId", expectedMembership.getUserId());

        RoleDto[] roles =
                getRolesByFilters(params)
                        .statusCode(200)
                        .extract().as(RoleDto[].class);

        assertThat(roles.length).isEqualTo(1);
        assertThat(roles).contains(RoleDto.fromModel(DEVELOPER_ROLE()));
    }

    @Test
    void shouldGetRolesByFilterWhenOnlyTeamIdInParams() {
        Membership expectedMembership = DEFAULT_MEMBERSHIP();
        mockGetTeamById(mockServer, ORDINARY_CORAL_LYNX_TEAM_UUID, ORDINARY_CORAL_LYNX_TEAM());
        mockGetUserById(mockServer, GIANNI_USER_UUID, GIANNI_USER());
        createMembership(expectedMembership)
                .statusCode(201);

        Map<String, UUID> params = new HashMap<>();
        params.put("teamId", expectedMembership.getTeamId());

        RoleDto[] roles =
                getRolesByFilters(params)
                        .statusCode(200)
                        .extract().as(RoleDto[].class);

        assertThat(roles.length).isEqualTo(1);
        assertThat(roles).contains(RoleDto.fromModel(DEVELOPER_ROLE()));
    }
}
