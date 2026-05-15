package ru.masterskaya.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

@Component
public class RequestCachingFilter implements Filter {


    @Value("${app.openapi.cache-limit}")
    private DataSize cacheLimit;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpServletRequest) {

            int maxCacheSizeByte = (int) cacheLimit.toBytes();

            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(
                    httpServletRequest,
                    maxCacheSizeByte
            );

            // Вычитываем поток из InputStream
            wrappedRequest.getInputStream().readAllBytes();
            chain.doFilter(wrappedRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}
