package com.sky.mapper;
import com.sky.entity.Employee;
import java.util.List;
import org.apache.ibatis.annotations.Param;
public interface EmployeeMapper {
    Employee getByUsername(@Param("username") String username);
    Employee getById(@Param("id") Long id);
    long count(@Param("name") String name);
    List<Employee> page(@Param("name") String name, @Param("offset") int offset, @Param("limit") int limit);
}
