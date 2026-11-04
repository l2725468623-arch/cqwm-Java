package com.sky.tools;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
/** 独立连通性检查，不启动 Spring 或连接 MySQL，不输出凭证。 */
public class OssConnectionCheck {
    public static void main(String[] args) {
        AliOssProperties p=new AliOssProperties();
        p.setAccessKeyId(System.getenv("ALI_OSS_ACCESS_KEY_ID"));p.setAccessKeySecret(System.getenv("ALI_OSS_ACCESS_KEY_SECRET"));
        OSS client=null;String key="sky-take-out/connection-check/"+UUID.randomUUID()+".txt";
        boolean uploaded=false;
        try {
            client=AliOssUtil.createClient(p);
            byte[] content="cqwm-oss-connection-check".getBytes(StandardCharsets.UTF_8);
            ObjectMetadata metadata=new ObjectMetadata();metadata.setContentType("text/plain");metadata.setContentLength(content.length);
            client.putObject(p.getBucketName(),key,new ByteArrayInputStream(content),metadata);uploaded=true;
            if(client.getObjectMetadata(p.getBucketName(),key).getContentLength()!=content.length)throw new IllegalStateException();
            try{client.deleteObject(p.getBucketName(),key);System.out.println("OSS_CHECK_OK");}
            catch(RuntimeException cleanup){System.out.println("OSS_CHECK_OK_TEST_FILE_RETAINED");}
        } catch(OSSException ex) {System.out.println("OSS_CHECK_FAILED: "+ex.getErrorCode());System.exit(2);}
          catch(RuntimeException ex) {System.out.println(uploaded?"OSS_CHECK_WRITE_OK_READ_FAILED":"OSS_CHECK_FAILED");System.exit(3);}
        finally {if(client!=null)client.shutdown();}
    }
}
