package com.sky.mapper;
import com.sky.vo.DashboardOverviewVO;
import org.apache.ibatis.annotations.Select;
public interface DashboardMapper {
    @Select("SELECT (SELECT COUNT(*) FROM employee) AS employee_count, " +
            "(SELECT COUNT(*) FROM category) AS category_count, " +
            "(SELECT COUNT(*) FROM dish) AS dish_count, " +
            "(SELECT COUNT(*) FROM orders) AS order_count")
    DashboardOverviewVO overview();
    @Select("SELECT 1") int ping();
}
