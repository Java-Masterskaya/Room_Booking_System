package ru.masterskaya.config; // Укажите ваш пакет

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.masterskaya.interceptor.OpenApiInterceptor; // Укажите правильный путь к интерцептору

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final OpenApiInterceptor openApiInterceptor;

    // Spring сам внедрит сюда ваш интерцептор
    public WebConfig(OpenApiInterceptor openApiInterceptor) {
        this.openApiInterceptor = openApiInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Регистрируем интерцептор для всех путей
        registry.addInterceptor(openApiInterceptor).addPathPatterns("/**");
    }
}
