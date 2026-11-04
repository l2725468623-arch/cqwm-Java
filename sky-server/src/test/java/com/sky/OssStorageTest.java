package com.sky;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.PutObjectRequest;
import com.sky.exception.BaseException;
import com.sky.properties.AliOssProperties;
import com.sky.service.OssStorageService;
import com.sky.utils.AliOssUtil;
import com.sky.vo.FileUploadVO;
import java.net.URL;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class OssStorageTest {
    static final byte[] PNG=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jQ1kAAAAASUVORK5CYII=");
    @Test void imageTypeComesFromBytesAndKeysAreGeneratedByServer() {
        AliOssUtil util=mock(AliOssUtil.class);when(util.upload(any(),anyString(),anyString())).thenReturn("https://example.invalid/image.png");
        FileUploadVO result=new OssStorageService(util,new AliOssProperties()).upload(new MockMultipartFile("file","../../evil.svg","text/html",PNG));
        assertThat(result.getObjectKey()).matches("sky-take-out/images/[0-9a-f-]{36}\\.png");
        verify(util).upload(eq(PNG),eq(result.getObjectKey()),eq("image/png"));
        assertThat(result.getExpiresInSeconds()).isEqualTo(900);
    }
    @Test void forgedImageIsRejectedBeforeStorage() {
        AliOssUtil util=mock(AliOssUtil.class);
        assertThatThrownBy(()->new OssStorageService(util,new AliOssProperties()).upload(new MockMultipartFile("file","image.png","image/png","<script>x</script>".getBytes())))
            .isInstanceOf(BaseException.class);
        verifyNoInteractions(util);
    }
    @Test void emptyImageIsRejected() {
        AliOssUtil util=mock(AliOssUtil.class);
        assertThatThrownBy(()->new OssStorageService(util,new AliOssProperties()).upload(new MockMultipartFile("file",new byte[0])))
            .isInstanceOf(BaseException.class);verifyNoInteractions(util);
    }
    @Test void oversizedImageIsRejectedBeforeReadingBytes() throws Exception {
        MultipartFile file=mock(MultipartFile.class);when(file.isEmpty()).thenReturn(false);when(file.getSize()).thenReturn(5*1024*1024L+1);
        assertThatThrownBy(()->new OssStorageService(mock(AliOssUtil.class),new AliOssProperties()).upload(file)).isInstanceOf(BaseException.class);
        verify(file,never()).getBytes();
    }
    @Test void refreshCannotReadArbitraryBucketKeys() {
        AliOssUtil util=mock(AliOssUtil.class);OssStorageService service=new OssStorageService(util,new AliOssProperties());
        for(String key:new String[]{"../secret","other-folder/image.png","sky-take-out/images/not-a-uuid.png"})
            assertThatThrownBy(()->service.fileUrl(key)).isInstanceOf(BaseException.class);
        verifyNoInteractions(util);
    }
    @Test void sdkUploadSetsMetadataSignsUrlAndClosesClient() throws Exception {
        OSS client=mock(OSS.class);when(client.generatePresignedUrl(anyString(),anyString(),any(Date.class)))
            .thenReturn(new URL("https://example.invalid/image.png?signature=test"));
        AliOssUtil util=new AliOssUtil(new AliOssProperties(),()->client);
        assertThat(util.upload(PNG,"key.png","image/png")).startsWith("https://");
        ArgumentCaptor<PutObjectRequest> request=ArgumentCaptor.forClass(PutObjectRequest.class);verify(client).putObject(request.capture());
        assertThat(request.getValue().getBucketName()).isEqualTo("lay-java");
        assertThat(request.getValue().getMetadata().getContentType()).isEqualTo("image/png");
        verify(client).shutdown();
    }
    @Test void sdkFailureIsSanitizedAndClientStillCloses() {
        OSS client=mock(OSS.class);doThrow(new OSSException("sensitive-internal-detail")).when(client).putObject(any(PutObjectRequest.class));
        assertThatThrownBy(()->new AliOssUtil(new AliOssProperties(),()->client).upload(PNG,"key.png","image/png"))
            .isInstanceOf(BaseException.class).hasMessageNotContaining("sensitive-internal-detail");
        verify(client).shutdown();
    }
    @Test void missingCredentialsAreExplicitAndPropertiesDoNotPrintSecrets() {
        AliOssProperties properties=new AliOssProperties();
        assertThatThrownBy(()->AliOssUtil.createClient(properties)).isInstanceOf(BaseException.class);
        properties.setAccessKeyId("test-only-id");properties.setAccessKeySecret("test-only-secret");
        assertThat(properties.toString()).doesNotContain("test-only-id","test-only-secret");
    }
}
