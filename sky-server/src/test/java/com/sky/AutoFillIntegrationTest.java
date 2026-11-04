package com.sky;

import com.sky.context.BaseContext;
import com.sky.entity.Category;
import com.sky.entity.Employee;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.EmployeeMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

/** 通过 Spring 中真实的 MyBatis 代理验证切面，不直接调用切面方法。 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class AutoFillIntegrationTest {
    @Autowired EmployeeMapper employees;
    @Autowired CategoryMapper categories;
    @Autowired JdbcTemplate jdbc;
    private final LocalDateTime oldTime=LocalDateTime.of(2000,1,1,0,0);

    @BeforeEach @AfterEach
    void clean() {
        BaseContext.removeCurrentId();
        for(String table:new String[]{"dish","setmeal","category","orders","employee"}) jdbc.update("DELETE FROM "+table);
    }
    Employee newEmployee() {
        return Employee.builder().name("自动填充测试").username("autofill")
                .password("hash").phone("13800000000").sex("1")
                .idNumber("000000000000000000").status(1).build();
    }
    @Test void insertUsesOneTimestampAndTheActualCurrentOperator() {
        Employee employee=newEmployee();
        employee.setCreateTime(oldTime);employee.setUpdateTime(oldTime);
        employee.setCreateUser(999L);employee.setUpdateUser(999L);
        BaseContext.setCurrentId(77L);
        employees.insert(employee);
        BaseContext.removeCurrentId();
        Employee stored=employees.getById(employee.getId());
        assertThat(stored.getCreateUser()).isEqualTo(77L);
        assertThat(stored.getUpdateUser()).isEqualTo(77L);
        assertThat(stored.getCreateTime()).isAfter(oldTime);
        assertThat(stored.getUpdateTime()).isEqualTo(stored.getCreateTime());
    }
    @Test void employeeEditingAndStatusChangesKeepOriginalCreationFields() {
        BaseContext.setCurrentId(77L);
        Employee employee=newEmployee();employees.insert(employee);
        jdbc.update("UPDATE employee SET create_time=?,update_time=? WHERE id=?",oldTime,oldTime,employee.getId());
        BaseContext.setCurrentId(88L);
        employee.setName("编辑后");employees.updateProfile(employee);
        Employee stored=employees.getById(employee.getId());
        assertThat(stored.getName()).isEqualTo("编辑后");
        assertThat(stored.getCreateTime()).isEqualTo(oldTime);
        assertThat(stored.getCreateUser()).isEqualTo(77L);
        assertThat(stored.getUpdateTime()).isAfter(oldTime);
        assertThat(stored.getUpdateUser()).isEqualTo(88L);
        BaseContext.setCurrentId(99L);
        employees.updateStatus(Employee.builder().id(employee.getId()).status(0).build());
        stored=employees.getById(employee.getId());
        assertThat(stored.getStatus()).isZero();
        assertThat(stored.getCreateTime()).isEqualTo(oldTime);
        assertThat(stored.getCreateUser()).isEqualTo(77L);
        assertThat(stored.getUpdateUser()).isEqualTo(99L);
        assertThat(stored.getPassword()).isEqualTo("hash");
    }
    @Test void categoryInsertEditAndStatusUseTheSameAnnotationContract() {
        BaseContext.setCurrentId(77L);
        Category category=Category.builder().name("测试分类").type(1).sort(0).status(0).build();
        categories.insert(category);
        Category stored=categories.getById(category.getId());
        assertThat(stored.getCreateUser()).isEqualTo(77L);
        assertThat(stored.getCreateTime()).isEqualTo(stored.getUpdateTime());
        LocalDateTime created=stored.getCreateTime();
        BaseContext.setCurrentId(88L);
        category.setName("编辑分类");categories.update(category);
        assertThat(categories.getById(category.getId()).getUpdateUser()).isEqualTo(88L);
        BaseContext.setCurrentId(99L);
        categories.updateStatus(Category.builder().id(category.getId()).status(1).build());
        stored=categories.getById(category.getId());
        assertThat(stored.getStatus()).isEqualTo(1);
        assertThat(stored.getCreateUser()).isEqualTo(77L);
        assertThat(stored.getCreateTime()).isEqualTo(created);
        assertThat(stored.getUpdateUser()).isEqualTo(99L);
    }
    @Test void missingCurrentOperatorPreventsAnUnauditedWriteButAllowsReads() {
        assertThatThrownBy(()->employees.insert(newEmployee())).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM employee",Integer.class)).isZero();
        assertThat(employees.getByUsername("missing")).isNull();
    }
}
