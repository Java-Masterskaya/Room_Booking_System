import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import ru.masterskaya.config.OpenApiValidationConfig;
import ru.masterskaya.exceptions.CustomInvalidRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(OutputCaptureExtension.class)
class OpenApiTokenMaskingUnitTest {

    @Test
    void shouldMaskSensitiveDataInValidationLog(CapturedOutput output) {
        // 1. Инициализируем обработчик отчетов из конфигурации
        OpenApiValidationConfig.SafeValidationReportHandler handler = new OpenApiValidationConfig.SafeValidationReportHandler();

        String secretToken = "my-super-secret-token-12345";
        String rawValidationMessage = "String \"Bearer " + secretToken + "\" is too short (length: 37, required minimum: 50)";

        // 2. Создаем сообщение об ошибке
        ValidationReport.Message message = ValidationReport.Message.create(
                "validation.request.header.schema",
                rawValidationMessage
        ).build();

        // 3. Собираем отчет
        ValidationReport report = ValidationReport.from(message);

        // 4. Проверяем, что хэндлер блокирует запрос и выбрасывает CustomInvalidRequestException
        CustomInvalidRequestException thrownException = assertThrows(
                CustomInvalidRequestException.class,
                () -> handler.handleRequestReport("Validation failed", report)
        );

        // 5. Проверяем, что внутри самого исключения данные ЗАМАСКИРОВАНЫ
        String exceptionMessage = thrownException.getMessage();
        assertThat(exceptionMessage).doesNotContain(secretToken);
        assertThat(exceptionMessage).contains("*******");

        // 6. Считываем логи консоли, которые записал log.error() внутри хэндлера
        String logOutput = output.getAll();

        // Главные проверки безопасности логов сервера:
        assertThat(logOutput).contains("Validation failed");
        assertThat(logOutput).doesNotContain(secretToken);
        assertThat(logOutput).contains("*******");
    }
}

