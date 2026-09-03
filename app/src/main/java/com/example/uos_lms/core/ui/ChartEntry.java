package com.example.uos_lms.core.ui;

public final class ChartEntry {
    private final String label;
    private final int value;

    public ChartEntry(String label, int value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public int getValue() {
        return value;
    }
}
