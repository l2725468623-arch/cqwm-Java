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
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class DishManagementIntegrationTest {
    @Autowired TestRestTemplate client;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    String token;
    static final String IMAGE="sky-take-out/images/11111111-1111-4111-8111-111111111111.png";
    @BeforeEach void setup() throws Exception {
        clean();
        jdbc.update("INSERT INTO employee(id,name,username,password,status) VALUES(1,'管理员','admin','e10adc3949ba59abbe56e057f20f883e',1)");
        jdbc.update("INSERT INTO category(id,type,name,sort,status) VALUES(1,1,'热菜',0,1),(2,1,'饮品',1,1),(3,2,'套餐',0,1),(4,1,'禁用分类',0,0)");
        token=body(request(HttpMethod.POST,"/admin/employee/login",map("username","admin","password","123456"))).path("data").path("token").asText();
    }
    @AfterEach void clean() {
        for(String table:new String[]{"dish_flavor","setmeal_dish","dish","setmeal","category","orders","employee"})jdbc.update("DELETE FROM "+table);
    }
    Map<String,Object> map(Object... args){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<args.length;i+=2)m.put((String)args[i],args[i+1]);return m;}
    Map<String,Object> dish(String name){return map("name",name,"categoryId",1,"price","18.50","image",IMAGE,"description","新鲜现做","status",0,"flavors",Arrays.asList(map("name","辣度","value","[\"不辣\",\"微辣\"]")));}
    ResponseEntity<String> request(HttpMethod method,String path,Object data) throws Exception {
        HttpHeaders headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_JSON);if(token!=null)headers.set("token",token);
        return client.exchange(path,method,new HttpEntity<>(data==null?null:json.writeValueAsString(data),headers),String.class);
    }
    JsonNode body(ResponseEntity<String> response) throws Exception{return json.readTree(response.getBody());}
    JsonNode ok(HttpMethod method,String path,Object data) throws Exception {
        ResponseEntity<String> response=request(method,path,data);assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode result=body(response);assertThat(result.path("code").asInt()).as(result.toString()).isEqualTo(1);return result.path("data");
    }
    long create(String name) throws Exception {ok(HttpMethod.POST,"/admin/dish",dish(name));return id(name);}
    long id(String name){return jdbc.queryForObject("SELECT id FROM dish WHERE name=?",Long.class,name);}
    int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    @Test void createStoresDishFlavorsAuditAndDurableImageKey() throws Exception {
        long id=create("宫保鸡丁");JsonNode detail=ok(HttpMethod.GET,"/admin/dish/"+id,null);
        assertThat(detail.path("categoryName").asText()).isEqualTo("热菜");
        assertThat(detail.path("image").asText()).isEqualTo(IMAGE);
        assertThat(detail.path("flavors").size()).isEqualTo(1);
        assertThat(detail.path("flavors").get(0).path("value").asText()).isEqualTo("[\"不辣\",\"微辣\"]");
        assertThat(detail.path("createUser").asLong()).isEqualTo(1L);
        assertThat(detail.path("updateUser").asLong()).isEqualTo(1L);
    }
    @Test void editReplacesFlavorsAndPreservesCreationInformation() throws Exception {
        long id=create("原菜品");Object created=jdbc.queryForObject("SELECT create_time FROM dish WHERE id=?",Object.class,id);
        Map<String,Object> changed=dish("新菜品");changed.put("id",id);changed.put("price","25.00");changed.put("flavors",Arrays.asList(map("name","甜度","value","[\"少糖\",\"无糖\"]")));
        ok(HttpMethod.PUT,"/admin/dish",changed);JsonNode detail=ok(HttpMethod.GET,"/admin/dish/"+id,null);
        assertThat(detail.path("name").asText()).isEqualTo("新菜品");
        assertThat(detail.path("flavors").get(0).path("name").asText()).isEqualTo("甜度");
        assertThat(count("dish_flavor")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT create_time FROM dish WHERE id=?",Object.class,id)).isEqualTo(created);
    }
    @Test void pageSupportsFiltersAndRealPagination() throws Exception {
        create("香辣鸡丁");create("香辣鱼");Map<String,Object> drink=dish("柠檬茶");drink.put("categoryId",2);drink.put("status",1);ok(HttpMethod.POST,"/admin/dish",drink);
        JsonNode second=ok(HttpMethod.GET,"/admin/dish/page?page=2&pageSize=2",null);
        assertThat(second.path("total").asInt()).isEqualTo(3);assertThat(second.path("records").size()).isEqualTo(1);
        JsonNode filtered=ok(HttpMethod.GET,"/admin/dish/page?name=香辣&categoryId=1&status=0",null);
        assertThat(filtered.path("total").asInt()).isEqualTo(2);
        assertThat(ok(HttpMethod.GET,"/admin/dish/page?status=1",null).path("records").get(0).path("name").asText()).isEqualTo("柠檬茶");
        assertThat(ok(HttpMethod.GET,"/admin/dish/page?name=not-present",null).path("total").asInt()).isZero();
    }
    @Test void singleAndBatchDeletionRemoveRelatedFlavors() throws Exception {
        long a=create("甲"),b=create("乙"),c=create("丙");ok(HttpMethod.DELETE,"/admin/dish?ids="+a,null);
        assertThat(count("dish_flavor")).isEqualTo(2);
        ok(HttpMethod.DELETE,"/admin/dish?ids="+b+","+c+","+b,null);assertThat(count("dish")).isZero();assertThat(count("dish_flavor")).isZero();
    }
    @Test void onSaleMemberMakesTheWholeBatchRemain() throws Exception {
        long a=create("停售菜"),b=create("起售菜");ok(HttpMethod.POST,"/admin/dish/status/1?id="+b,null);
        assertThat(body(request(HttpMethod.DELETE,"/admin/dish?ids="+a+","+b,null)).path("code").asInt()).isZero();
        assertThat(count("dish")).isEqualTo(2);assertThat(count("dish_flavor")).isEqualTo(2);
    }
    @Test void setmealRelationBlocksDeletionWithoutRemovingAnyFlavors() throws Exception {
        long id=create("套餐菜品");jdbc.update("INSERT INTO setmeal_dish(setmeal_id,dish_id) VALUES(1,?)",id);
        assertThat(body(request(HttpMethod.DELETE,"/admin/dish?ids="+id,null)).path("code").asInt()).isZero();
        assertThat(count("dish")).isEqualTo(1);assertThat(count("dish_flavor")).isEqualTo(1);
    }
    @Test void duplicateNameEditRollsBackDishAndFlavorChanges() throws Exception {
        create("已存在");long id=create("原名称");Map<String,Object> changed=dish("已存在");changed.put("id",id);changed.put("flavors",Collections.emptyList());
        assertThat(body(request(HttpMethod.PUT,"/admin/dish",changed)).path("code").asInt()).isZero();
        assertThat(ok(HttpMethod.GET,"/admin/dish/"+id,null).path("name").asText()).isEqualTo("原名称");
        assertThat(count("dish_flavor")).isEqualTo(2);
    }
    @Test void invalidCategoryRejectsWriteAndFlavorInsert() throws Exception {
        for(int category:new int[]{3,4,999}){Map<String,Object> data=dish("错误分类");data.put("categoryId",category);assertThat(body(request(HttpMethod.POST,"/admin/dish",data)).path("code").asInt()).isZero();}
        assertThat(count("dish")).isZero();assertThat(count("dish_flavor")).isZero();
    }
    @Test void invalidPriceAndImageAreRejected() throws Exception {
        for(String price:new String[]{"-1","0","1.234","100000000"}){Map<String,Object> data=dish("错误价格");data.put("price",price);assertThat(request(HttpMethod.POST,"/admin/dish",data).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);}
        Map<String,Object> bad=dish("错误图片");bad.put("image","https://example.invalid/signed-url");assertThat(request(HttpMethod.POST,"/admin/dish",bad).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(count("dish")).isZero();
    }
    @Test void malformedAndDuplicateFlavorsLeaveNoPartialDish() throws Exception {
        for(String value:new String[]{"bad-json","[]","[1]","[\"微辣\",\"微辣\"]"}){Map<String,Object> data=dish("口味测试");data.put("flavors",Arrays.asList(map("name","辣度","value",value)));assertThat(body(request(HttpMethod.POST,"/admin/dish",data)).path("code").asInt()).isZero();}
        Map<String,Object> data=dish("重名口味");data.put("flavors",Arrays.asList(map("name","辣度","value","[\"微辣\"]"),map("name","辣度","value","[\"中辣\"]")));
        assertThat(body(request(HttpMethod.POST,"/admin/dish",data)).path("code").asInt()).isZero();assertThat(count("dish")).isZero();
    }
    @Test void unauthorizedRequestsAreBlocked() throws Exception {
        token=null;assertThat(request(HttpMethod.POST,"/admin/dish",dish("未登录")).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(request(HttpMethod.DELETE,"/admin/dish?ids=1",null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(request(HttpMethod.GET,"/admin/dish/page",null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    @Test void missingIdsAndInvalidPagingFailWithoutChangingData() throws Exception {
        long id=create("保留");
        assertThat(body(request(HttpMethod.GET,"/admin/dish/999999",null)).path("code").asInt()).isZero();
        assertThat(body(request(HttpMethod.DELETE,"/admin/dish?ids="+id+",999999",null)).path("code").asInt()).isZero();
        assertThat(request(HttpMethod.GET,"/admin/dish/page?page=0&pageSize=1000",null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(request(HttpMethod.DELETE,"/admin/dish?ids=-1",null)).path("code").asInt()).isZero();assertThat(count("dish")).isEqualTo(1);
    }
    @Test void statusChangesPreserveAuditAndEnableDeletionAfterStopping() throws Exception {
        long id=create("状态测试");Object created=jdbc.queryForObject("SELECT create_time FROM dish WHERE id=?",Object.class,id);
        ok(HttpMethod.POST,"/admin/dish/status/1?id="+id,null);assertThat(jdbc.queryForObject("SELECT status FROM dish WHERE id=?",Integer.class,id)).isEqualTo(1);
        ok(HttpMethod.POST,"/admin/dish/status/0?id="+id,null);assertThat(jdbc.queryForObject("SELECT create_time FROM dish WHERE id=?",Object.class,id)).isEqualTo(created);
        assertThat(jdbc.queryForObject("SELECT update_user FROM dish WHERE id=?",Long.class,id)).isEqualTo(1);
        assertThat(body(request(HttpMethod.POST,"/admin/dish/status/2?id="+id,null)).path("code").asInt()).isZero();ok(HttpMethod.DELETE,"/admin/dish?ids="+id,null);
    }
}
