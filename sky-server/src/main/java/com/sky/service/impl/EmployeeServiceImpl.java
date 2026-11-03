package com.sky.service.impl;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.*;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import com.sky.vo.EmployeeVO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeMapper employeeMapper;
    public Employee login(EmployeeLoginDTO dto) {
        Employee employee = employeeMapper.getByUsername(dto.getUsername());
        if (employee == null) throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        // 与课程数据库的 MD5 密码格式一致；不记录请求中的密码。
        String hash = DigestUtils.md5DigestAsHex(dto.getPassword().getBytes(StandardCharsets.UTF_8));
        if (employee.getPassword() == null || !MessageDigest.isEqual(
                hash.getBytes(StandardCharsets.UTF_8), employee.getPassword().getBytes(StandardCharsets.UTF_8))) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        if (!StatusConstant.ENABLE.equals(employee.getStatus())) {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }
        return employee;
    }
    public EmployeeVO currentEmployee() {
        Employee employee = employeeMapper.getById(BaseContext.getCurrentId());
        if (employee == null || !StatusConstant.ENABLE.equals(employee.getStatus())) {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }
        return toVO(employee);
    }
    public PageResult pageQuery(EmployeePageQueryDTO dto) {
        List<EmployeeVO> records = employeeMapper.page(dto.getName(),
                (dto.getPage() - 1) * dto.getPageSize(), dto.getPageSize())
                .stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult(employeeMapper.count(dto.getName()), records);
    }
    private EmployeeVO toVO(Employee employee) {
        EmployeeVO vo = new EmployeeVO();
        BeanUtils.copyProperties(employee, vo);
        return vo;
    }
}
