package ru.masterskaya.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthRequest {
    @NotBlank(message = "Поле email обязательное")
    @Email(message = "Email должен быть корректный")
    String email;

    @NotBlank(message = "Поле password обязательное")
    @Size(min = 6, message = "Пароль должен состоять минимум из 6 символов")
    String password;
}
