package ru.masterskaya.config;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.springmvc.DefaultValidationReportHandler;
import com.atlassian.oai.validator.springmvc.OpenApiValidationInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.masterskaya.filter.CustomOpenApiValidationFilter;




@Configuration
public class OpenApiValidationConfig implements WebMvcConfigurer {

    @Value("${app.openapi.spec-path}")
    private String specPath;

    @Value("${app.openapi.max-body-size}")
    private String maxBodySize;

    @Bean
    public OpenApiValidationInterceptor openApiValidationInterceptor() {
        OpenApiInteractionValidator validator = OpenApiInteractionValidator
                .createFor(specPath)
                .build();

        return new OpenApiValidationInterceptor(validator, new DefaultValidationReportHandler());
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
        config.addAllowedOrigin("http://localhost:3000");
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