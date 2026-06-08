package ru.masterskaya.openApiDemoBooking;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OpenApiDemoBookingRequest {
    private Integer roomId;
    private Integer userId;
    private String startTime;
    private String endTime;
}
