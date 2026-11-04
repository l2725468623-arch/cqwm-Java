package com.sky.vo;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class FileUploadVO {
    private String objectKey;
    private String url;
    private long expiresInSeconds;
}
