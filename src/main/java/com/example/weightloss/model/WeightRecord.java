package com.example.weightloss.model;

/** 单条体重记录：体重、日期、备注与心情。 */
public class WeightRecord {

    public static final int MOOD_RELAXED = 0;
    public static final int MOOD_NORMAL = 1;
    public static final int MOOD_TIRED = 2;

    private long id;
    private double weight;
    private long date;
    private String note;
    private int mood = MOOD_NORMAL;

    public WeightRecord() {
    }

    public WeightRecord(double weight, long date, String note) {
        this(weight, date, note, MOOD_NORMAL);
    }

    public WeightRecord(double weight, long date, String note, int mood) {
        this.weight = weight;
        this.date = date;
        this.note = note;
        this.mood = mood;
    }

    public WeightRecord(long id, double weight, long date, String note) {
        this(id, weight, date, note, MOOD_NORMAL);
    }

    public WeightRecord(long id, double weight, long date, String note, int mood) {
        this.id = id;
        this.weight = weight;
        this.date = date;
        this.note = note;
        this.mood = mood;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getMood() {
        return mood;
    }

    public void setMood(int mood) {
        this.mood = mood;
    }
}
