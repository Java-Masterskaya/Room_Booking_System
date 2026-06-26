package ru.masterskaya.dto.auth;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.masterskaya.annotation.mask.Mask;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthResponse {
    @Mask(Mask.MaskType.JWT)
    String token;

    @Mask(Mask.MaskType.EMAIL)
    String email;
    String role;
}
