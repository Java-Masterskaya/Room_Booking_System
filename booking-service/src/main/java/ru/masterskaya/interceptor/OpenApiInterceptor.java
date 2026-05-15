package ru.masterskaya.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import ru.masterskaya.service.OpenApiValidatorService;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class OpenApiInterceptor implements HandlerInterceptor {

    private final OpenApiValidatorService validatorService;
    private final ObjectMapper objectMapper;

    public OpenApiInterceptor(OpenApiValidatorService validatorService, ObjectMapper objectMapper) {
        this.validatorService = validatorService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // Чтение тела
        String body = "";
        if (request instanceof ContentCachingRequestWrapper wrapperedRequest) {
            body = new String (wrapperedRequest.getContentAsByteArray(), StandardCharsets.UTF_8);
            System.out.println("+++++++++++++++++++++++");
            System.out.println(body);
        }

        // Сбор заголовков
        Map<String, String> headers = new HashMap<>();
        Collections.list(request.getHeaderNames()).forEach(hn -> headers.put(hn, request.getHeader(hn)));

        // Сбор query параметров
        Map<String, String> queryParams = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> queryParams.put(k, v[0])); // Берем первое значение из массива

        try {
            validatorService.validateRequest(
                    request.getMethod(),
                    request.getRequestURI(),
                    body,
                    headers,
                    queryParams
            );
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, String> errorResponse = Map.of(
                    "error", "Contract violation",
                    "details", exception.getMessage()
            );

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return false;
        }

        return true;
    }
}

