package com.example.joshuatrabkap1;

import java.util.Date;

public class WeightEntry {

    private long id;
    private long userId;
    private Date date;
    private double weight;
    private String notes;

    // Constructors
    public WeightEntry() {
        // Default constructor used for loading data or initializing before setting fields
    }


    public WeightEntry(long userId, double weight, Date date) {
        this.userId = userId;
        this.weight = weight;
        this.date = date;
        this.notes = null;
    }

    public WeightEntry(long id, long userId, double weight, Date date, String notes) {
        this.id = id;
        this.userId = userId;
        this.weight = weight;
        this.date = date;
        this.notes = notes;
    }

    // Getters and Setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "WeightEntry{" +
                "id=" + id +
                ", userId=" + userId +
                ", date=" + (date != null ? date.toString() : "null") +
                ", weight=" + weight +
                '}';
    }
}
