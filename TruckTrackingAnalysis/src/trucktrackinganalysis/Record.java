/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trucktrackinganalysis;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author JAY
 */
public class Record {

    private String trip_id;
    private Date date;      // year = starts from 1900 (1900+year), month (0-11), date(1-31)
    private String macid;
    private String name;
    private String startlocation;
    private Time sTime;
    private Time gTime;
    private String endlocation;
    private Time eTime;
    private Time waitTime;
    private Time timespent;
    private String description;
    private String distance;
    private boolean error;

    public Record(String trip_id, String macid, String startlocation, String sTime, String gTime,
            String endlocation, String eTime, String description, ArrayList<Point> distanceList, boolean error) {
        setDateAndTripId(trip_id.trim());
        setNameAndMacid(macid.trim());
        this.startlocation = startlocation.trim();
        setStartTime(sTime.trim());
        setGateInTime(gTime.trim());
        this.endlocation = endlocation.trim();
        setEndTime(eTime.trim());
        setTimeSpent(null);       // null -> set timespent based on stime and etime
        this.description = description.trim();
        calculateWaitTime();
        calculateDistance(distanceList);
        this.error = error;
    }

    public Record(String trip_id, String macid, String startlocation, String sTime, String gTime,
            String endlocation, String eTime, String description, String distance, boolean error) {
        setDateAndTripId(trip_id.trim());
        setNameAndMacid(macid.trim());
        this.startlocation = startlocation.trim();
        setStartTime(sTime.trim());
        setGateInTime(gTime.trim());
        this.endlocation = endlocation.trim();
        setEndTime(eTime.trim());
        setTimeSpent(null);       // null -> set timespent based on stime and etime
        this.description = description.trim();
        calculateWaitTime();
        this.distance = distance == null ? "0.00" : distance;
        this.error = error;
    }

    /**
     *
     * @return queue time as string
     */
    public String getWaitTimeAsString() {
        int h = waitTime.getHours();
        int m = waitTime.getMinutes();
        return (h == 0 && m == 0) ? "" : ("" + ((h < 10) ? "0" + h : h) + ":" + ((m < 10) ? "0" + m : m));
    }

    private void calculateWaitTime() {
        int minutes = 0;
        int hours = 0;
        if (startlocation.equalsIgnoreCase(endlocation)) {
            if (containsIgnoreCase(description, "gate in")) { // set time from stime and gtime
                if (gTime.getHours() < sTime.getHours()) {
                    minutes = (int) (((gTime.getTime() + (24 * 60 * 60 * 1000)) - sTime.getTime()) / 1000) / 60;
                } else {
                    minutes = (int) ((gTime.getTime() - sTime.getTime()) / 1000) / 60;
                }
                hours = minutes / 60;
                if (hours > 0) {
                    minutes = minutes - (hours * 60);
                }
            }
        }
        waitTime = new Time(hours, minutes, 00);
    }

    /**
     * checks src if sub string is present in it
     *
     * @param src
     * @param sub
     * @return true if src contains sub, else false
     */
    public static boolean containsIgnoreCase(String src, String sub) {
        final int length = sub.length();
        if (length == 0) {
            return true; // Empty string is contained
        }
        final char firstLo = Character.toLowerCase(sub.charAt(0));
        final char firstUp = Character.toUpperCase(sub.charAt(0));

        for (int i = src.length() - length; i >= 0; i--) {
            // Quick check before calling the more expensive regionMatches() method:
            final char ch = src.charAt(i);
            if (ch != firstLo && ch != firstUp) {
                continue;
            }
            if (src.regionMatches(true, i, sub, 0, length)) {
                return true;
            }
        }
        return false;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean er) {
        error = er;
    }

    public String getName() {
        return name;
    }

    public String getTrip_id() {
        return trip_id;
    }

    public String getDate() {
        return ((date.getMonth() + 1) + "-" + (date.getDate()) + "-" + (date.getYear()));
    }

