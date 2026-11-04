package com.sky.utils;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.*;
import com.sky.exception.BaseException;
import com.sky.properties.AliOssProperties;
import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.function.Supplier;
import org.springframework.util.StringUtils;

/** OSS 客户端按需创建，不依赖 Bucket 公开读权限。 */
public class AliOssUtil {
    private final AliOssProperties properties;
    private final Supplier<OSS> clientFactory;
    public AliOssUtil(AliOssProperties properties) { this(properties,()->createClient(properties)); }
    public AliOssUtil(AliOssProperties properties,Supplier<OSS> clientFactory) {
        this.properties=properties;this.clientFactory=clientFactory;
    }
    public static OSS createClient(AliOssProperties p) {
        if(!StringUtils.hasText(p.getAccessKeyId()) || !StringUtils.hasText(p.getAccessKeySecret()))
            throw new BaseException("请先运行本地 OSS 配置工具，设置上传凭证");
        if(!StringUtils.hasText(p.getBucketName()) || !StringUtils.hasText(p.getEndpoint()) || !p.getEndpoint().startsWith("https://"))
            throw new BaseException("OSS Bucket 或 HTTPS Endpoint 配置不完整");
        ClientBuilderConfiguration configuration=new ClientBuilderConfiguration();
        configuration.setSignatureVersion(SignVersion.V4);
        configuration.setConnectionTimeout(10000);configuration.setSocketTimeout(20000);
        return OSSClientBuilder.create().endpoint(p.getEndpoint()).region(p.getRegion())
            .credentialsProvider(new DefaultCredentialProvider(p.getAccessKeyId(),p.getAccessKeySecret()))
            .clientConfiguration(configuration).build();
    }
    public String upload(byte[] bytes,String objectKey,String contentType) {
        OSS client=clientFactory.get();
        try {
            ObjectMetadata metadata=new ObjectMetadata();
            metadata.setContentType(contentType);metadata.setContentLength(bytes.length);
            client.putObject(new PutObjectRequest(properties.getBucketName(),objectKey,new ByteArrayInputStream(bytes),metadata));
            return signedUrl(client,objectKey);
        } catch(OSSException ex) {
            throw new BaseException("OSS 上传失败，请检查凭证权限、Bucket 和地域配置");
        } catch(ClientException ex) {
            throw new BaseException("无法连接 OSS，请检查网络后重试");
        } finally {client.shutdown();}
    }
    public String fileUrl(String objectKey) {
        OSS client=clientFactory.get();
        try {return signedUrl(client,objectKey);} finally {client.shutdown();}
    }
    private String signedUrl(OSS client,String objectKey) {
        long seconds=properties.getUrlTtlSeconds();
        if(seconds<1 || seconds>3600)throw new BaseException("图片链接有效期应在1至3600秒之间");
        return client.generatePresignedUrl(properties.getBucketName(),objectKey,new Date(System.currentTimeMillis()+seconds*1000)).toExternalForm();
    }
}
