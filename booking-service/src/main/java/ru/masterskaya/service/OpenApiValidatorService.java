package ru.masterskaya.service;

import jakarta.annotation.PostConstruct;
import org.openapi4j.operation.validator.model.Request;
import org.openapi4j.operation.validator.model.impl.Body;
import org.openapi4j.operation.validator.model.impl.DefaultRequest;
import org.openapi4j.operation.validator.validation.RequestValidator;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Map;

@Service
public class OpenApiValidatorService {

    private RequestValidator requestValidator;

    @Value("${app.openapi.spec-path}")
    private String specPath;

    @PostConstruct
    public void init() throws Exception {
        URL specUrl = getClass().getClassLoader().getResource(specPath);
        if (specUrl == null) {
            throw new RuntimeException("Файл контракта не найден: " + specPath);
        }
        OpenApi3 openApi = new OpenApi3Parser().parse(specUrl, false);
        this.requestValidator = new RequestValidator(openApi);
    }

    public void validateRequest(String method, String path, String body,
                                Map<String, String> headers,
                                Map<String, String> queryParams) throws Exception {
        System.out.println("///////////////////////////");
        System.out.println("method" + method);
        System.out.println("path" + path);
        System.out.println("body" + body);
        System.out.println("headers" + headers);
        System.out.println("queryParams" + queryParams);
        Request.Method httpMethod = Request.Method.valueOf(method.toUpperCase());
        DefaultRequest.Builder builder = new DefaultRequest.Builder(path, httpMethod);

        if (body != null && !body.isEmpty()) {
            builder.body(Body.from(body));
        }

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        if (queryParams != null && !queryParams.isEmpty()) {
            StringBuilder queryString = new StringBuilder();
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (queryString.length() > 0) queryString.append("&");
                queryString.append(entry.getKey()).append("=").append(entry.getValue());
            }
            builder.query(queryString.toString());
        }

        // Валидация
        try {
            requestValidator.validate(builder.build());
        } catch (ValidationException exception) {
            // Дерево ошибок превращаем в строку
            throw new Exception("Contract violation: " + exception.results().toString());
        }
    }
}