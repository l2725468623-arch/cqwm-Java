package com.sky.properties;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
@Data
@Component
@ConfigurationProperties(prefix="sky.alioss")
public class AliOssProperties {
    private String endpoint="https://oss-cn-guangzhou.aliyuncs.com";
    private String region="cn-guangzhou";
    @ToString.Exclude private String accessKeyId;
    @ToString.Exclude private String accessKeySecret;
    private String bucketName="lay-java";
    private long urlTtlSeconds=900;
}
