package com.sky.controller.admin;
import com.sky.mapper.DashboardMapper;
import com.sky.result.Result;
import com.sky.vo.DashboardOverviewVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@Api(tags = "工作台")
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardMapper dashboardMapper;
    @ApiOperation("业务数据概览")
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview() { return Result.success(dashboardMapper.overview()); }
}
