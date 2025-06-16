package com.github.paicoding.forum.core.util;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;

/**
 * @author yihui
 * @date 2022/7/6
 */
public class CrossUtil {
    /**
     * 支持跨域
     *
     * @param request
     * @param response
     */
    public static void buildCors(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");

        // 设置允许的HTTP方法
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
        // 设置预检请求的缓存时间
        response.setHeader("Access-Control-Max-Age", "3600");
        // 设置允许的请求头
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Origin, X-Requested-With, Content-Type, Accept, X-Real-IP, X-Forwarded-For, d-uuid, User-Agent, x-zd-cs, Proxy-Client-IP, HTTP_CLIENT_IP, HTTP_X_FORWARDED_FOR, x-access-token");

        if (StringUtils.isNotBlank(origin)) {
            // 如果 Origin 头存在，则将其反射回 Access-Control-Allow-Origin，并允许携带凭证
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        } else {
            // 如果 Origin 头不存在（例如：同源请求或非浏览器请求），
            // 允许所有来源但通常不涉及凭证，或者根据实际情况决定是否允许凭证。
            // 对于浏览器跨域请求，Origin头通常是存在的。
            // 在这里，我们将不允许凭证，以避免 Access-Control-Allow-Origin: * 与 allowCredentials: true 冲突
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Credentials", "false");
        }
    }
}
