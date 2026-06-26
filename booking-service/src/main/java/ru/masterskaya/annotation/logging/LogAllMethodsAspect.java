package ru.masterskaya.annotation.logging;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import ru.masterskaya.annotation.mask.Mask;
import ru.masterskaya.annotation.mask.MaskingRegistry;
import ru.masterskaya.config.MaskingProperties;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Аспект для логирования выполнения всех методов классов, помеченных аннотацией {@link LogAllMethods}.
 * <p>
 * Этот аспект обеспечивает сквозное логирование, включая:
 * <ul>
 *   <li>Запись входа в метод с аргументами</li>
 *   <li>Измерение времени выполнения</li>
 *   <li>Логирование результата или исключения</li>
 *   <li>Запись времени выполнения метода</li>
 * </ul>
 * <p>
 * <b>Формат логируемых сообщений:</b>
 * <pre>
 * [ClassName.methodName] Starting execution
 * Arguments: [arg1, arg2, ...]
 * Exiting method [ClassName.methodName] with result {result}
 * Execution time: {time} ms
 * Exception in method [ClassName.methodName] after {time} ms
 * </pre>
 * <p>
 * <b>Пример использования:</b>
 * <pre>{@code
 * // Конфигурация Spring
 * @Configuration
 * @EnableAspectJAutoProxy
 * public class AopConfig {
 *     @Bean
 *     public LogAllMethodsAspect logAllMethodsAspect() {
 *         return new LogAllMethodsAspect();
 *     }
 * }
 *
 * // Помеченный класс будет логироваться
 * @Service
 * @LogAllMethods
 * public class UserService {
 *     public User getUser(Long id) { ... }
 * }
 * }</pre>
 * <p>
 * <b>Особенности реализации:</b>
 * <ul>
 *   <li>Использует advice типа {@code @Around} для полного контроля над выполнением</li>
 *   <li>Логирует как успешные выполнения, так и исключения</li>
 *   <li>Измеряет время выполнения с точностью до миллисекунд</li>
 *   <li>Извлекает имя класса и метода через reflection</li>
 * </ul>
 *
 * @see LogAllMethods аннотация, которую обрабатывает этот аспект
 * @see Around advice, используемый для перехвата вызовов
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAllMethodsAspect {

    private final MaskingRegistry maskingRegistry;
    private final MaskingProperties maskingProperties;

    /**
     * Advice, перехватывающий выполнение методов в классах с аннотацией {@link LogAllMethods}.
     * <p>
     * Выполняет следующие действия для каждого перехваченного метода:
     * <ol>
     *   <li>Извлекает имя класса и метода</li>
     *   <li>Замеряет время начала выполнения</li>
     *   <li>Логирует вход в метод с аргументами</li>
     *   <li>Выполняет оригинальный метод</li>
     *   <li>При успехе - логирует результат и время выполнения</li>
     *   <li>При исключении - логирует ошибку и время до сбоя</li>
     * </ol>
     *
     * @param joinPoint точка соединения, содержащая информацию о вызываемом методе
     * @return результат выполнения оригинального метода
     * @throws Throwable если оригинальный метод выбрасывает исключение
     */
    @Around("@within(ru.masterskaya.annotation.logging.LogAllMethods)")
    public Object logWithMasking(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!maskingProperties.isEnabled()) {
            return joinPoint.proceed();
        }

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;

        long startTime = System.currentTimeMillis();

        // Логируем замаскированные аргументы
        if (log.isInfoEnabled()) {
            Map<String, Object> maskedArgs = maskMethodArgs(joinPoint);
            log.info("[{}] Args: {}", fullMethodName, maskedArgs);
        }

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            if (log.isInfoEnabled()) {
                log.info("[{}] Completed in {}ms, result: {}",
                        fullMethodName, executionTime,
                        result == null ? "void" : maskFieldsInObject(result, 0));
            }

            return result;
        } catch (Throwable throwable) {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.error("Exception in method [{}] after {} ms: {}", fullMethodName, executionTime, throwable.getMessage());
            throw throwable;
        }
    }

    /**
     * Маскирует аргументы метода с учетом аннотаций @Mask и конфигурации
     */
    private Map<String, Object> maskMethodArgs(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        Map<String, Object> maskedArgs = new LinkedHashMap<>();

        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            Object arg = args[i];

            // Проверяем аннотацию @Mask на параметре
            Mask maskAnnotation = parameters[i].getAnnotation(Mask.class);

            if (maskAnnotation != null) {
                maskedArgs.put(paramName, maskValue(arg, maskAnnotation.value()));
            } else if (isSensitiveByName(paramName)) {
                // Маскируем по имени из конфигурации
                maskedArgs.put(paramName, maskValue(arg, Mask.MaskType.FULL));
            } else {
                // Рекурсивно маскируем вложенные объекты
                maskedArgs.put(paramName, maskNestedObject(arg, 0));
            }
        }

        return maskedArgs;
    }

    /**
     * Рекурсивно обрабатывает вложенные объекты
     */
    private Object maskNestedObject(Object obj, int depth) {
        if (obj == null || depth > 3) { // Защита от бесконечной рекурсии
            return obj;
        }

        // Проверяем, является ли строка чувствительной по паттерну
        if (obj instanceof String strValue) {
            return maskByPatterns(strValue);
        }

        // Для DTO/сущностей маскируем поля
        if (isDomainObject(obj)) {
            return maskFieldsInObject(obj, depth + 1);
        }

        // Для коллекций обрабатываем элементы
        if (obj instanceof Collection<?> collection) {
            return collection.stream()
                    .map(item -> maskNestedObject(item, depth + 1))
                    .toList();
        }

        if (obj instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> maskNestedObject(e.getValue(), depth + 1),
                            (v1, v2) -> v2,
                            LinkedHashMap::new
                    ));
        }

        // Примитивы и простые типы оставляем как есть
        return obj;
    }

    private Object maskFieldsInObject(Object obj, int depth) {
        Map<String, Object> fieldMap = new LinkedHashMap<>();

        for (Field field : getAllFields(obj.getClass())) {
            field.setAccessible(true);
            try {
                String fieldName = field.getName();
                Object fieldValue = field.get(obj);

                // Проверяем аннотацию на поле
                Mask maskAnnotation = field.getAnnotation(Mask.class);

                if (maskAnnotation != null) {
                    fieldMap.put(fieldName, maskValue(fieldValue, maskAnnotation.value()));
                } else if (isSensitiveByName(fieldName)) {
                    fieldMap.put(fieldName, maskValue(fieldValue, Mask.MaskType.FULL));
                } else {
                    fieldMap.put(fieldName, maskNestedObject(fieldValue, depth));
                }
            } catch (Exception e) {
                fieldMap.put(field.getName(), "ERROR");
            }
        }

        return fieldMap;
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    private Object maskByPatterns(String value) {
        if (value == null) return null;

        // Проверяем по паттернам из конфигурации
        for (Map.Entry<String, String> pattern : maskingProperties.getPatterns().entrySet()) {
            if (value.matches(pattern.getValue())) {
                switch (pattern.getKey()) {
                    case "jwt":
                        return maskingRegistry.mask(value, Mask.MaskType.JWT);
                    case "email":
                        return maskingRegistry.mask(value, Mask.MaskType.EMAIL);
                }
            }
        }

        return value;
    }

    private boolean isSensitiveByName(String name) {
        if (name == null) return false;
        String lowerName = name.toLowerCase();
        return maskingProperties.getSensitiveFields().stream()
                .anyMatch(lowerName::contains);
    }

    private Object maskValue(Object value, Mask.MaskType type) {
        if (value == null) return null;
        if (value instanceof String strValue) {
            return maskingRegistry.mask(strValue, type);
        }
        return "******";
    }

    private boolean isDomainObject(Object obj) {
        if (obj == null) return false;
        String packageName = obj.getClass().getPackageName();
        return packageName.startsWith("ru.masterskaya");
    }

    @PostConstruct
    public void init() {
        log.info("LogAllMethodsAspect initialized");
    }
}