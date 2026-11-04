package com.sky.service;
import com.sky.exception.BaseException;
import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import com.sky.vo.FileUploadVO;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
@Service
@RequiredArgsConstructor
public class OssStorageService {
    private final AliOssUtil aliOssUtil;
    private final AliOssProperties properties;
    public FileUploadVO upload(MultipartFile file) {
        if(file==null || file.isEmpty())throw new BaseException("请选择要上传的图片");
        if(file.getSize()>5*1024*1024)throw new BaseException("图片大小不能超过5MB");
        byte[] bytes;
        try {bytes=file.getBytes();}catch(IOException ex){throw new BaseException("读取图片失败，请重新选择文件");}
        String extension;
        if(bytes.length>=8 && (bytes[0]&255)==137 && bytes[1]==80 && bytes[2]==78 && bytes[3]==71 && bytes[4]==13 && bytes[5]==10 && bytes[6]==26 && bytes[7]==10)extension="png";
        else if(bytes.length>=3 && (bytes[0]&255)==255 && (bytes[1]&255)==216 && (bytes[2]&255)==255)extension="jpg";
        else if(bytes.length>=12 && bytes[0]==82 && bytes[1]==73 && bytes[2]==70 && bytes[3]==70 && bytes[8]==87 && bytes[9]==69 && bytes[10]==66 && bytes[11]==80)extension="webp";
        else throw new BaseException("仅支持 PNG、JPEG 或 WebP 图片");
        String key="sky-take-out/images/"+UUID.randomUUID()+"."+extension;
        String url=aliOssUtil.upload(bytes,key,extension.equals("jpg")?"image/jpeg":"image/"+extension);
        return new FileUploadVO(key,url,properties.getUrlTtlSeconds());
    }
    public FileUploadVO fileUrl(String key) {
        if(key==null || !key.matches("sky-take-out/images/[0-9a-f-]{36}\\.(png|jpg|webp)"))
            throw new BaseException("图片标识不正确");
        return new FileUploadVO(key,aliOssUtil.fileUrl(key),properties.getUrlTtlSeconds());
    }
}
