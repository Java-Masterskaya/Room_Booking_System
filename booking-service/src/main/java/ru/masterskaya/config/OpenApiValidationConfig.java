package ru.masterskaya.config;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.springmvc.DefaultValidationReportHandler;
import com.atlassian.oai.validator.springmvc.OpenApiValidationInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.masterskaya.exceptions.CustomInvalidRequestException;
import ru.masterskaya.filter.CustomOpenApiValidationFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Configuration
public class OpenApiValidationConfig implements WebMvcConfigurer {

    @Value("${app.openapi.spec-path}")
    private String specPath;

    @Value("${app.openapi.max-body-size}")
    private String maxBodySize;

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Bean
    public OpenApiValidationInterceptor openApiValidationInterceptor() {
        OpenApiInteractionValidator validator = OpenApiInteractionValidator
                .createFor(specPath)
                .build();

        return new OpenApiValidationInterceptor(validator, new SafeValidationReportHandler());
    }

    public static class SafeValidationReportHandler extends DefaultValidationReportHandler {
        private static final Logger log = LoggerFactory.getLogger(SafeValidationReportHandler.class);

        // ВЫНОСИМ РЕГУЛЯРКУ СЮДА: Компилируется ровно 1 раз при старте приложения
        private static final Pattern FIELD_PATTERN = Pattern.compile(
                "(?i)(?:password|secret|token|cookie)['\"]?\\s*(?:with value|is|=)\\s*['\"]?([^'\"\\s,]+)['\"]?"
        );

        @Override
        public void handleRequestReport(String messagePrefix, ValidationReport report) {
            if (report.hasErrors()) {
                List<String> maskedErrorsForClient = new ArrayList<>();

                for (ValidationReport.Message message : report.getMessages()) {
                    String originalMessage = message.getMessage();
                    String maskedMessage = originalMessage;

                    // ШАГ 1: Защита токенов авторизации (Bearer / Basic)
                    if (maskedMessage.toLowerCase().contains("bearer ")) {
                        maskedMessage = maskedMessage.replaceAll("(?i)bearer\\s+[^'\"\\s,]+", "Bearer *******");
                    }
                    if (maskedMessage.toLowerCase().contains("basic ")) {
                        maskedMessage = maskedMessage.replaceAll("(?i)basic\\s+[^'\"\\s,]+", "Basic *******");
                    }

                    // ШАГ 2: Защита JSON-полей в теле запроса (password, secret, token, cookie)
                    Matcher matcher = FIELD_PATTERN.matcher(maskedMessage);
                    if (matcher.find()) {

                        String sensitiveValue = matcher.group(1);
                        maskedMessage = maskedMessage.replace(sensitiveValue, "*******");
                    }

                    // Логируем на сервере БЕЗОПАСНЫЙ текст
                    if (message.getLevel() == ValidationReport.Level.ERROR) {
                        log.error("{}: {}", messagePrefix, maskedMessage);
                    } else {
                        log.warn("{}: {}", messagePrefix, maskedMessage);
                    }

                    // Сохраняем безопасный текст для передачи в DTO ответа
                    maskedErrorsForClient.add(maskedMessage);
                }

                throw new CustomInvalidRequestException(report, maskedErrorsForClient);
            }
        }
    }


    @Bean
    public FilterRegistrationBean<CustomOpenApiValidationFilter> openApiValidationFilter() {
        FilterRegistrationBean<CustomOpenApiValidationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new CustomOpenApiValidationFilter(maxBodySize));
        registrationBean.addUrlPatterns("/api/*");              // Кэшируем все запросы
        registrationBean.setOrder(Integer.MIN_VALUE + 1);       // Приоритет после CORS
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin(allowedOrigin);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/api/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Integer.MIN_VALUE); // Самый высокий приоритет
        return bean;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(openApiValidationInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/actuator/**"
                );
    }
}