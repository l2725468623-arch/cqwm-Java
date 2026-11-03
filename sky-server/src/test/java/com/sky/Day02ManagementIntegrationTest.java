package com.sky;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class Day02ManagementIntegrationTest {
    @Autowired TestRestTemplate client;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    String token;
    @BeforeEach void setup() throws Exception {
        clean();
        jdbc.update("INSERT INTO employee (id,name,username,password,phone,sex,id_number,status,create_time) VALUES " +
            "(1,'管理员','admin','e10adc3949ba59abbe56e057f20f883e','13800000000','1','000000000000000000',1,CURRENT_TIMESTAMP)");
        token=body(request(HttpMethod.POST,"/admin/employee/login",map("username","admin","password","123456")))
            .path("data").path("token").asText();
    }
    @AfterEach void clean() {
        for(String table:new String[]{"dish","setmeal","category","orders","employee"}) jdbc.update("DELETE FROM "+table);
    }
    Map<String,Object> map(Object... values) {
        Map<String,Object> result=new LinkedHashMap<>();
        for(int i=0;i<values.length;i+=2)result.put((String)values[i],values[i+1]);
        return result;
    }
    Map<String,Object> employee(String username) {
        return map("username",username,"name","测试员工","phone","13912345678","sex","1","idNumber","110101199001010011");
    }
    Map<String,Object> category(String name,int type,int sort) {return map("name",name,"type",type,"sort",sort);}
    ResponseEntity<String> request(HttpMethod method,String path,Object data) throws Exception {
        HttpHeaders headers=new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        if(token!=null)headers.set("token",token);
        return client.exchange(path,method,new HttpEntity<>(data==null?null:json.writeValueAsString(data),headers),String.class);
    }
    JsonNode body(ResponseEntity<String> response) throws Exception {return json.readTree(response.getBody());}
    JsonNode success(HttpMethod method,String path,Object data) throws Exception {
        ResponseEntity<String> response=request(method,path,data);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body=body(response);assertThat(body.path("code").asInt()).as(body.toString()).isEqualTo(1);
        return body.path("data");
    }
    long employeeId(String username) {return jdbc.queryForObject("SELECT id FROM employee WHERE username=?",Long.class,username);}
    long categoryId(String name) {return jdbc.queryForObject("SELECT id FROM category WHERE name=?",Long.class,name);}

    @Test void newEmployeeHasHashedDefaultPasswordAndRealOperatorAudit() throws Exception {
        success(HttpMethod.POST,"/admin/employee",employee("zhangsan"));
        Map<String,Object> row=jdbc.queryForMap("SELECT * FROM employee WHERE username='zhangsan'");
        assertThat(row.get("password")).isEqualTo("e10adc3949ba59abbe56e057f20f883e");
        assertThat(((Number)row.get("status")).intValue()).isEqualTo(1);
        assertThat(((Number)row.get("create_user")).longValue()).isEqualTo(1L);
        assertThat(row.get("create_time")).isNotNull();
        JsonNode login=success(HttpMethod.POST,"/admin/employee/login",map("username","zhangsan","password","123456"));
        assertThat(login.path("token").asText()).isNotBlank();
    }
    @Test void duplicateAndInvalidEmployeeInputsAreRejected() throws Exception {
        assertThat(body(request(HttpMethod.POST,"/admin/employee",employee("admin"))).path("code").asInt()).isZero();
        Map<String,Object> bad=employee("bad-user");bad.put("phone","123");
        assertThat(request(HttpMethod.POST,"/admin/employee",bad).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM employee",Integer.class)).isEqualTo(1);
    }
    @Test void editAndStatusChangesPreservePasswordAndCreationAudit() throws Exception {
        success(HttpMethod.POST,"/admin/employee",employee("lisi"));long id=employeeId("lisi");
        Map<String,Object> changed=employee("lisi_new");changed.put("id",id);changed.put("name","李四");
        success(HttpMethod.PUT,"/admin/employee",changed);
        JsonNode detail=success(HttpMethod.GET,"/admin/employee/"+id,null);
        assertThat(detail.path("name").asText()).isEqualTo("李四");
        assertThat(detail.path("idNumber").asText()).isEqualTo("110101199001010011");
        assertThat(detail.has("password")).isFalse();
        success(HttpMethod.POST,"/admin/employee/status/0?id="+id,null);
        assertThat(body(request(HttpMethod.POST,"/admin/employee/login",map("username","lisi_new","password","123456"))).path("code").asInt()).isZero();
        success(HttpMethod.POST,"/admin/employee/status/1?id="+id,null);
        success(HttpMethod.POST,"/admin/employee/login",map("username","lisi_new","password","123456"));
        assertThat(jdbc.queryForObject("SELECT create_user FROM employee WHERE id=?",Long.class,id)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT update_user FROM employee WHERE id=?",Long.class,id)).isEqualTo(1L);
    }
    @Test void administratorAndInvalidStatusAreProtected() throws Exception {
        for(String path:new String[]{"/admin/employee/status/0?id=1","/admin/employee/status/2?id=1"})
            assertThat(body(request(HttpMethod.POST,path,null)).path("code").asInt()).isZero();
        assertThat(body(request(HttpMethod.GET,"/admin/employee/999999",null)).path("code").asInt()).isZero();
    }
    @Test void allWritesRequireLogin() throws Exception {
        token=null;
        assertThat(request(HttpMethod.POST,"/admin/employee",employee("anonymous")).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(request(HttpMethod.POST,"/admin/category",category("未授权",1,1)).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    @Test void categoryLifecycleSupportsTypeFilterStatusSortingAndDelete() throws Exception {
        success(HttpMethod.POST,"/admin/category",category("热菜",1,2));long id=categoryId("热菜");
        assertThat(success(HttpMethod.GET,"/admin/category/list?type=1",null).size()).isZero();
        success(HttpMethod.POST,"/admin/category/status/1?id="+id,null);
        success(HttpMethod.POST,"/admin/category",category("单人套餐",2,1));
        assertThat(success(HttpMethod.GET,"/admin/category/list?type=1",null).size()).isEqualTo(1);
        assertThat(success(HttpMethod.GET,"/admin/category/page?type=2&page=1&pageSize=10",null).path("total").asInt()).isEqualTo(1);
        Map<String,Object> update=category("川菜",1,5);update.put("id",id);
        success(HttpMethod.PUT,"/admin/category",update);
        assertThat(success(HttpMethod.GET,"/admin/category/"+id,null).path("name").asText()).isEqualTo("川菜");
        assertThat(jdbc.queryForObject("SELECT create_user FROM category WHERE id=?",Long.class,id)).isEqualTo(1L);
        success(HttpMethod.POST,"/admin/category/status/0?id="+id,null);
        success(HttpMethod.DELETE,"/admin/category?id="+id,null);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM category WHERE id=?",Integer.class,id)).isZero();
    }
    @Test void linkedDishOrSetmealPreventsCategoryDeletion() throws Exception {
        for(String table:new String[]{"dish","setmeal"}) {
            success(HttpMethod.POST,"/admin/category",category(table,1,0));long id=categoryId(table);
            jdbc.update("INSERT INTO "+table+" (id,category_id) VALUES (?,?)",id,id);
            assertThat(body(request(HttpMethod.DELETE,"/admin/category?id="+id,null)).path("code").asInt()).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM category WHERE id=?",Integer.class,id)).isEqualTo(1);
        }
    }
    @Test void duplicateCategoryAndInvalidTypeAreRejected() throws Exception {
        success(HttpMethod.POST,"/admin/category",category("饮品",1,1));
        assertThat(body(request(HttpMethod.POST,"/admin/category",category("饮品",2,1))).path("code").asInt()).isZero();
        assertThat(request(HttpMethod.POST,"/admin/category",category("错误类型",3,0)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(request(HttpMethod.GET,"/admin/category/page?page=0",null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    @Test void longIdsAreReturnedAsExactStringsForBrowserEditing() throws Exception {
        long id=9007199254740993L;
        jdbc.update("INSERT INTO employee (id,name,username,password,status) VALUES (?,'大ID员工','largeid','hash',1)",id);
        assertThat(success(HttpMethod.GET,"/admin/employee/"+id,null).path("id").asText()).isEqualTo("9007199254740993");
    }
}
