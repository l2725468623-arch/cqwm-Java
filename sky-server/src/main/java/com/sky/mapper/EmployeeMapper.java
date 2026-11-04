package com.sky.mapper;
import com.sky.annotation.AutoFill;
import com.sky.enumeration.OperationType;
import com.sky.entity.Employee;
import java.util.List;
import org.apache.ibatis.annotations.Param;
public interface EmployeeMapper {
    @AutoFill(OperationType.INSERT)
    int insert(Employee employee);
    @AutoFill(OperationType.UPDATE)
    int updateProfile(Employee employee);
    @AutoFill(OperationType.UPDATE)
    int updateStatus(Employee employee);
    Employee getByUsername(@Param("username") String username);
    Employee getById(@Param("id") Long id);
    long count(@Param("name") String name);
    List<Employee> page(@Param("name") String name, @Param("offset") int offset, @Param("limit") int limit);
}
