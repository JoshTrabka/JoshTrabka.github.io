package com.example.joshuatrabkap1;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
public class WeightPredictor {
    /**
     * Enhancement Plan Data Structures and Algorithms:
     * Logic to predict when the user will reach their goal weight using their current entries
     * to calculate the slope, then using the goal weight, recent weight, and slope to output a prediction date
     *
     */
    public static class PredictionResult {
        public final long targetDateTimestamp;
        public final String statusMessage;
        public final boolean isSuccess;

        public PredictionResult(long targetDateTimestamp, String statusMessage, boolean isSuccess) {
            this.targetDateTimestamp = targetDateTimestamp;
            this.statusMessage = statusMessage;
            this.isSuccess = isSuccess;
        }
    }

    /**
     * Calculates the ordinary least squares linear regression based on historical weight data.
     * Maps time (X in days elapsed) against weight (Y in lbs).
     */
    public static PredictionResult predictGoalDate(List<WeightEntry> entries, double goalWeight) {
        //  Guard Clause: Checks that there are at least 5 entries before outputting a prediction
        if (entries == null || entries.size() < 5) {
            return new PredictionResult(0, "Insufficient Data (Minimum 5 entries required)", false);
        }

        //  Sort entries chronologically to ensure time arrays are sequential
        Collections.sort(entries, Comparator.comparing(WeightEntry::getDate));

        int n = entries.size();
        long firstEntryTime = entries.get(0).getDate().getTime();
        double msPerDay = 1000.0 * 60 * 60 * 24;

        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;

        // Process elements into independent day offsets (X) and dependent weights (Y)
        for (WeightEntry entry : entries) {
            // Days elapsed since baseline entry 0
            double daysElapsed = (entry.getDate().getTime() - firstEntryTime) / msPerDay;
            double weight = entry.getWeight();

            sumX += daysElapsed;
            sumY += weight;
            sumXY += daysElapsed * weight;
            sumXX += daysElapsed * daysElapsed;
        }

        //  Calculate standard linear regression slope (m) and intercept (b) formulas
        double denominator = (n * sumXX) - (sumX * sumX);

        // Safety check to handle a flat line or edge case where all data points share the exact same timestamp
        if (Math.abs(denominator) < 0.0001) {
            return new PredictionResult(0, "Stable progression: More variance required to project trend", false);
        }

        double slope = ((n * sumXY) - (sumX * sumY)) / denominator;
        double intercept = (sumY - (slope * sumX)) / n;

        //  Explicitly find the newest weight entry by date to prevent sorting bugs
        WeightEntry newestEntry = entries.get(0);
        for (WeightEntry entry : entries) {
            if (entry.getDate().getTime() > newestEntry.getDate().getTime()) {
                newestEntry = entry;
            }
        }
        double currentWeight = newestEntry.getWeight();

        // Safe Directional Guard Checks
        if (currentWeight > goalWeight && slope >= 0) {
            return new PredictionResult(0, "Weight is increasing or flat (" + String.format(Locale.US, "%.2f", slope) + " lbs/day); cannot reach a lower goal.", false);
        }
        if (currentWeight < goalWeight && slope <= 0) {
            return new PredictionResult(0, "Weight is decreasing or flat (" + String.format(Locale.US, "%.2f", slope) + " lbs/day); cannot reach a higher goal.", false);
        }

        //  Calculate target day offset where y = goalWeight -> x = (y - b) / m
        double targetDayOffset = (goalWeight - intercept) / slope;

        // Map back to a real UNIX timestamp
        long targetTimestamp = firstEntryTime + (long) (targetDayOffset * msPerDay);

        return new PredictionResult(targetTimestamp, "Success", true);
    }
}
