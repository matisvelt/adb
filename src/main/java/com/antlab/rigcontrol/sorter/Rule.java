package com.antlab.rigcontrol.sorter;

public class Rule {
    private String id;
    private boolean enabled = true;
    private String condition;
    private String destination;
    private String notes;

    public Rule() {
    }

    public Rule(String id, boolean enabled, String condition, String destination, String notes) {
        this.id = id;
        this.enabled = enabled;
        this.condition = condition;
        this.destination = destination;
        this.notes = notes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
