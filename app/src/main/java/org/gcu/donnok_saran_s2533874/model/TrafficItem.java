package org.gcu.donnok_saran_s2533874.model;

public class TrafficItem {
    private String title;
    private String description;
    private String publishedDate;
    private double toLatitude;
    private double toLongitude;
    private double fromLatitude;
    private double fromLongitude;

    public TrafficItem (String title, String description, String publishedDate, double toLatitude, double toLongitude, double fromLatitude, double fromLongitude) {
        this.title = title;
        this.description = description;
        this.publishedDate = publishedDate;
        this.toLatitude = toLatitude;
        this.toLongitude = toLongitude;
        this.fromLatitude = fromLatitude;
        this.fromLongitude = fromLongitude;
}

public String getTitle() {return title;}
public String getDescription() {return description;}
public String getPublishedDate() {return publishedDate;}
public double getToLatitude() {return toLatitude;}
public double getToLongitude() {return toLongitude;}
public double getFromLatitude() {return fromLatitude;}
public double getFromLongitude() {return fromLongitude;}

}
