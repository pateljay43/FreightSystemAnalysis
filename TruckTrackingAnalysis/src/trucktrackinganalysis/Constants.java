/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trucktrackinganalysis;

/**
 *
 * @author JAY
 */
public class Constants {

    public static String host = "134.139.249.76";
//    public static String host = "localhost";
    public static int port = 27017;
    public static String databaseName = "eobrdb";
    public static String reportCollectionName = "reports";

    // report collection column names
    public static String _id = "_id";
    public static String trip_id = "trip_id";
    public static String macid = "macid";
    public static String startlocation = "startlocation";
    public static String starttime = "starttime";
    public static String gateintime = "gateintime";
    public static String endlocation = "endlocation";
    public static String endtime = "endtime";
    public static String distance = "distance";
    public static String description = "description";
    public static String error = "error";

    // table fields
    public static final int tDate = 0;
    public static final int tName = 1;
    public static final int tStart = 2;
    public static final int tEnd = 3;
    public static final int tStartTime = 4;
    public static final int tEndTime = 5;
    public static final int tTimeSpent = 6;
    public static final int tWaitTime = 7;
    public static final int tDistance = 8;
    public static final int tError = 9;
    public static final int tDescription = 10;

    // export file types
    public static final int eErrors = 0;
    public static final int eErrorsByDriver = 1;
    public static final int eAllData = 2;
    public static final int eAllDataByDriver = 3;
    public static final int eSelectedData = 4;

    // driver names (sorted manually) mapped with macids
    public static String[] drivers = {"Any", "Kirk", "Neftali Bedoya", "No Name",
        "Octavio Diaz", "Santos Herara"};
    public static String[] macids = {"0c:48:85:e9:b7:4c", "64:89:9a:75:bc:7a", "58:3f:54:c0:f7:fa",
        "0c:48:85:be:3d:00", "0c:48:85:ce:ee:b6",};

    // warehouses and terminal names
    public static String[] locations = {"Any", "Outer Harbo", "APM", "Eagle Marine Services, Ltd",
        "Seaside Transportation Service", "West Basin", "TRAPAC", "Berth", "Total Terminals",
        "SSA Terminal", "PIER C BERTH C60-C62", "Long Beach Container Terminal",
        "International Transportation Service", "Pacific Container Terminal",
        "ICTF RAIL", "ICE INT'L GROUP", "ACT FULFILLMENT",
        "ACT FULFILLMENT WHSE", "ADM WAREHOUSE", "AG FUMIGATION", "ALL AMERICAN HOME CENTER",
        "AMERICOLD CARSON", "ATSI-LOS ANGELES", "BNSF (COMMERCE)", "BAXTER HEALTHCARE CORP",
        "BDP EL SEGUNDO", "BDP INT'L/NALCO", "BROOKVALE", "BROOKVALE CARSON",
        "BROOKVALE CITY OF INDUSTRY", "BROOKVALE STOR YARD", "BROOKVALE TIMBERLAND",
        "BUENA PARK COLD STORAGE", "CES - CARSON", "FLOOR & DÉCOR", "HARBOR WEIGHERS",
        "KOMAR - MIRA LOMA", "METRO YARD", "NESTLES USA -DC 620", "NEW VIEW GIFTS @ OHL WHSE",
        "NYK-TARGET LONG BEACH", "OFF DOCK USA", "PRECISE DISTRIBUTION", "PRICE TRANSFER",
        "PRICE TRANSFER - CET", "PRIME WEST WAREHOUSE", "TARGET CENTER", "TARGET DISTRIBUTION - T553",
        "TARGET DISTRIBUTION CENTER #3807", "TARGET DISTRIBUTION CENTER #T553",
        "TRIMODAL DISTRIBUTION", "TRIMODAL-CITY DIST", "NO PLACE NAME", "Yusen Terminal",
        "California Cartage Express", "Timberland", "Prime Source Building Products",
        "HUDD - CAL CARTAGE YARD", "MS INTERNATIONAL", "CSL EXPRESS LINE", "Vinotemp International",
        "Container Freight Express", "Roxy Trading", "California Multimodal Inc", "PRE900",
        "DAM104", "CUT", "THR230", "UWDC", "APL", "BERF10", "UWDC#2", "BER300", "BER234",
        "BER400", "ICO630", "EAST BAY LOGISTIC", "GEORGE WAREHOUSE", "REGAL LOGISTIC", "GE APPLIANCE"};

}
