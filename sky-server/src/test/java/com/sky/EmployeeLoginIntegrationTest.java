package com.sky;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.constant.JwtClaimsConstant;
import com.sky.utils.JwtUtil;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmployeeLoginIntegrationTest {
    @Autowired private TestRestTemplate client;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void resetIsolatedTestDatabase() {
        jdbc.update("DELETE FROM employee");
        jdbc.update("INSERT INTO employee (id,name,username,password,phone,sex,id_number,status,create_time) " +
                "VALUES (1,'管理员','admin','e10adc3949ba59abbe56e057f20f883e','13800000000','1','000000000000000000',1,CURRENT_TIMESTAMP)");
    }

    private ResponseEntity<String> login(String username, String password) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        java.util.Map<String,String> request = new java.util.HashMap<>();
        request.put("username", username);
        request.put("password", password);
        return client.postForEntity("/admin/employee/login",
                new HttpEntity<>(objectMapper.writeValueAsString(request), headers), String.class);
    }

    private ResponseEntity<String> authorizedGet(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token);
        return client.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private String token() throws Exception {
        JsonNode response = objectMapper.readTree(login("admin", "123456").getBody());
        assertThat(response.path("code").asInt()).isEqualTo(1);
        return response.path("data").path("token").asText();
    }

    @Test
    void successfulLoginAllowsProtectedReadsWithoutExposingPasswords() throws Exception {
        String token = token();
        assertThat(token).isNotBlank();
        ResponseEntity<String> profile = authorizedGet("/admin/employee/profile", token);
        assertThat(profile.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(profile.getBody()).path("data").path("username").asText()).isEqualTo("admin");
        assertThat(profile.getBody()).doesNotContain("password", "e10adc");
        ResponseEntity<String> page = authorizedGet("/admin/employee/page?page=1&pageSize=10", token);
        assertThat(objectMapper.readTree(page.getBody()).path("data").path("total").asInt()).isEqualTo(1);
        assertThat(page.getBody()).doesNotContain("password", "e10adc");
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        JsonNode body = objectMapper.readTree(login("admin", "wrong-password").getBody());
        assertThat(body.path("code").asInt()).isZero();
        assertThat(body.path("msg").asText()).isEqualTo("密码错误");
        assertThat(body.path("data").isNull()).isTrue();
    }

    @Test
    void unknownAndSqlInjectionUsernamesAreRejected() throws Exception {
        for (String name : new String[]{"missing", "admin' OR '1'='1"}) {
            JsonNode body = objectMapper.readTree(login(name, "123456").getBody());
            assertThat(body.path("code").asInt()).isZero();
            assertThat(body.path("msg").asText()).isEqualTo("账号不存在");
        }
    }

    @Test
    void disabledAccountCannotLogIn() throws Exception {
        jdbc.update("UPDATE employee SET status=0 WHERE id=1");
        JsonNode body = objectMapper.readTree(login("admin", "123456").getBody());
        assertThat(body.path("code").asInt()).isZero();
        assertThat(body.path("msg").asText()).isEqualTo("账号已被禁用");
    }

    @Test
    void missingMalformedAndExpiredTokensAreRejected() {
        assertThat(client.getForEntity("/admin/employee/profile", String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String expired = JwtUtil.createJWT("test-only-secret-for-isolated-h2-integration-tests", -10000,
                Collections.singletonMap(JwtClaimsConstant.EMP_ID, 1L));
        for (String invalid : new String[]{"invalid-token", expired}) {
            assertThat(authorizedGet("/admin/employee/profile", invalid).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void disablingAccountAlsoRejectsPreviouslyIssuedToken() throws Exception {
        String token = token();
        jdbc.update("UPDATE employee SET status=0 WHERE id=1");
        assertThat(authorizedGet("/admin/employee/profile", token).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginAndPaginationParametersAreValidated() throws Exception {
        assertThat(login("", "").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(authorizedGet("/admin/employee/page?page=0&pageSize=1000", token()).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void employeeSearchAndDashboardUseActualStoredData() throws Exception {
        String token = token();
        JsonNode filtered = objectMapper.readTree(authorizedGet("/admin/employee/page?name=not-present", token).getBody());
        assertThat(filtered.path("data").path("total").asInt()).isZero();
        JsonNode overview = objectMapper.readTree(authorizedGet("/admin/dashboard/overview", token).getBody()).path("data");
        assertThat(overview.path("employeeCount").asInt()).isEqualTo(1);
        assertThat(overview.path("dishCount").asInt()).isZero();
    }
}
