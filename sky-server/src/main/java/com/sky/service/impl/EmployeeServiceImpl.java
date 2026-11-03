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

    public void save(com.sky.dto.EmployeeDTO dto) {
        Employee employee = new Employee();
        BeanUtils.copyProperties(dto, employee);
        employee.setId(null);
        employee.setPassword(DigestUtils.md5DigestAsHex(com.sky.constant.PasswordConstant.DEFAULT_PASSWORD.getBytes(StandardCharsets.UTF_8)));
        employee.setStatus(StatusConstant.ENABLE);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        employee.setCreateTime(now); employee.setUpdateTime(now);
        employee.setCreateUser(BaseContext.getCurrentId()); employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.insert(employee);
    }
    public void update(com.sky.dto.EmployeeDTO dto) {
        requireEmployee(dto.getId());
        Employee employee = new Employee();
        BeanUtils.copyProperties(dto, employee);
        employee.setUpdateTime(java.time.LocalDateTime.now());
        employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.updateProfile(employee);
    }
    public void startOrStop(Integer status, Long id) {
        if (status == null || (status != 0 && status != 1)) throw new BaseException("状态只能为0或1");
        Employee employee = requireEmployee(id);
        if (status == 0 && ("admin".equals(employee.getUsername()) || id.equals(BaseContext.getCurrentId())))
            throw new BaseException("不能禁用管理员或当前登录账号");
        employeeMapper.updateStatus(id, status, java.time.LocalDateTime.now(), BaseContext.getCurrentId());
    }
    public EmployeeVO getById(Long id) { return toVO(requireEmployee(id)); }
    private Employee requireEmployee(Long id) {
        if (id == null || id < 1) throw new BaseException("请选择有效员工");
        Employee employee = employeeMapper.getById(id);
        if (employee == null) throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        return employee;
    }
    private EmployeeVO toVO(Employee employee) {
        EmployeeVO vo = new EmployeeVO();
        BeanUtils.copyProperties(employee, vo);
        return vo;
    }
}
