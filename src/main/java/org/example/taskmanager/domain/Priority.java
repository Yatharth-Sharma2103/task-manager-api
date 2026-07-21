package org.example.taskmanager.domain;

/**
 * Task priority levels. Ordered from lowest to highest urgency so that the
 * ordinal value can be used for sorting and by the AI prioritization engine.
 */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}
