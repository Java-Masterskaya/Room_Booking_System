package ru.masterskaya.model;

public enum OutboxStatus {
    NEW,
    PROCESSING,
    PROCESSED,
    FAILED
}
