package com.sky.service;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;
import com.sky.vo.EmployeeVO;
public interface EmployeeService {
    Employee login(EmployeeLoginDTO dto);
    EmployeeVO currentEmployee();
    PageResult pageQuery(EmployeePageQueryDTO dto);
}
