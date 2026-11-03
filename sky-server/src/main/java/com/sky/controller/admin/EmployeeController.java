package com.sky.controller.admin;
import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import com.sky.vo.EmployeeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@Api(tags = "员工相关接口")
@RestController
@RequestMapping("/admin/employee")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;
    private final JwtProperties jwtProperties;
    @ApiOperation("员工登录")
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@Valid @RequestBody EmployeeLoginDTO dto) {
        Employee employee = employeeService.login(dto);
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(jwtProperties.getAdminSecretKey(), jwtProperties.getAdminTtl(), claims);
        return Result.success(EmployeeLoginVO.builder().id(employee.getId())
                .name(employee.getName()).username(employee.getUsername()).token(token).build());
    }
    @ApiOperation("当前员工信息")
    @GetMapping("/profile")
    public Result<EmployeeVO> profile() { return Result.success(employeeService.currentEmployee()); }
    @ApiOperation("员工分页查询")
    @GetMapping("/page")
    public Result<PageResult> page(@Valid EmployeePageQueryDTO dto) {
        return Result.success(employeeService.pageQuery(dto));
    }
    @ApiOperation("员工退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() { return Result.success(); }
    @ApiOperation("新增员工")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody com.sky.dto.EmployeeDTO employeeDTO) {
        employeeService.save(employeeDTO);
        return Result.success();
    }
    @ApiOperation("启用或禁用员工账号")
    @PostMapping("/status/{status}")
    public Result<Void> startOrStop(@PathVariable Integer status, @RequestParam Long id) {
        employeeService.startOrStop(status, id);
        return Result.success();
    }
    @ApiOperation("按ID查询员工，编辑回显")
    @GetMapping("/{id}")
    public Result<EmployeeVO> getById(@PathVariable Long id) {
        return Result.success(employeeService.getById(id));
    }
    @ApiOperation("编辑员工")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody com.sky.dto.EmployeeDTO employeeDTO) {
        employeeService.update(employeeDTO);
        return Result.success();
    }
}
