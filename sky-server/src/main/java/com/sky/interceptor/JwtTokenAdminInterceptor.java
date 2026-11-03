package com.sky.interceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.mapper.EmployeeMapper;
import com.sky.entity.Employee;
import com.sky.constant.StatusConstant;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
@Component
@RequiredArgsConstructor
public class JwtTokenAdminInterceptor implements HandlerInterceptor {
    private final JwtProperties jwtProperties;
    private final EmployeeMapper employeeMapper;
    private final ObjectMapper objectMapper;
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        BaseContext.removeCurrentId();
        if ("OPTIONS".equals(request.getMethod())) return true;
        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(),
                    request.getHeader(jwtProperties.getAdminTokenName()));
            Long employeeId = Long.valueOf(String.valueOf(claims.get(JwtClaimsConstant.EMP_ID)));
            Employee employee = employeeMapper.getById(employeeId);
            if (employee == null || !StatusConstant.ENABLE.equals(employee.getStatus())) {
                return reject(response);
            }
            BaseContext.setCurrentId(employeeId);
            return true;
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            return reject(response);
        }
    }
    private boolean reject(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), Result.error(MessageConstant.USER_NOT_LOGIN));
        return false;
    }
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.removeCurrentId();
    }
}
