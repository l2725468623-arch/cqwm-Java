package com.sky.controller.admin;
import com.sky.result.Result;
import com.sky.service.OssStorageService;
import com.sky.vo.FileUploadVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@Api(tags="公共上传接口")
@RestController
@RequestMapping("/admin/common")
@RequiredArgsConstructor
public class CommonController {
    private final OssStorageService storageService;
    @ApiOperation("上传菜品或套餐图片")
    @PostMapping(value="/upload",consumes="multipart/form-data")
    public Result<FileUploadVO> upload(@RequestParam("file") MultipartFile file) {return Result.success(storageService.upload(file));}
    @ApiOperation("刷新图片临时访问地址")
    @GetMapping("/file-url")
    public Result<FileUploadVO> fileUrl(@RequestParam String objectKey) {return Result.success(storageService.fileUrl(objectKey));}
}