    /**
     * set date and trip id based in dt
     *
     * @param dt dt can be tripid or date
     */
    public final void setDateAndTripId(String dt) {
        int y, m, d;
        if (dt.contains("-")) {     // dt is in date format
            String[] arr = dt.split("-");
            m = Integer.parseInt(arr[0]);
            d = Integer.parseInt(arr[1]);
            y = Integer.parseInt(arr[2]);
            if (m < 10) {
                arr[0] = "0" + m;
            }
            if (d < 10) {
                arr[1] = "0" + d;
            }
            trip_id = "" + arr[2] + arr[0] + arr[1];
        } else {      // dt is trip_id (yyyymmdd)
            String[] arr = {dt.substring(4, 6), dt.substring(6, dt.length()), dt.substring(0, 4)};
            m = Integer.parseInt(arr[0]);
            d = Integer.parseInt(arr[1]);
            y = Integer.parseInt(arr[2]);
            trip_id = dt;
        }
        date = new Date(y, (m - 1), d);
    }

    public String getMacid() {
        return macid;
    }

    /**
     * set mac id and name of driver
     *
     * @param macid madid can be mac_id or driver name
     */
    public final void setNameAndMacid(String macid) {
        if (macid.contains(":")) {  // macid in in 12:13:23: form
            this.macid = macid;
            for (int i = 0; i < Constants.macids.length; i++) {
                if (Constants.macids[i].equalsIgnoreCase(macid)) {
                    name = Constants.drivers[i + 1];      // offset fue to "Any"
                }
            }
        } else {      // macid is name of driver
            name = macid;
            for (int i = 1; i < Constants.drivers.length; i++) {
                if (Constants.drivers[i].equalsIgnoreCase(macid)) {
                    this.macid = Constants.macids[i - 1];
                }
            }
        }
    }

    public final String getDriverName() {
        return name;
    }

    public String getStartlocation() {
        return startlocation;
    }

    public void setStartlocation(String startlocation) {
        this.startlocation = startlocation;
    }

    public String getEndlocation() {
        return endlocation;
    }

