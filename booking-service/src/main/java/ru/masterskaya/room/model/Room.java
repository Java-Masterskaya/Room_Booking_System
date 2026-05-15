package ru.masterskaya.room.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Room {
    private Integer id;
    private String name;
    private Integer capacity;
    private List<String> equipment;
}
