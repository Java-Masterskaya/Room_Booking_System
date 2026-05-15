package ru.masterskaya.booking;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BookingRequest {
    private Integer roomId;
    private Integer userId;
    private String startTime;
    private String endTime;
}
