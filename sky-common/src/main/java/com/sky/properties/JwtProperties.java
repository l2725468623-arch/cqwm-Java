package com.sky.properties;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "sky.jwt")
public class JwtProperties {
    @NotBlank(message = "请通过启动脚本设置 SKY_JWT_ADMIN_SECRET")
    private String adminSecretKey;
    @Min(1)
    private long adminTtl = 7200000L;
    @NotBlank
    private String adminTokenName = "token";
    private String userSecretKey;
    private long userTtl = 7200000L;
    private String userTokenName = "authentication";
}
