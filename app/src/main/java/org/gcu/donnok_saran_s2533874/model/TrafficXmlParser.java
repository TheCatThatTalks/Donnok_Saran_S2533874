package org.gcu.donnok_saran_s2533874.model;

import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

//Pull parser for reading DATEX II XML format from Traffic Scotland
public class TrafficXmlParser {

    public static List<TrafficItem> parse(String xmlData) {
        List<TrafficItem> items = new ArrayList<>();
        if (xmlData == null || xmlData.isEmpty()) return items;

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(xmlData));

            //Track where we are in XML tree
            boolean insideSituationRecord = false;
            boolean insideTo = false;
            boolean insideFrom = false;
            boolean insideComment = false;
            boolean insideDescriptor = false;

            String recordType = "";
            String title = "";
            String description = "";
            String startTime = "";
            String endTime = "";
            String publishedDate = "";
            String roadName = "";
            String delaysType = "";
            double toLat = 0.0, toLon = 0.0, fromLat = 0.0, fromLon = 0.0;

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = xpp.getName();

                if (eventType == XmlPullParser.START_TAG) {
                    if ("situationRecord".equalsIgnoreCase(tagName)) {
                        insideSituationRecord = true;

                        //Reset variables for each record
                        recordType = "";
                        title = "";
                        description = "";
                        publishedDate = "";
                        startTime = "";
                        endTime = "";
                        roadName = "";
                        delaysType = "";
                        toLat = 0.0; toLon = 0.0; fromLat = 0.0; fromLon = 0.0;

                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                            if ("type".equalsIgnoreCase(xpp.getAttributeName(i))) {
                                recordType = xpp.getAttributeValue(i);
                                break;
                            }
                        }
                    } else if (insideSituationRecord) {
                        if ("to".equalsIgnoreCase(tagName)) { insideTo = true; insideFrom = false; }
                        else if ("from".equalsIgnoreCase(tagName)) { insideFrom = true; insideTo = false; }
                        else if ("generalPublicComment".equalsIgnoreCase(tagName)) { insideComment = true; }
                        else if ("descriptor".equalsIgnoreCase(tagName)) { insideDescriptor = true; }
                        else if ("delaysType".equalsIgnoreCase(tagName)) { delaysType = xpp.nextText().trim(); }
                        else if ("value".equalsIgnoreCase(tagName)) {
                            String valText = xpp.nextText().trim();
                            if (insideComment && description.isEmpty()) {
                                description = valText;
                            } else if (insideDescriptor && roadName.isEmpty()) {
                                if (valText.matches("^[AM]\\d+.*")) {
                                    roadName = valText.split(" ")[0];
                                }
                            }
                        } else if ("situationRecordCreationTime".equalsIgnoreCase(tagName) || "publicationTime".equalsIgnoreCase(tagName)) {
                            publishedDate = xpp.nextText().trim();
                        } else if ("overallStartTime".equalsIgnoreCase(tagName)) {
                            startTime = xpp.nextText().trim();
                        } else if ("overallEndTime".equalsIgnoreCase(tagName)) {
                            endTime = xpp.nextText().trim();
                        } else if ("latitude".equalsIgnoreCase(tagName)) {
                            double val = parseDoubleSafely(xpp.nextText().trim());
                            if (insideTo) toLat = val;
                            else if (insideFrom) fromLat = val;
                            else { if (toLat == 0.0) toLat = val; }
                        } else if ("longitude".equalsIgnoreCase(tagName)) {
                            double val = parseDoubleSafely(xpp.nextText().trim());
                            if (insideTo) toLon = val;
                            else if (insideFrom) fromLon = val;
                            else { if (toLon == 0.0) toLon = val; }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if ("to".equalsIgnoreCase(tagName)) insideTo = false;
                    else if ("from".equalsIgnoreCase(tagName)) insideFrom = false;
                    else if ("generalPublicComment".equalsIgnoreCase(tagName)) insideComment = false;
                    else if ("descriptor".equalsIgnoreCase(tagName)) insideDescriptor = false;
                    else if ("situationRecord".equalsIgnoreCase(tagName) && insideSituationRecord) {
                        if (!description.isEmpty() || toLat != 0.0 || fromLat != 0.0) {
                            title = buildTitle(description, roadName);
                            if (roadName.isEmpty()) roadName = extractRoadFromText(description);

                            // Copy coordinates across if only one coordinate pair was provided
                            if (toLat == 0.0 && fromLat != 0.0) toLat = fromLat;
                            if (toLon == 0.0 && fromLon != 0.0) toLon = fromLon;
                            if (fromLat == 0.0 && toLat != 0.0) fromLat = toLat;
                            if (fromLon == 0.0 && toLon != 0.0) fromLon = toLon;

                            TrafficItem item = new TrafficItem(
                                    recordType, title, description, publishedDate,
                                    startTime, endTime, roadName, delaysType,
                                    toLat, toLon, fromLat, fromLon
                            );
                            items.add(item);
                        }
                        insideSituationRecord = false;
                    }
                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            Log.e("TrafficXmlParser", "Error parsing DATEX II XML", e);
        }
        return items;
    }

    private static double parseDoubleSafely(String str) {
        try { return Double.parseDouble(str); } catch (Exception e) { return 0.0; }
    }

    private static String buildTitle(String desc, String road) {
        if (desc == null || desc.isEmpty()) return "Traffic Event";
        int idx = desc.indexOf('-');
        if (idx > 0 && idx < 50) return desc.substring(0, idx).trim();
        return desc.length() > 50 ? desc.substring(0, 50).trim() + "..." : desc;
    }

    private static String extractRoadFromText(String text) {
        if (text == null) return "";
        for (String word : text.split("\\s+")) {
            if (word.matches("^[AM]\\d+[A-Za-z]?$") || word.matches("^A\\d+$") || word.matches("^M\\d+$")) {
                return word;
            }
        }
        return "";
    }
}