    public void setEndlocation(String endlocation) {
        this.endlocation = endlocation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTimeSpentAsString() {
        int h = timespent.getHours();
        int m = timespent.getMinutes();
        return (h == 0 && m == 0) ? "" : ("" + ((h < 10) ? "0" + h : h) + ":" + ((m < 10) ? "0" + m : m));
    }

    public Time getTimeSpent() {
        return timespent;
    }

    public String getStartTime() {
        int h = sTime.getHours();
        int m = sTime.getMinutes();
        return ("" + ((h < 10) ? "0" + h : h) + ":" + ((m < 10) ? "0" + m : m));
    }

    public String getGateInTime() {
        int h = gTime.getHours();
        int m = gTime.getMinutes();
        if (h == 0 && m == 0) {
            return "";
        } else {
            return ("" + ((h < 10) ? "0" + h : h) + ":" + ((m < 10) ? "0" + m : m));
        }
    }

    public String getEndTime() {
        int h = eTime.getHours();
        int m = eTime.getMinutes();
        return ("" + ((h < 10) ? "0" + h : h) + ":" + ((m < 10) ? "0" + m : m));
    }

    /**
     * set time spent
     *
     * @param time time can be in hh:mm format or null -> calculate from start
     * and end time
     */
    private void setTimeSpent(String time) {
        int minutes = 0;
        int hours = 0;
        if (time == null || time.equalsIgnoreCase("") || time.equalsIgnoreCase("-")) {
            // set time from stime and etime
            if (eTime.getHours() < sTime.getHours()) {
                minutes = (int) (((eTime.getTime() + (24 * 60 * 60 * 1000)) - sTime.getTime()) / 1000) / 60;
            } else {
                minutes = (int) ((eTime.getTime() - sTime.getTime()) / 1000) / 60;
            }
            hours = minutes / 60;
            if (hours > 0) {
                minutes = minutes - (hours * 60);
            }
        } else if (time.contains(":")) {      // time is hh:mm
            String[] arr = time.split(":");
            hours = Integer.parseInt(arr[0]);
            minutes = Integer.parseInt(arr[1]);
        }
        timespent = new Time(hours, minutes, 00);
    }

    /**
     * calculate the distance for list of geographical points
     *
     * @param list list of lat,long points
     * @return distance as string
     */
    private void calculateDistance(ArrayList<Point> list) {
        int m = 0, h = 0;
        Double dist = 0.00;
        Point lastpoint;
        Point currentpoint;
        if (list.size() > 1) {
            lastpoint = list.get(0);
            for (int i = 1; i < list.size(); i++) {
                currentpoint = list.get(i);
                dist = distance(lastpoint.getLatitude(), lastpoint.getLongitude(),
                        currentpoint.getLatitude(), currentpoint.getLongitude(), "M") + dist;
                lastpoint = currentpoint;
            }
        }
        String[] split = (dist + "00").split("\\.");
        distance = "" + split[0] + "." + split[1].substring(0, 2);
    }

    public final String getDistance() {
        return distance;
    }

    public final void setStartTime(String time) {
        String[] stime = time.split(":");
        int sh = Integer.parseInt(stime[0]);
        int sm = Integer.parseInt(stime[1]);
        sTime = new Time(sh, sm, 0);
    }

    public final void setGateInTime(String time) {
        if (time.equalsIgnoreCase("") || time.matches("( )*")) {
            gTime = new Time(0, 0, 0);
        } else {
            String[] gtime = time.split(":");
            int sh = Integer.parseInt(gtime[0]);
            int sm = Integer.parseInt(gtime[1]);
            gTime = new Time(sh, sm, 0);
        }
    }

    public final void setEndTime(String time) {
        String[] etime = time.split(":");
        int eh = Integer.parseInt(etime[0]);
        int em = Integer.parseInt(etime[1]);
        eTime = new Time(eh, em, 0);
    }
    /*::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::                                                                         :*/
    /*::  This routine calculates the distance between two points (given the     :*/
    /*::  latitude/longitude of those points). It is being used to calculate     :*/
    /*::  the distance between two locations using GeoDataSource (TM) prodducts  :*/
    /*::                                                                         :*/
    /*::  Definitions:                                                           :*/
    /*::    South latitudes are negative, east longitudes are positive           :*/
    /*::                                                                         :*/
    /*::  Passed to function:                                                    :*/
    /*::    lat1, lon1 = Latitude and Longitude of point 1 (in decimal degrees)  :*/
    /*::    lat2, lon2 = Latitude and Longitude of point 2 (in decimal degrees)  :*/
    /*::    unit = the unit you desire for results                               :*/
    /*::           where: 'M' is statute miles (default)                         :*/
    /*::                  'K' is kilometers                                      :*/
    /*::                  'N' is nautical miles                                  :*/
    /*::  Worldwide cities and other features databases with latitude longitude  :*/
    /*::  are available at http://www.geodatasource.com                          :*/
    /*::                                                                         :*/
    /*::  For enquiries, please contact sales@geodatasource.com                  :*/
    /*::                                                                         :*/
    /*::  Official Web site: http://www.geodatasource.com                        :*/
    /*::                                                                         :*/
    /*::           GeoDataSource.com (C) All Rights Reserved 2015                :*/
    /*::                                                                         :*/
    /*::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/

    private double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(Math.min(dist, 1.000));
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if ("K".equalsIgnoreCase(unit)) {
            dist = dist * 1.609344;
        } else if ("N".equalsIgnoreCase(unit)) {
            dist = dist * 0.8684;
        }
        return (dist);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::  This function converts decimal degrees to radians             :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::  This function converts radians to decimal degrees             :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

}
