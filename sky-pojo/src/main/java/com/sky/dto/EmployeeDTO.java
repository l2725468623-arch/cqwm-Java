package com.sky.dto;
import javax.validation.constraints.*;
import lombok.Data;
@Data
public class EmployeeDTO {
    @Positive private Long id;
    @NotBlank(message="请输入用户名") @Size(min=2,max=32,message="用户名长度应为2至32个字符")
    private String username;
    @NotBlank(message="请输入员工姓名") @Size(max=32) private String name;
    @NotBlank(message="请输入手机号") @Pattern(regexp="1\\d{10}",message="请输入正确的11位手机号")
    private String phone;
    @NotBlank(message="请选择性别") @Pattern(regexp="[01]",message="性别只能是0或1") private String sex;
    @NotBlank(message="请输入身份证号")
    @Pattern(regexp="(?:\\d{15}|\\d{17}[\\dXx])",message="身份证号格式不正确") private String idNumber;
}
