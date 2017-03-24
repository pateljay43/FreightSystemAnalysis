/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trucktrackinganalysis;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.opencsv.CSVWriter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.bson.Document;

/**
 *
 * @author JAY
 */
public class TruckTrackingAnalysis extends JFrame implements ActionListener {

    private final JPanel allDataPanel;
    private final JPanel addReportPanel;
    private final JPanel editDetailPanel;
    private ArrayList<Record> reportList;
    private final ArrayList<String> timespent;
    private JTabbedPane tabPane;

    private static MongoClient mongoClient;
    private static MongoDatabase db;
    private static FindIterable<Document> trips;
    private static FindIterable<Document> reports;
    private static MongoCollection<Document> tripsCollection;
    private static MongoCollection<Document> reportsCollection;

    public TruckTrackingAnalysis() {
        // connect to the university server
        mongoClient = new MongoClient(Constants.host, Constants.port);
        // get connection to the specified database
        db = mongoClient.getDatabase(Constants.databaseName);
        // try to create collection if it does not exist
        try {
            db.createCollection(Constants.reportCollectionName);
        } catch (Exception ex) {
            System.out.println("Collection already exists");
        } finally {
            // connect to the specified collection
            reportsCollection = db.getCollection(Constants.reportCollectionName);
        }
        // connect to the raw data collection
        tripsCollection = db.getCollection("trips");
        // retrive all documents from raw data, then sort them based on id
        trips = tripsCollection.find().sort(new Document("id", 1));
        // retrive all documents from report's collection, 
        // then sort in order of [trip_id,mac_id,starttime]
        reports = reportsCollection.find().sort(new BasicDBObject()
                .append(Constants.trip_id, 1)
                .append(Constants.macid, 1)
                .append(Constants.starttime, 1));
        // list of time spent for each transaction returned after query
        timespent = new ArrayList<>();
        // UI tab pane for adding a record or displaying all the transactions
        tabPane = new JTabbedPane();
        // panel to display all the transactions
        allDataPanel = new JPanel();
        // panel to add record to database
        addReportPanel = new JPanel();
        // panel to edit the selected transaction
        editDetailPanel = new JPanel();

        // initialize all the panels
        initAllDataPanel();
        initAddReportPanel();
        initEditDetailPanel();

        // add two tabs with title mapped to specific panel
        tabPane.addTab("Add Record", addReportPanel);
        tabPane.addTab("All Data", allDataPanel);

        // set panel with index 1 selected initially. (index starts with 0)
        tabPane.setSelectedIndex(1);

        // refresh all data panel when it is selected from tab pane
        tabPane.addChangeListener((ChangeEvent e) -> {
            if (tabPane.getSelectedIndex() == 1) {
                query();
            }
        });

        // set bounds for tab pane
        tabPane.setBounds(0, 0, 1280, 650);
        // add the tab pane to the main frame UI
        add(tabPane, BorderLayout.CENTER);
        // add the edit record panel to the main frame UI
        add(editDetailPanel, BorderLayout.CENTER);
        // refresh the table with all the transactions in database
        refresh(null);
        setTitle("Truck Tracking Report");
        setLayout(null);
        setSize(1280, 670);
        setMinimumSize(new Dimension(1280, 670));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    // variables for edit record panel
    private JLabel editDriverNameLBL;
    private JLabel editStartLocationLBL;
    private JLabel editEndLocationLBL;
    private JLabel editStartTimeLBL;
    private JLabel editGateInTimeLBL;
    private JLabel editEndTimeLBL;
    private JLabel editDateLBL;
    private JTextArea editDate;
    private JComboBox editDriverNameCB;
    private JComboBox editStartLocationCB;
    private JComboBox editEndLocationCB;
    private JTextArea editStartTime;
    private JTextArea editGateInTime;
    private JTextArea editEndTime;
    private JLabel editDescriptionLBL;
    private JTextArea editDescriptionTxt;
    private JScrollPane editDescriptionContainer;
    private JCheckBox editErrorCB;
    private JButton editDoneBtn;
    private JButton editCancelBtn;
    private static int row;     // row pointer for selected row in table

    private void initEditDetailPanel() {
        row = -1;
        editDateLBL = new JLabel("Date: (mm-dd-yyyy)");
        editDriverNameLBL = new JLabel("Driver Name: ");
        editStartLocationLBL = new JLabel("Start Location: ");
        editStartTimeLBL = new JLabel("Start Time: (hh:mm)");
        editGateInTimeLBL = new JLabel("GateIn Time: (hh:mm)");
        editEndLocationLBL = new JLabel("End Location: ");
        editEndTimeLBL = new JLabel("End Time: (hh:mm)");
        editDate = new JTextArea();
        editDriverNameCB = new JComboBox(Constants.drivers);
        editStartLocationCB = new JComboBox(Constants.locations);
        editStartTime = new JTextArea();
        editGateInTime = new JTextArea();
        editEndLocationCB = new JComboBox(Constants.locations);
        editEndTime = new JTextArea();
        editDescriptionLBL = new JLabel("Description: ");
        editDescriptionTxt = new JTextArea();
        editDescriptionTxt.setLineWrap(true);
        editDescriptionContainer = new JScrollPane(editDescriptionTxt,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        editErrorCB = new JCheckBox("Error");
        editDoneBtn = new JButton("Done");
        editCancelBtn = new JButton("Cancel");
        editDriverNameCB.removeItemAt(0);        // remove "Any"
        editStartLocationCB.removeItemAt(0);        // remove "Any"
        editEndLocationCB.removeItemAt(0);        // remove "Any"
        // update the database when edit button is pressed
        editDoneBtn.addActionListener((ActionEvent e) -> {
            Record r = checkEditDetails();
            if (r != null) {
                // update database
                update(row, r);
                row = -1;
                editDescriptionTxt.setText("");
                changeView(true);
            }
        });
        // return to all data panel view
        editCancelBtn.addActionListener((ActionEvent e) -> {
            row = -1;
            editDescriptionTxt.setText("");
            changeView(true);
        });

        editDateLBL.setBounds(10, 10, 150, 20);
        editDate.setBounds(150, 10, 150, 20);
        editDriverNameLBL.setBounds(510, 10, 150, 20);
        editDriverNameCB.setBounds(650, 10, 200, 20);
        editStartLocationLBL.setBounds(10, 50, 150, 20);
        editStartLocationCB.setBounds(150, 50, 300, 20);
        editStartTimeLBL.setBounds(510, 50, 150, 20);
        editStartTime.setBounds(650, 50, 150, 20);
        editGateInTimeLBL.setBounds(510, 100, 150, 20);
        editGateInTime.setBounds(650, 100, 150, 20);
        editEndLocationLBL.setBounds(10, 150, 150, 20);
        editEndLocationCB.setBounds(150, 150, 300, 20);
        editEndTimeLBL.setBounds(510, 150, 150, 20);
        editEndTime.setBounds(650, 150, 150, 20);
        editDescriptionLBL.setBounds(10, 200, 150, 20);
        editDescriptionContainer.setBounds(150, 200, 500, 100);
        editErrorCB.setBounds(150, 330, 100, 30);
        editDoneBtn.setBounds(150, 380, 100, 30);
        editCancelBtn.setBounds(300, 380, 100, 30);

        editDetailPanel.add(editDateLBL);
        editDetailPanel.add(editDate);
        editDetailPanel.add(editDriverNameLBL);
        editDetailPanel.add(editDriverNameCB);
        editDetailPanel.add(editStartLocationLBL);
        editDetailPanel.add(editStartLocationCB);
        editDetailPanel.add(editStartTimeLBL);
        editDetailPanel.add(editStartTime);
        editDetailPanel.add(editGateInTimeLBL);
        editDetailPanel.add(editGateInTime);
        editDetailPanel.add(editEndLocationLBL);
        editDetailPanel.add(editEndLocationCB);
        editDetailPanel.add(editEndTimeLBL);
        editDetailPanel.add(editEndTime);
        editDetailPanel.add(editDescriptionLBL);
        editDetailPanel.add(editDescriptionContainer);
        editDetailPanel.add(editErrorCB);
        editDetailPanel.add(editDoneBtn);
        editDetailPanel.add(editCancelBtn);

        editDetailPanel.setBounds(250, 100, 1280, 650);
        editDetailPanel.setLayout(null);
        editDetailPanel.setVisible(false);
    }

    /**
     *
     * @param homePanel true - set tabpane visible, false - set edit panel
     * visible
     */
    private void changeView(boolean homePanel) {
        tabPane.setVisible(homePanel);
        editDetailPanel.setVisible(!homePanel);
        if (homePanel) {
            query();
            setTitle("Truck Tracking Report");
            setLocationRelativeTo(null);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
        } else {
            setTitle("Edit Detail");
            setLocationRelativeTo(null);
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        }
        revalidate();
        repaint();
    }

    // variables for all data panel
    private JScrollPane tableScrollPane;
    private JTable tableContainer;
    private TableModel table;
    private JLabel avgTimeSpentlbl;
    private JLabel avgTimeSpentView;
    private JLabel dateLBL;
    private JLabel driverNameLBL;
    private JLabel startLocationLBL;
    private JLabel endLocationLBL;
    private JLabel searchLBL;
    private JButton deleteBtn;
    private JButton searchBtn;
    private JButton getAllBtn;
    private JButton editBtn;
    private JButton exportSelectedBtn;
    private JButton exportErrorBtn;
    private JButton exportErrorByDriverBtn;
    private JButton exportAllDataBtn;
    private JButton exportAllDataByDriverBtn;
    private JComboBox driverNameCB;
    private JComboBox startLocationCB;
    private JComboBox endLocationCB;
    private JTextArea dateTxt;
    private JTextArea searchTxt;
    private JCheckBox errorCB;
    private ArrayList<Integer> selectedRows;    // list of row ids for multiple selected rows

    private void initAllDataPanel() {
        avgTimeSpentlbl = new JLabel("Average Time Spent");
        dateLBL = new JLabel("Date (mm-dd-yyyy)");
        driverNameLBL = new JLabel("Driver");
        startLocationLBL = new JLabel("Start Location");
        endLocationLBL = new JLabel("End Location");
        searchLBL = new JLabel("Search:");
        avgTimeSpentView = new JLabel();
        table = new TableModel();
        tableContainer = new JTable(table);
        tableContainer.setGridColor(Color.gray);
        tableContainer.setShowVerticalLines(false);
        tableContainer.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tableContainer.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (e.getValueIsAdjusting()) {      // called only when mouse is used to select row
                selectedRows.clear();
                for (int i = 0; i < tableContainer.getRowCount(); i++) {
                    if (tableContainer.isRowSelected(i)) {
                        selectedRows.add(i);
                    }
                }
                if (!selectedRows.isEmpty()) {
                    calculateAvgTimeSpent(selectedRows);
                }
            }
        });
        tableScrollPane = new JScrollPane(tableContainer,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        resizeColumnWidth(tableContainer);
        deleteBtn = new JButton("Delete");
        editBtn = new JButton("Edit");
        exportSelectedBtn = new JButton("Export Selected");
        exportErrorBtn = new JButton("Export Errors");
        exportErrorByDriverBtn = new JButton("Export Errors By Driver");
        exportAllDataBtn = new JButton("Export All Data");
        exportAllDataByDriverBtn = new JButton("Export All Data By Driver");
        searchBtn = new JButton("Search");
        getAllBtn = new JButton("Get All");
        dateTxt = new JTextArea();
        // fill the drop down box with the static list
        driverNameCB = new JComboBox(Constants.drivers);
        startLocationCB = new JComboBox(Constants.locations);
        endLocationCB = new JComboBox(Constants.locations);
        selectedRows = new ArrayList<>();
        searchTxt = new JTextArea();
        errorCB = new JCheckBox("Error");
        searchBtn.setName("Search");
        getAllBtn.setName("GetAll");
        deleteBtn.setName("Delete");
        editBtn.setName("Edit");
        exportSelectedBtn.setName("Export Selected");
        exportErrorBtn.setName("Export Errors");
        exportErrorByDriverBtn.setName("Export Errors By Driver");
        exportAllDataBtn.setName("Export All Data");
        exportAllDataByDriverBtn.setName("Export All Data By Driver");

        // add action listener for all the buttons on panel
        searchBtn.addActionListener(this);
        getAllBtn.addActionListener(this);
        deleteBtn.addActionListener(this);
        editBtn.addActionListener(this);
        exportSelectedBtn.addActionListener(this);
        exportErrorBtn.addActionListener(this);
        exportErrorByDriverBtn.addActionListener(this);
        exportAllDataBtn.addActionListener(this);
        exportAllDataByDriverBtn.addActionListener(this);

        avgTimeSpentlbl.setBounds(10, 10, 130, 30);
        avgTimeSpentView.setBounds(55, 35, 40, 30);
        dateLBL.setBounds(150, 10, 130, 30);
        dateTxt.setBounds(160, 40, 100, 20);
        searchLBL.setBounds(50, 65, 70, 30);
        searchTxt.setBounds(160, 70, 300, 20);
        errorCB.setBounds(520, 65, 100, 30);
        searchBtn.setBounds(620, 65, 100, 30);
        driverNameLBL.setBounds(330, 10, 100, 30);
        driverNameCB.setBounds(300, 40, 200, 20);
        startLocationLBL.setBounds(530, 10, 120, 30);
        startLocationCB.setBounds(500, 40, 300, 20);
        endLocationLBL.setBounds(830, 10, 120, 30);
        endLocationCB.setBounds(800, 40, 300, 20);
        tableScrollPane.setBounds(0, 100, 1100, 500);
        getAllBtn.setBounds(1105, 65, 150, 30);
        deleteBtn.setBounds(1105, 115, 150, 30);
        editBtn.setBounds(1105, 165, 150, 30);
        exportErrorBtn.setBounds(1105, 215, 150, 30);
        exportErrorByDriverBtn.setBounds(1105, 265, 150, 30);
        exportAllDataBtn.setBounds(1105, 315, 150, 30);
        exportAllDataByDriverBtn.setBounds(1105, 365, 150, 30);
        exportSelectedBtn.setBounds(1105, 415, 150, 30);

        allDataPanel.setLayout(null);

        allDataPanel.add(dateTxt);
        allDataPanel.add(driverNameCB);
        allDataPanel.add(startLocationCB);
        allDataPanel.add(endLocationCB);
        allDataPanel.add(deleteBtn);
        allDataPanel.add(editBtn);
        allDataPanel.add(exportSelectedBtn);
        allDataPanel.add(exportErrorBtn);
        allDataPanel.add(exportErrorByDriverBtn);
        allDataPanel.add(exportAllDataBtn);
        allDataPanel.add(exportAllDataByDriverBtn);
        allDataPanel.add(searchLBL);
        allDataPanel.add(searchTxt);
        allDataPanel.add(errorCB);
        allDataPanel.add(searchBtn);
        allDataPanel.add(getAllBtn);
        allDataPanel.add(avgTimeSpentlbl);
        allDataPanel.add(dateLBL);
        allDataPanel.add(driverNameLBL);
        allDataPanel.add(startLocationLBL);
        allDataPanel.add(endLocationLBL);
        allDataPanel.add(avgTimeSpentView);
        allDataPanel.add(tableScrollPane);
    }

    /**
     * set the width of all columns based on the largest content in given table
     *
     * @param table table whose column's width is to be modified
     */
    public void resizeColumnWidth(JTable table) {
        final TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = 0; // Min width
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                if (column == Constants.tStartTime
                        || column == Constants.tEndTime
                        || column == Constants.tTimeSpent
                        || column == Constants.tWaitTime
                        || column == Constants.tDistance
                        || column == Constants.tError) {
                    width = Math.max(comp.getPreferredSize().width + 30, width);
                } else {
                    width = Math.max(comp.getPreferredSize().width + 10, width);
                }
            }
            columnModel.getColumn(column).setPreferredWidth(width);
        }
    }

    // variables for add record panel
    private JLabel addDriverNameLBL;
    private JLabel addStartLocationLBL;
    private JLabel addEndLocationLBL;
    private JLabel addStartTimeLBL;
    private JLabel addGateInTimeLBL;
    private JLabel addEndTimeLBL;
    private JLabel addDateLBL;
    private JLabel addDescriptionLBL;
    private JTextArea addDate;
    private JComboBox addDriverNameCB;
    private JComboBox addStartLocationCB;
    private JComboBox addEndLocationCB;
    private JTextArea addStartTime;
    private JTextArea addGateInTime;
    private JTextArea addEndTime;
    private JTextArea addDescription;
    private JScrollPane descriptionContainer;
    private JCheckBox addErrorCB;
    private JButton doneBtn;
    private JButton fenceInBtn;
    private JButton fenceOutBtn;
    private JButton gateInBtn;
    private JButton travelledBtn;
    private JButton loadPickedUpBtn;
    private JButton loadDeliveredBtn;
    private JButton loadUnavailableBtn;
    private JButton chassisPickedUpBtn;
    private JButton chassisDeliveredBtn;
    private JButton chassisUnavailableBtn;
    private JButton emptyPickedUpBtn;
    private JButton emptyDeliveredBtn;
    private JButton emptyUnavailableBtn;

    private void initAddReportPanel() {
        addDateLBL = new JLabel("Date: (mm-dd-yyyy)");
        addDriverNameLBL = new JLabel("Driver Name: ");
        addStartLocationLBL = new JLabel("Start Location: ");
        addStartTimeLBL = new JLabel("Start Time: (hh:mm)");
        addGateInTimeLBL = new JLabel("GateIn Time: (hh:mm)");
        addEndLocationLBL = new JLabel("End Location: ");
        addEndTimeLBL = new JLabel("End Time: (hh:mm)");
        addDescriptionLBL = new JLabel("Description: ");
        addDate = new JTextArea();
        addDriverNameCB = new JComboBox(Constants.drivers);
        addStartLocationCB = new JComboBox(Constants.locations);
        addStartTime = new JTextArea();
        addGateInTime = new JTextArea();
        addEndLocationCB = new JComboBox(Constants.locations);
        addEndTime = new JTextArea();
        addDescription = new JTextArea();
        addDescription.setLineWrap(true);
        descriptionContainer = new JScrollPane(addDescription,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        addErrorCB = new JCheckBox("Error");
        doneBtn = new JButton("Done");

        fenceInBtn = new JButton("Fence In");
        fenceOutBtn = new JButton("Fence Out");
        gateInBtn = new JButton("Gate In");
        travelledBtn = new JButton("Travelled");
        loadDeliveredBtn = new JButton("Load Delivered");
        loadPickedUpBtn = new JButton("Load Picked Up");
        loadUnavailableBtn = new JButton("Load Unavailable");
        emptyDeliveredBtn = new JButton("Empty Delivered");
        emptyPickedUpBtn = new JButton("Empty Picked Up");
        emptyUnavailableBtn = new JButton("Empty Unavailable");
        chassisDeliveredBtn = new JButton("Chassis Delivered");
        chassisPickedUpBtn = new JButton("Chassis Picked Up");
        chassisUnavailableBtn = new JButton("Chassis Unavailable");

        addDriverNameCB.removeItemAt(0);        // remove "Any"
        addStartLocationCB.removeItemAt(0);        // remove "Any"
        addEndLocationCB.removeItemAt(0);        // remove "Any"

        addDateLBL.setBounds(260, 110, 150, 20);
        addDate.setBounds(400, 110, 150, 20);
        addDriverNameLBL.setBounds(760, 110, 150, 20);
        addDriverNameCB.setBounds(900, 110, 200, 20);
        addStartLocationLBL.setBounds(260, 150, 150, 20);
        addStartLocationCB.setBounds(400, 150, 300, 20);
        addStartTimeLBL.setBounds(760, 150, 150, 20);
        addStartTime.setBounds(900, 150, 150, 20);
        addGateInTimeLBL.setBounds(760, 200, 150, 20);
        addGateInTime.setBounds(900, 200, 150, 20);
        addEndLocationLBL.setBounds(260, 250, 150, 20);
        addEndLocationCB.setBounds(400, 250, 300, 20);
        addEndTimeLBL.setBounds(760, 250, 150, 20);
        addEndTime.setBounds(900, 250, 150, 20);
        fenceInBtn.setBounds(100, 300, 150, 30);
        fenceOutBtn.setBounds(1000, 300, 150, 30);
        gateInBtn.setBounds(100, 340, 150, 30);
        loadDeliveredBtn.setBounds(100, 390, 150, 30);
        loadPickedUpBtn.setBounds(100, 430, 150, 30);
        loadUnavailableBtn.setBounds(100, 470, 150, 30);
        emptyDeliveredBtn.setBounds(1000, 390, 150, 30);
        emptyPickedUpBtn.setBounds(1000, 430, 150, 30);
        emptyUnavailableBtn.setBounds(1000, 470, 150, 30);
        chassisDeliveredBtn.setBounds(100, 510, 150, 30);
        chassisPickedUpBtn.setBounds(100, 550, 150, 30);
        chassisUnavailableBtn.setBounds(1000, 510, 150, 30);
        travelledBtn.setBounds(1000, 550, 150, 30);
        addDescriptionLBL.setBounds(260, 300, 150, 20);
        descriptionContainer.setBounds(400, 300, 500, 100);
        addErrorCB.setBounds(400, 430, 100, 30);
        doneBtn.setBounds(400, 480, 100, 30);

        doneBtn.setName("Done");

        doneBtn.addActionListener(this);
        fenceInBtn.addActionListener(this);
        fenceOutBtn.addActionListener(this);
        gateInBtn.addActionListener(this);
        loadDeliveredBtn.addActionListener(this);
        loadPickedUpBtn.addActionListener(this);
        loadUnavailableBtn.addActionListener(this);
        emptyDeliveredBtn.addActionListener(this);
        emptyPickedUpBtn.addActionListener(this);
        emptyUnavailableBtn.addActionListener(this);
        chassisDeliveredBtn.addActionListener(this);
        chassisPickedUpBtn.addActionListener(this);
        chassisUnavailableBtn.addActionListener(this);
        travelledBtn.addActionListener(this);

        addReportPanel.setLayout(null);
        addReportPanel.add(addDateLBL);
        addReportPanel.add(addDate);
        addReportPanel.add(addDriverNameLBL);
        addReportPanel.add(addDriverNameCB);
        addReportPanel.add(addStartLocationLBL);
        addReportPanel.add(addStartLocationCB);
        addReportPanel.add(addStartTimeLBL);
        addReportPanel.add(addStartTime);
        addReportPanel.add(addGateInTimeLBL);
        addReportPanel.add(addGateInTime);
        addReportPanel.add(addEndLocationLBL);
        addReportPanel.add(addEndLocationCB);
        addReportPanel.add(addEndTimeLBL);
        addReportPanel.add(addEndTime);
        addReportPanel.add(addDescriptionLBL);
        addReportPanel.add(descriptionContainer);
        addReportPanel.add(addErrorCB);
        addReportPanel.add(doneBtn);
        addReportPanel.add(fenceInBtn);
        addReportPanel.add(fenceOutBtn);
        addReportPanel.add(gateInBtn);
        addReportPanel.add(loadDeliveredBtn);
        addReportPanel.add(loadPickedUpBtn);
        addReportPanel.add(loadUnavailableBtn);
        addReportPanel.add(emptyDeliveredBtn);
        addReportPanel.add(emptyPickedUpBtn);
        addReportPanel.add(emptyUnavailableBtn);
        addReportPanel.add(chassisDeliveredBtn);
        addReportPanel.add(chassisPickedUpBtn);
        addReportPanel.add(chassisUnavailableBtn);
        addReportPanel.add(travelledBtn);
    }

    /**
     *
     * @param doc document for modifying the query to report database. null ->
     * fetch all the transactions from database
     */
    private void refresh(Document doc) {
        if (doc == null) {
            reports = reportsCollection.find().sort(new BasicDBObject()
                    .append(Constants.trip_id, 1)
                    .append(Constants.macid, 1)
                    .append(Constants.starttime, 1));
        } else {
            reports = reportsCollection.find(doc).sort(new BasicDBObject()
                    .append(Constants.trip_id, 1)
                    .append(Constants.macid, 1)
                    .append(Constants.starttime, 1));
        }
        timespent.clear();
        generateReportList();
        calculateAvgTimeSpent(null);       // null -> calculate for all rows
        table.fireTableDataChanged();
    }

    /**
     * insert new record to the database
     *
     * @param r record to be inserted
     */
    private void insert(Record r) {
        Document mydoc = new Document();
        mydoc.append(Constants.trip_id, r.getTrip_id())
                .append(Constants.macid, r.getMacid())
                .append(Constants.startlocation, r.getStartlocation())
                .append(Constants.starttime, r.getStartTime())
                .append(Constants.gateintime, r.getGateInTime())
                .append(Constants.endlocation, r.getEndlocation())
                .append(Constants.endtime, r.getEndTime())
                .append(Constants.description, r.getDescription())
                .append(Constants.distance, r.getDistance())
                .append(Constants.error, r.isError());
        reportsCollection.insertOne(mydoc);
    }

    /**
     * update a record in the database
     *
     * @param index index of the record to be updated
     * @param r updated record
     */
    private void update(int index, Record r) {
        int i = 0;
        Document mydoc = null;
        for (Document d : reports) {
            if (i == index) {
                mydoc = d;
                break;
            }
            i++;
        }
        reportsCollection.updateOne(new Document(Constants._id, mydoc.get(Constants._id)),
                new Document("$set", new Document().append(Constants.trip_id, r.getTrip_id())
                        .append(Constants.macid, r.getMacid())
                        .append(Constants.startlocation, r.getStartlocation())
                        .append(Constants.starttime, r.getStartTime())
                        .append(Constants.gateintime, r.getGateInTime())
                        .append(Constants.endlocation, r.getEndlocation())
                        .append(Constants.endtime, r.getEndTime())
                        .append(Constants.description, r.getDescription())
                        .append(Constants.distance, r.getDistance())
                        .append(Constants.error, r.isError())));
    }

    /**
     * format the date in yyyymmdd format which is trip_id in database
     *
     * @param dt date in mm-dd-yyyy format
     * @return trip_id in yyyymmdd format
     */
    private static String getTripID(String dt) {
        String[] arr = dt.split("-");
        String trip_id;
        int m, d;
        m = Integer.parseInt(arr[0]);
        d = Integer.parseInt(arr[1]);
        if (m < 10) {
            arr[0] = "0" + m;
        }
        if (d < 10) {
            arr[1] = "0" + d;
        }
        trip_id = "" + arr[2] + arr[0] + arr[1];
        return trip_id;
    }

    /**
     * get the macid mapped for the driver name statically in constants file
     *
     * @param name name of the driver
     * @return macid of the driver
     */
    private static String getMacId(String name) {
        for (int i = 1; i < Constants.drivers.length; i++) {
            if (Constants.drivers[i].equalsIgnoreCase(name)) {
                return Constants.macids[i - 1];
            }
        }
        return null;
    }

    /**
     * query the database based on the filters selected (optionally)
     */
    private void query() {
        String dt = dateTxt.getText();
        if (!(dt.equalsIgnoreCase("") || dt.matches("( )*") || dt.matches("(\t)*") || dt.matches("(\\d)*(-)(\\d)*(-)(\\d)*"))) {
            JOptionPane.showMessageDialog(this, "Enter date in mm-dd-yyyy format",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String driver = (String) driverNameCB.getSelectedItem();
        String start = (String) startLocationCB.getSelectedItem();
        String end = (String) endLocationCB.getSelectedItem();
        String search = (String) searchTxt.getText();
        boolean error = errorCB.isSelected();
        Document mydoc = new Document();
        if (dt.matches("(\\d)*(-)(\\d)*(-)(\\d)*")) {
            mydoc.append(Constants.trip_id, getTripID(dt));
        }
        if (!driver.equalsIgnoreCase("Any")) {
            mydoc.append(Constants.macid, getMacId(driver));
        }
        if (!start.equalsIgnoreCase("Any")) {
            mydoc.append(Constants.startlocation, start);
        }
        if (!end.equalsIgnoreCase("Any")) {
            mydoc.append(Constants.endlocation, end);
        }
        if (!(search.equalsIgnoreCase("") || search.matches("( )*") || search.matches("(\t)*"))) {
            if (search.contains("!")) {
                mydoc.append(Constants.description, new Document("$not", Pattern.compile(search.substring(1, search.length()), Pattern.CASE_INSENSITIVE)));
            } else {
                mydoc.append(Constants.description, new Document("$regex", Pattern.compile(search, Pattern.CASE_INSENSITIVE)));
            }
        }
        if (error) {
            mydoc.append(Constants.error, error);
        }
        refresh(mydoc);
    }

    /**
     * delete the selected transaction(s) from the database
     */
    private void delete() {
        ArrayList<Integer> removeList = new ArrayList<>();
        for (int i = 0; i < tableContainer.getRowCount(); i++) {
            if (tableContainer.isRowSelected(i)) {
                removeList.add(i);
            }
        }
        if (!removeList.isEmpty()) {
            Integer i = 0;
            for (Document d : reports) {
                if (removeList.contains(i)) {
                    reportsCollection.deleteOne(new BasicDBObject("_id", d.get("_id")));
                    boolean b = removeList.remove(i);
                }
                i++;
            }
            query();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // name of the element e which fired the action
        String name = ((JButton) e.getSource()).getName();
        int index = tabPane.getSelectedIndex();
        if (index == 0) {   // current pane is add record pane
            if (name != null && name.equalsIgnoreCase("Done")) {    // insert new record
                Record r = checkAddDetails();   // validate the new data to be inserted
                if (r != null) {
                    // update database
                    insert(r);
                }
            } else {        // add description based on the button pressed
                String text = addDescription.getText();
                if (text == null || text.equalsIgnoreCase("") || text.matches("( )*")) {
                    addDescription.setText(text + ((JButton) e.getSource()).getText());
                } else {
                    addDescription.setText(text + " - " + ((JButton) e.getSource()).getText());
                }
            }
        } else if (index == 1) {    // current pane is all data pane
            if (name.equalsIgnoreCase("Search")) {  // search button pressed
                query();
            } else if (name.equalsIgnoreCase("GetAll")) {   // get all data from database
                dateTxt.setText("");
                driverNameCB.setSelectedIndex(0);
                startLocationCB.setSelectedIndex(0);
                endLocationCB.setSelectedIndex(0);
                refresh(null);
            } else if (name.equalsIgnoreCase("Delete")) {   // delete selected rows
                delete();
            } else if (name.equalsIgnoreCase("Edit")) { // edit the selected row
                row = -1;
                for (int i = 0; i < tableContainer.getRowCount(); i++) {
                    if (tableContainer.isRowSelected(i)) {
                        row = i;
                    }
                }
                if (row != -1) {
                    Record record = reportList.get(row);
                    editDate.setText(record.getDate());
                    editDriverNameCB.setSelectedItem(record.getDriverName());
                    editStartLocationCB.setSelectedItem(record.getStartlocation());
                    editEndLocationCB.setSelectedItem(record.getEndlocation());
                    editStartTime.setText(record.getStartTime());
                    editGateInTime.setText(record.getGateInTime());
                    editEndTime.setText(record.getEndTime());
                    editDescriptionTxt.setText(record.getDescription());
                    editErrorCB.setSelected(record.isError());
                    changeView(false);
                }
            } else if (name.equalsIgnoreCase("Export Errors")) {
                export(Constants.eErrors);
            } else if (name.equalsIgnoreCase("Export All Data")) {
                export(Constants.eAllData);
            } else if (name.equalsIgnoreCase("Export Errors By Driver")) {
                export(Constants.eErrorsByDriver);
            } else if (name.equalsIgnoreCase("Export All Data By Driver")) {
                export(Constants.eAllDataByDriver);
            } else if (name.equalsIgnoreCase("Export Selected")) {
                export(Constants.eSelectedData);
            }
        }
    }

    /**
     * export all the transactions currently in the table to excel spreadsheet
     * csv file
     *
     * @param type type transactions to be included in the file
     */
    private void export(int type) {
        CSVWriter writer = null;
        switch (type) {
            case Constants.eErrors:
                try {
                    writer = new CSVWriter(new FileWriter("Errors.csv"), ',');
                    String[] columnNames = {"Date", "Name", "Start",
                        "End", "StartTime", "EndTime", "Time Spent", "Wait Time",
                        "Distance", "Error", "Description"};
                    writer.writeNext(columnNames);
                    int nRow = table.getRowCount();
                    int nCol = table.getColumnCount();
                    //write the header information

                    for (int i = 0; i < nRow; i++) {
                        String[] col = new String[nCol];
                        if ((boolean) table.getValueAt(i, Constants.tError)) {
                            for (int j = 0; j < nCol; j++) {
//                                if (j != Constants.tError) {
//                                    if (j == Constants.tDescription) {
//                                        col[j - 1] = (String) table.getValueAt(i, j);
//                                    } else {
//                                col[j] = (String) table.getValueAt(i, j);
//                                    }
//                                }
                                if (j == Constants.tError) {
                                    col[j] = ((Boolean) table.getValueAt(i, j)).toString();
                                } else {
                                    col[j] = (String) table.getValueAt(i, j);
                                }
                            }
                            writer.writeNext(col);
                        }
                    }
                    writer.close();
                } catch (IOException ex) {
                    Logger.getLogger(TruckTrackingAnalysis.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            case Constants.eErrorsByDriver:
                try {
                    for (String driver : Constants.drivers) {
                        if (driver.equalsIgnoreCase("Any")) {
                            continue;
                        }
                        writer = new CSVWriter(new FileWriter("Error By " + driver + ".csv"), ',');
                        String[] columnNames = {"Date", "Name", "Start",
                            "End", "StartTime", "EndTime", "Time Spent", "Wait Time",
                            "Distance", "Error", "Description"};
                        writer.writeNext(columnNames);
                        int nRow = table.getRowCount();
                        int nCol = table.getColumnCount();
                        //write the header information
                        for (int i = 0; i < nRow; i++) {
                            String[] col = new String[nCol];
                            if ((boolean) table.getValueAt(i, Constants.tError)) {
                                if (((String) table.getValueAt(i, Constants.tName)).equalsIgnoreCase(driver)) {
                                    for (int j = 0; j < nCol; j++) {
                                        if (j == Constants.tError) {
                                            col[j] = ((Boolean) table.getValueAt(i, j)).toString();
                                        } else {
                                            col[j] = (String) table.getValueAt(i, j);
                                        }
                                    }
                                    writer.writeNext(col);
                                }
                            }
                        }
                        writer.close();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(TruckTrackingAnalysis.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            case Constants.eAllData:
                try {
                    writer = new CSVWriter(new FileWriter("All Data.csv"), ',');
                    String[] columnNames = {"Date", "Name", "Start",
                        "End", "StartTime", "EndTime", "Time Spent", "Wait Time",
                        "Distance", "Error", "Description"};
                    writer.writeNext(columnNames);
                    int nRow = table.getRowCount();
                    int nCol = table.getColumnCount();
                    //write the header information
                    for (int i = 0; i < nRow; i++) {
                        String[] col = new String[nCol];
                        for (int j = 0; j < nCol; j++) {
                            if (j == Constants.tError) {
                                col[j] = ((Boolean) table.getValueAt(i, j)).toString();
                            } else {
                                col[j] = (String) table.getValueAt(i, j);
                            }
                        }
                        writer.writeNext(col);
                    }
                    writer.close();
                } catch (IOException ex) {
                    Logger.getLogger(TruckTrackingAnalysis.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            case Constants.eAllDataByDriver:
                try {
                    for (String driver : Constants.drivers) {
                        if (driver.equalsIgnoreCase("Any")) {
                            continue;
                        }
                        writer = new CSVWriter(new FileWriter("All Data for " + driver + ".csv"), ',');
                        String[] columnNames = {"Date", "Name", "Start",
                            "End", "StartTime", "EndTime", "Time Spent", "Wait Time",
                            "Distance", "Error", "Description"};
                        writer.writeNext(columnNames);
                        int nRow = table.getRowCount();
                        int nCol = table.getColumnCount();
                        //write the header information
                        for (int i = 0; i < nRow; i++) {
                            String[] col = new String[nCol];
                            if (((String) table.getValueAt(i, Constants.tName)).equalsIgnoreCase(driver)) {
                                for (int j = 0; j < nCol; j++) {
                                    if (j == Constants.tError) {
                                        col[j] = ((Boolean) table.getValueAt(i, j)).toString();
                                    } else {
                                        col[j] = (String) table.getValueAt(i, j);
                                    }
                                }
                                writer.writeNext(col);
                            }
                        }
                        writer.close();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(TruckTrackingAnalysis.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            case Constants.eSelectedData:
                try {
                    writer = new CSVWriter(new FileWriter("Selected Data.csv"), ',');
                    String[] columnNames = {"Date", "Name", "Start",
                        "End", "StartTime", "EndTime", "Time Spent", "Wait Time",
                        "Distance", "Error", "Description"};
                    writer.writeNext(columnNames);
                    int nCol = table.getColumnCount();
                    //write the header information
                    int[] rows = tableContainer.getSelectedRows();
                    if (rows.length > 0) {
                        for (int i : rows) {
                            String[] col = new String[nCol];
                            for (int j = 0; j < nCol; j++) {
                                if (j == Constants.tError) {
                                    col[j] = ((Boolean) table.getValueAt(i, j)).toString();
                                } else {
                                    col[j] = (String) table.getValueAt(i, j);
                                }
                            }
                            writer.writeNext(col);
                        }
                    }
                    writer.close();
                } catch (IOException ex) {
                    Logger.getLogger(TruckTrackingAnalysis.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
        }
    }

    /**
     * calculate the average time spent
     *
     * @param list list of transactions to be included in the calculation. null
     * -> include all the transaction currently in the table
     */
    public void calculateAvgTimeSpent(ArrayList<Integer> list) {
        if (reportList.isEmpty()) {
            avgTimeSpentView.setText("00:00");
        } else {
            timespent.clear();
            if (list != null) {
                int i = 0;
                for (Record r : reportList) {
                    if (list.contains(i)) {
                        timespent.add(r.getTimeSpentAsString());
                    }
                    i++;
                }
            } else if (list == null) {
                reportList.stream().forEach((Record r) -> {
                    timespent.add(r.getTimeSpentAsString());
                });
            }
            int m = 0;
            int h = 0;
            for (String t : timespent) {
                String[] arr = t.split(":");
                h = h + Integer.parseInt(arr[0]);
                m = m + Integer.parseInt(arr[1]);
            }
            double mf = m;
            double hf = h;
            mf = mf / timespent.size();
            mf = mf + ((hf % timespent.size()) / timespent.size() * 60.0);
            h = (h / timespent.size());
            m = (int) mf;
            while (m > 59) {
                h++;
                m = m - 60;
            }
            avgTimeSpentView.setText("" + ((h < 10) ? "0" + h : h) + ":" + ((m < 10) ? "0" + m : m));
        }
    }

    /**
     * validate the new record details
     *
     * @return if anything is invalid return null, else new record to be
     * inserted in database
     */
    public Record checkAddDetails() {
        String date = ((String) addDate.getText()).trim();
        if (date == null || date.equalsIgnoreCase("")) {
            JOptionPane.showMessageDialog(this, "Enter Date",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        } else if (!date.matches("(\\d)*(-)(\\d)*(-)(\\d)*")) {
            JOptionPane.showMessageDialog(this, "Enter date in mm-dd-yyyy format",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        } else if (date.matches("(\\d)*(-)(\\d)*(-)(\\d)*")) {
            String[] arr = date.split("-");
            int m = Integer.parseInt(arr[0]);
            int d = Integer.parseInt(arr[1]);
            int y = Integer.parseInt(arr[2]);
            if (m < 1 || m > 12 || d < 1 || d > 31) {
                JOptionPane.showMessageDialog(this, "Date is not correct",
                        "Error!", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        String drivername = ((String) addDriverNameCB.getSelectedItem()).trim();
        if (drivername == null || drivername.equalsIgnoreCase("")) {
            JOptionPane.showMessageDialog(this, "Select Driver",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        String startlocation = ((String) addStartLocationCB.getSelectedItem()).trim();
        if (startlocation == null || startlocation.equalsIgnoreCase("")) {
            JOptionPane.showMessageDialog(this, "Select Start Location",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        String stime = ((String) addStartTime.getText()).trim();
        if (stime == null || stime.equalsIgnoreCase("")) {
            JOptionPane.showMessageDialog(this, "Enter Start Time",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        } else if (!stime.matches("(\\d)*(:)(\\d)*")) {
            JOptionPane.showMessageDialog(this, "Enter Start time in hh:mm format",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        } else if (stime.matches("(\\d)*(:)(\\d)*")) {
            String[] arr = stime.split(":");
            int h = Integer.parseInt(arr[0]);
            int m = Integer.parseInt(arr[1]);
            if (m < 0 || m > 59 || h < 0 || h > 23) {
                JOptionPane.showMessageDialog(this, "Start Time is not correct",
                        "Error!", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        String gtime = ((String) addGateInTime.getText()).trim();
        if (!(gtime == null || gtime.equalsIgnoreCase("")
                || gtime.matches("( )*") || gtime.matches("(\t)*") || gtime.matches("(\\d)*(:)(\\d)*"))) {
            JOptionPane.showMessageDialog(this, "Enter GateIn time in hh:mm format or Leave blank",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        } else if (gtime.matches("(\\d)*(:)(\\d)*")) {
            String[] arr = gtime.split(":");
            int h = Integer.parseInt(arr[0]);
            int m = Integer.parseInt(arr[1]);
            if (m < 0 || m > 59 || h < 0 || h > 23) {
                JOptionPane.showMessageDialog(this, "Start Time is not correct",
                        "Error!", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        String endlocation = ((String) addEndLocationCB.getSelectedItem()).trim();
        if (endlocation == null || endlocation.equalsIgnoreCase("")) {
            JOptionPane.showMessageDialog(this, "Select End Location",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        String etime = ((String) addEndTime.getText()).trim();
        if (etime == null || etime.equalsIgnoreCase("")) {
            JOptionPane.showMessageDialog(this, "Enter End Time",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        } else if (!etime.matches("(\\d)*(:)(\\d)*")) {
            JOptionPane.showMessageDialog(this, "Enter End time in hh:mm format",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        } else if (etime.matches("(\\d)*(:)(\\d)*")) {
            String[] arr = etime.split(":");
            int h = Integer.parseInt(arr[0]);
            int m = Integer.parseInt(arr[1]);
            if (m < 0 || m > 59 || h < 0 || h > 23) {
                JOptionPane.showMessageDialog(this, "End Time is not correct",
                        "Error!", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        if (startlocation.equalsIgnoreCase(endlocation) && (gtime.matches("") || gtime.matches("( )*")
                || gtime.matches("(\t)*"))) {
            gtime = stime;
        }
        String desc = ((String) addDescription.getText()).trim();
        Document doc = new Document().append(Constants.trip_id, Integer.parseInt(getTripID(date)))
                .append("truck_id", getMacId(drivername));
        trips = tripsCollection.find(doc).sort(new Document("id", 1));
        boolean start = false;
        ArrayList<Point> points = new ArrayList<>();
        for (Document d : trips) {
            String time = Document.parse(d.getString("time")).getString("time").trim();
            if (start) {
                if (!compareTime(etime, time, 1)) {
//                    System.out.println("" + time);
                    points.add(new Point(d.getDouble("latitude"), d.getDouble("longitude")));
                    break;
                }
//                System.out.println("" + time);
                points.add(new Point(d.getDouble("latitude"), d.getDouble("longitude")));
            } else if (compareTime(stime, time, 0)) {
                start = true;
                points.add(new Point(d.getDouble("latitude"), d.getDouble("longitude")));
            }
        }
        return (new Record(date, drivername, startlocation, stime, gtime, endlocation, etime, desc, points,
                addErrorCB.isSelected()));
    }

    /**
     * validate the edited record details
     *
     * @return if anything is invalid return null, else edited record to be
     * updated in database
     */
    public Record checkEditDetails() {
        String date = ((String) editDate.getText()).trim();
        if (date == null || date.equalsIgnoreCase("")) {
            JOptionPane.showMessageDialog(this, "Enter Date",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        } else if (!date.matches("(\\d)*(-)(\\d)*(-)(\\d)*")) {
            JOptionPane.showMessageDialog(this, "Enter date in mm-dd-yyyy format",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        } else if (date.matches("(\\d)*(-)(\\d)*(-)(\\d)*")) {
            String[] arr = date.split("-");
            int m = Integer.parseInt(arr[0]);
            int d = Integer.parseInt(arr[1]);
            int y = Integer.parseInt(arr[2]);
            if (m < 1 || m > 12 || d < 1 || d > 31) {
                JOptionPane.showMessageDialog(this, "Date is not correct",
                        "Error!", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        String drivername = ((String) editDriverNameCB.getSelectedItem()).trim();
        if (drivername == null || drivername.equalsIgnoreCase("")) {
            JOptionPane.showMessageDialog(this, "Select Driver",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        String startlocation = ((String) editStartLocationCB.getSelectedItem()).trim();
        if (startlocation == null || startlocation.equalsIgnoreCase("")) {
            JOptionPane.showMessageDialog(this, "Select Start Location",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        String stime = ((String) editStartTime.getText()).trim();
        if (stime == null || stime.equalsIgnoreCase("")) {
            JOptionPane.showMessageDialog(this, "Enter Start Time",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        } else if (!stime.matches("(\\d)*(:)(\\d)*")) {
            JOptionPane.showMessageDialog(this, "Enter Start time in hh:mm format",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        } else if (stime.matches("(\\d)*(:)(\\d)*")) {
            String[] arr = stime.split(":");
            int h = Integer.parseInt(arr[0]);
            int m = Integer.parseInt(arr[1]);
            if (m < 0 || m > 59 || h < 0 || h > 23) {
                JOptionPane.showMessageDialog(this, "Start Time is not correct",
                        "Error!", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        String gtime = ((String) editGateInTime.getText()).trim();
        if (!(gtime == null || gtime.equalsIgnoreCase("")
                || gtime.matches("( )*") || gtime.matches("(\t)*") || gtime.matches("(\\d)*(:)(\\d)*"))) {
            JOptionPane.showMessageDialog(this, "Enter GateIn time in hh:mm format or Leave blank",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        } else if (gtime.matches("(\\d)*(:)(\\d)*")) {
            String[] arr = gtime.split(":");
            int h = Integer.parseInt(arr[0]);
            int m = Integer.parseInt(arr[1]);
            if (m < 0 || m > 59 || h < 0 || h > 23) {
                JOptionPane.showMessageDialog(this, "Start Time is not correct",
                        "Error!", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        String endlocation = ((String) editEndLocationCB.getSelectedItem()).trim();
        if (endlocation == null || endlocation.equalsIgnoreCase("")) {
            JOptionPane.showMessageDialog(this, "Select End Location",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        String etime = ((String) editEndTime.getText()).trim();
        if (etime == null || etime.equalsIgnoreCase("")) {
            JOptionPane.showMessageDialog(this, "Enter End Time",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        } else if (!etime.matches("(\\d)*(:)(\\d)*")) {
            JOptionPane.showMessageDialog(this, "Enter End time in hh:mm format",
                    "Error!", JOptionPane.ERROR_MESSAGE);
            return null;
        } else if (etime.matches("(\\d)*(:)(\\d)*")) {
            String[] arr = etime.split(":");
            int h = Integer.parseInt(arr[0]);
            int m = Integer.parseInt(arr[1]);
            if (m < 0 || m > 59 || h < 0 || h > 23) {
                JOptionPane.showMessageDialog(this, "End Time is not correct",
                        "Error!", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        if (startlocation.equalsIgnoreCase(endlocation) && (gtime.matches("") || gtime.matches("( )*")
                || gtime.matches("(\t)*"))) {
            gtime = stime;
        }
        String desc = ((String) editDescriptionTxt.getText()).trim();
        Document doc = new Document().append(Constants.trip_id, Integer.parseInt(getTripID(date)))
                .append("truck_id", getMacId(drivername));
        trips = tripsCollection.find(doc).sort(new Document("id", 1));
        boolean start = false;
        ArrayList<Point> points = new ArrayList<>();
        for (Document d : trips) {
            String time = Document.parse(d.getString("time")).getString("time").trim();
            if (start) {
                if (!compareTime(etime, time, 1)) {
//                    System.out.println("" + time);
                    points.add(new Point(d.getDouble("latitude"), d.getDouble("longitude")));
                    break;
                }
//                System.out.println("" + time);
                points.add(new Point(d.getDouble("latitude"), d.getDouble("longitude")));
            } else if (compareTime(stime, time, 0)) {
                start = true;
                points.add(new Point(d.getDouble("latitude"), d.getDouble("longitude")));
            }
        }
        return (new Record(date, drivername, startlocation, stime, gtime, endlocation, etime, desc, points,
                editErrorCB.isSelected()));
    }

    /**
     * generate list of records from the documents fetched from database
     */
    public void generateReportList() {
        reportList = new ArrayList<>();
        for (Document r : reports) {
            String trip_id = (String) r.get(Constants.trip_id);
            String macid = (String) r.get(Constants.macid);
            String startlocation = (String) r.get(Constants.startlocation);
            String starttime = (String) r.get(Constants.starttime);
            String gateintime = (String) r.get(Constants.gateintime);
            String endlocation = (String) r.get(Constants.endlocation);
            String endtime = (String) r.get(Constants.endtime);
            String distance = r.getString(Constants.distance);
            String description = (String) r.get(Constants.description);
            Boolean error = (Boolean) r.get(Constants.error);
            reportList.add(new Record(trip_id, macid, startlocation, starttime, gateintime,
                    endlocation, endtime, description, distance, error));
        }
    }

    /**
     * custom JTable model
     */
    class TableModel extends AbstractTableModel {

        private final String[] columnNames = {"Date", "Name", "Start",
            "End", "StartTime", "EndTime", "Time Spent", "Wait Time", "Distance", "Error", "Description"};
        private final Class[] columnClass = new Class[]{Integer.class, String.class, String.class,
            String.class, String.class, String.class, String.class, String.class,
            String.class, Boolean.class, String.class};

        /**
         * custom JTable model
         */
        public TableModel() {
            generateReportList();
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column]; //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnClass[columnIndex];
        }

        @Override
        public int getRowCount() {
            return reportList.size();
        }

        @Override
        public int getColumnCount() {
            return columnClass.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Record row = reportList.get(rowIndex);
            switch (columnIndex) {
                case Constants.tDate:
                    return row.getDate();
                case Constants.tName:
                    return row.getName();
                case Constants.tStart:
                    return row.getStartlocation();
                case Constants.tEnd:
                    return row.getEndlocation();
                case Constants.tStartTime:
                    return row.getStartTime();
                case Constants.tEndTime:
                    return row.getEndTime();
                case Constants.tTimeSpent:
                    return row.getTimeSpentAsString();
                case Constants.tWaitTime:
                    return row.getWaitTimeAsString();
                case Constants.tDistance:
                    return row.getDistance();
                case Constants.tError:
                    return row.isError();
                case Constants.tDescription:
                    return row.getDescription();
                default:
                    return "-";
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (rowIndex < reportList.size()) { // update error checkbox
                reportList.get(rowIndex).setError((boolean) aValue);
                int i = 0;
                for (Document r : reports) {
                    if (i == rowIndex) {
                        reportsCollection.updateOne(new Document(Constants._id, r.get(Constants._id)),
                                new Document("$set", new Document()
                                        .append(Constants.error, reportList.get(rowIndex).isError())));
                        break;
                    }
                    i++;
                }
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return (columnIndex == Constants.tError);   // only error column is editable in table view
        }
    }

    public static void main(String[] args) {
        // pre sort the statically assigned values
        Arrays.sort(Constants.locations, 1, Constants.locations.length);
        Arrays.sort(Constants.drivers, 1, Constants.drivers.length);

        // start the Application
        new TruckTrackingAnalysis();
    }

    /**
     * calculate the distance for list of geographical points
     *
     * @param list list of lat,long points
     * @return distance as string
     */
    private static String calculateDistance(ArrayList<Point> list) {
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
        String[] split = (dist + "0000").split("\\.");
        return ("" + split[0] + "." + split[1].substring(0, 2));
    }

    /**
     * calculate distance between two given point
     *
     * @param lat1 point 1 latitude
     * @param lon1 point 1 longitude
     * @param lat2 point 2 latitude
     * @param lon2 point 2 longitude
     * @param unit miles, kilometers, nautical miles
     * @return distance between two points
     */
    private static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(Math.min(dist, 1.000));
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if ("K".equalsIgnoreCase(unit)) {               // kilometer
            dist = dist * 1.609344;
        } else if ("N".equalsIgnoreCase(unit)) {        // nautical miles 
            dist = dist * 0.8684;
        }
        return (dist);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::  This function converts decimal degrees to radians             :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::  This function converts radians to decimal degrees             :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

    /**
     * @param str1 time from reports collection
     * @param str2 time from trips collection
     * @param key str1 is 0 - start time, 1 - end time
     * @return true if both times are same, else false
     */
    private static boolean compareTime(String str1, String str2, int key) {
        String[] split1 = str1.split(":");
        String[] split2 = str2.split(":");
        int h1 = Integer.parseInt(split1[0]);
        int m1 = Integer.parseInt(split1[1]);
        int h2 = Integer.parseInt(split2[0]);
        int m2 = Integer.parseInt(split2[1]);
        return (key == 0) ? ((h1 <= h2) && (m1 <= m2)) : ((h1 > h2) || ((h1 == h2) && (m1 > m2)));
    }
}
