package com.sky.dto;
import java.io.Serializable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;
@Data
public class EmployeeLoginDTO implements Serializable {
    @NotBlank(message = "请输入用户名")
    @Size(max = 32, message = "用户名最多32个字符")
    private String username;
    @NotBlank(message = "请输入密码")
    @Size(max = 128, message = "密码长度超出限制")
    private String password;
}
