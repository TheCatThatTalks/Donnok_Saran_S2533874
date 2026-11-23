package org.gcu.donnok_saran_s2533874.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TrafficItem implements java.io.Serializable {

    private String recordType; // e.g., MaintenanceWorks, GeneralObstruction, RoadOrCarriagewayOrLaneManagement
    private String title;
    private String description;
    private String publishedDate;
    private String startTime;
    private String endTime;
    private String roadName; // e.g., M8, A82, M74
    private String delaysType; // e.g. longDelays
    private double toLatitude;
    private double toLongitude;
    private double fromLatitude;
    private double fromLongitude;
    private TrafficItem selectedItem;


    // Data model for single traffic incident or roadwork item.
    // Serializable so we can easily pass it between screens with Intent extras.
    public TrafficItem(String recordType, String title, String description, String publishedDate,
                       String startTime, String endTime, String roadName, String delaysType,
                       double toLatitude, double toLongitude, double fromLatitude, double fromLongitude) {
        this.recordType = recordType;
        this.title = title;
        this.description = description;
        this.publishedDate = publishedDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.roadName = roadName;
        this.delaysType = delaysType;
        this.toLatitude = toLatitude;
        this.toLongitude = toLongitude;
        this.fromLatitude = fromLatitude;
        this.fromLongitude = fromLongitude;
    }

    // Getters with null fallback
    public String getRecordType() { return recordType != null ? recordType : ""; }
    public String getTitle() { return title != null ? title : ""; }
    public String getDescription() { return description != null ? description : ""; }
    public String getPublishedDate() { return publishedDate != null ? publishedDate : ""; }
    public String getStartTime() { return startTime != null ? startTime : ""; }
    public String getEndTime() { return endTime != null ? endTime : ""; }
    public String getRoadName() { return roadName != null ? roadName : ""; }
    public String getDelaysType() { return delaysType != null ? delaysType : ""; }
    public double getToLatitude() { return toLatitude; }
    public double getToLongitude() { return toLongitude; }
    public double getFromLatitude() { return fromLatitude; }
    public double getFromLongitude() { return fromLongitude; }

    //Check if this item is roadworks
    public boolean isMaintenanceWork() {
        return recordType != null && recordType.toLowerCase().contains("maintenanceworks");
    }

    //Get user-friendly category title
    public String getCategory() {
        return isMaintenanceWork() ? "Current Roadworks" : "Unplanned Event";
    }

    //Work out how many days roadworks will last
    public long getDurationDays() {
        if (startTime == null || startTime.isEmpty() || endTime == null || endTime.isEmpty()) {
            return -1;
        }
        try {
            //Clean up ISO timezone format
            String cleanStart = startTime.replaceAll("([+-]\\d{2}):(\\d{2})$", "$1$2");
            String cleanEnd = endTime.replaceAll("([+-]\\d{2}):(\\d{2})$", "$1$2");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.UK);
            Date startDate = sdf.parse(cleanStart);
            Date endDate = sdf.parse(cleanEnd);

            if (startDate != null && endDate != null) {
                long diffInMillis = endDate.getTime() - startDate.getTime();
                return diffInMillis / (1000 * 60 * 60 * 24);
            }
        } catch (Exception e) {
            // Backup parsing for simple yyyy-MM-dd dates
            try {
                String simpleStart = startTime.substring(0, 10);
                String simpleEnd = endTime.substring(0, 10);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
                Date sDate = sdf.parse(simpleStart);
                Date eDate = sdf.parse(simpleEnd);
                if (sDate != null && eDate != null) {
                    return (eDate.getTime() - sDate.getTime()) / (1000 * 60 * 60 * 24);
                }
            } catch (Exception ignored) {}
        }
        return -1;
    }

    //Get human-readable severity label
    public String getDelaySeverity() {
        if ("longDelays".equalsIgnoreCase(delaysType)) {
            return "Long Delay";
        }
        long days = getDurationDays();
        if (days < 0) {
            return isMaintenanceWork() ? "Ongoing Work" : "Unplanned Hazard";
        } else if (days <= 3) {
            return "Short Delay (" + days + " days)";
        } else if (days <= 7) {
            return "Medium Delay (" + days + " days)";
        } else {
            return "Long Delay (" + days + " days)";
        }
    }

    // Return badge color matching delay level
    public String getDelayColorHex() {
        if ("longDelays".equalsIgnoreCase(delaysType)) {
            return "#C62828"; // Red
        }
        long days = getDurationDays();
        if (days < 0) {
            return isMaintenanceWork() ? "#EF6C00" : "#C62828";
        } else if (days <= 3) {
            return "#2E7D32"; // Green
        } else if (days <= 7) {
            return "#EF6C00"; // Orange
        } else {
            return "#C62828"; // Red
        }
    }

    // Calculate distance from user's GPS position in km using Haversine formula
    public double calculateDistanceKm(double userLat, double userLon) {
        double lat = toLatitude != 0.0 ? toLatitude : fromLatitude;
        double lon = toLongitude != 0.0 ? toLongitude : fromLongitude;
        if (lat == 0.0 && lon == 0.0) return 9999.0;

        double earthRadius = 6371; // km
        double dLat = Math.toRadians(lat - userLat);
        double dLon = Math.toRadians(lon - userLon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(lat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    // Make raw date string look nice (e.g. 22 Jul 2023, 14:30)
    public static String formatDate(String isoDateStr) {
        if (isoDateStr == null || isoDateStr.trim().isEmpty()) {
            return "";
        }
        try {
            String cleanDate = isoDateStr.replaceAll("([+-]\\d{2}):(\\d{2})$", "$1$2");
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.UK);
            Date date = isoFormat.parse(cleanDate);
            if (date != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.UK);
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            try {
                if (isoDateStr.length() >= 10) {
                    String ymd = isoDateStr.substring(0, 10);
                    SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
                    Date date = simpleFormat.parse(ymd);
                    if (date != null) {
                        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
                        return outputFormat.format(date);
                    }
                }
            } catch (Exception ignored) {}
        }
        return isoDateStr;
    }

    public String getFormattedStartTime() { return formatDate(startTime); }
    public String getFormattedEndTime() { return formatDate(endTime); }
    public String getFormattedPublishedDate() { return formatDate(publishedDate); }

    // Format full details for detail pane or popup dialog
    public String getFormattedDetails(int type) {
        StringBuilder sb = new StringBuilder();
        sb.append("Category: ").append(getCategory()).append("\n");
        if (!roadName.isEmpty()) {
            sb.append("Road: ").append(roadName).append("\n");
        }
        sb.append("Severity: ").append(getDelaySeverity()).append("\n");
        if (!getFormattedStartTime().isEmpty()) {
            sb.append("Start Time: ").append(getFormattedStartTime()).append("\n");
        }
        if (!getFormattedEndTime().isEmpty()) {
            sb.append("End Time: ").append(getFormattedEndTime()).append("\n");
        }
        if (!getFormattedPublishedDate().isEmpty()) {
            sb.append("Published: ").append(getFormattedPublishedDate()).append("\n");
        }
        sb.append("\nDescription:\n").append(getDescription());
        return sb.toString();
    }
}