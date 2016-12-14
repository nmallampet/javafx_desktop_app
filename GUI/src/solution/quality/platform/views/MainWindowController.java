/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.views;

import com.sun.javafx.scene.control.skin.ListViewSkin;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import solution.quality.platform.SystemsQualityPlatform;
import solution.quality.platform.dataStructures.casesStruct;
import solution.quality.platform.dataStructures.suiteCellStruct;
import solution.quality.platform.runFlow.runFlowSuite;
import solution.quality.platform.saveXML.sqpXML;
import solution.quality.platform.saveXML.xmlController;
import solution.quality.platform.utils;

/**
 * FXML Controller class
 *
 * @author nmallam1
 */
public class MainWindowController implements Initializable {

    @FXML MenuBar menuBar;
    
    @FXML Label suiteNameLabel;
    @FXML Label testCasesLabel;
    
    @FXML ListView suiteListView;
    @FXML ListView testCaseListView;
    
    @FXML TabPane testCaseTabPane;
    @FXML Tab PropTab;
    @FXML Tab ResultsTab;
    
    @FXML ToolBar bottomBar;
    
    @FXML AnchorPane propAP;
    @FXML AnchorPane resAP;
    
    @FXML Button globalEDAB;
    
    
    MenuItem fileSave;
    Menu menuAction;
    Menu menuGenerate;
    
    final private int INIT_LAYOUT_Y = 55;
    final private int MAIN_CELL_SIZE = 50;
    final private int SUB_CELL_SIZE = 35;
    
    
    private String LOCATION;
    private sqpXML PROJMETA;
    
    
    private xmlController xmlMVC;
    
    private suiteCellStruct cellViewDS;
    private ArrayList<casesStruct> suiteList;
    
    
    private boolean hasEDAConfig = false;
    private String edaDefault = null;
    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        initmenuBar();
        initVisibility();
        setDisableTop(true);
        xmlMVC = null;
        cellViewDS = new suiteCellStruct(suiteListView, testCaseListView);
        suiteList = new ArrayList<>();
        testCaseTabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            @Override
            public void changed(ObservableValue<? extends Tab> observable, Tab oldTab, Tab newTab) {
                if(newTab == ResultsTab) {ResultTabOnClick();}
                   
            }
        });
        
//        Timeline service = new Timeline(new KeyFrame(Duration.seconds(5), (ActionEvent event) -> {
//            setandgetStatus();
//        }));
//        service.setCycleCount(Timeline.INDEFINITE);
//        service.play();


        //debugOpen();
        
        //testcomment
        
        
    }    
    
    
    private void setDisableTop(boolean bool) {
        fileSave.setDisable(bool);
        menuAction.setDisable(bool);
        menuGenerate.setDisable(bool);
        globalEDAB.setDisable(bool);
    }
    
    
    public void setandgetStatus() {
        
        int suiteIndex = suiteListView.getSelectionModel().getSelectedIndex();
        int tcIndex = testCaseListView.getSelectionModel().getSelectedIndex();
        suiteListView.getSelectionModel().clearSelection();
        testCaseListView.getSelectionModel().clearSelection();


//        System.out.println("INDEX:" + suiteIndex + "," + tcIndex);
//        if(PROJMETA != null) {
//            List<String> suiteStatusList = utils.getSuiteStatusList(PROJMETA, LOCATION);
//            //suiteListView.setItems(null);
//            testCaseListView.setItems(null);
//            this.cellViewDS.setSuiteDataP(suiteListView, suiteStatusList);
//            
//            System.out.println("HI");
//            suiteListView.refresh();
//        }
//        
//        if(suiteIndex != -1) {
//            suiteListView.getSelectionModel().select(suiteIndex);
//            if(tcIndex != -1)
//                testCaseListView.getSelectionModel().select(tcIndex);
//        }

        
        if(suiteIndex >= 0 && suiteIndex < suiteListView.getItems().size()) {
           // suiteListView.setUserData(suiteListView.getItems().get(suiteIndex));
            List<String> suiteStatusList = utils.getSuiteStatusList(PROJMETA, LOCATION);
            suiteListView.setUserData(suiteStatusList);
            ObservableList<String> i = FXCollections.observableList(suiteStatusList);
            suiteListView.setItems(null);
            suiteListView.setItems(i);
//            
            
            //this.cellViewDS.setSuiteDataP(suiteListView, suiteStatusList);
//            UpdateableListViewSkin<String> skin = new UpdateableListViewSkin(suiteListView);
//            suiteListView.setSkin(skin);
//            ((UpdateableListViewSkin) suiteListView.getSkin()).refresh();

//            if(tcIndex >= 0  && tcIndex < testCaseListView.getItems().size()) {
//                testCaseListView.setUserData(testCaseListView.getItems().get(tcIndex));
//                ArrayList<String> suites = new ArrayList<>(suiteListView.getItems());
//                for(String s: suites)
//                    System.out.println("SUITES:" + s);
//                for(String data: suites) {
//                    List<String> statusTCSList = utils.getTCStatusList(data, PROJMETA, LOCATION);
//                    for(String s: statusTCSList)
//                        System.out.println("TCS:" + s);
//                    this.cellViewDS.setTCData(testCaseListView, statusTCSList);
//                }
//            }
        }
        
    }
    
    
    private void debugOpen() {
        File file = new File("C:\\Users\\nmallam1\\Desktop\\sampleOut\\test1\\test1.sqp");
        this.LOCATION = file.getParent();
        xmlMVC = new xmlController(this.LOCATION);
        System.out.println("File Path: " + this.LOCATION);
        loadFile(file);
        //this.cellViewDS.setSuiteData(suiteListView, PROJMETA);
    }
    
    private void initmenuBar() {
        initmenuBarFile();
        initmenuBarAction();
        initmenuBarGenerate();
    }
    
    private void initVisibility() {
        suiteNameLabel.setVisible(false);
        testCasesLabel.setVisible(false);
        
        //suiteListView.setVisible(false);
        //testCaseListView.setVisible(false);
        
        testCaseTabPane.setVisible(false);
    }
    
    private void saveFile() {
        if(this.xmlMVC != null) {
            this.PROJMETA.setModDate(utils.getCurrentDate());
            this.xmlMVC.setPROJMETA(PROJMETA);
            this.xmlMVC.saveFile();
        }
    }
    
    private void loadFile(File file) {
        this.xmlMVC.loadFile(file);
        PROJMETA = this.xmlMVC.getPROJMETA();
        ArrayList<casesStruct> t = new ArrayList<>(this.PROJMETA.getSuites());
        this.suiteList = t;
        List<String> suiteStatusList = utils.getSuiteStatusList(PROJMETA, LOCATION);
        this.cellViewDS.setSuiteData(suiteListView, suiteStatusList);
        String tEDA = PROJMETA.getDefEDA();
        if(tEDA != null) {
            this.hasEDAConfig = true;
            this.edaDefault = tEDA;
        }
        setDisableTop(false);
        System.out.println("Loaded project: " + PROJMETA.getName());
    }
    
    private void newProject() {
        final Stage popupStage = new Stage(StageStyle.UTILITY);    
        FXMLLoader loader = new FXMLLoader(SystemsQualityPlatform.class.getResource("views/newProject.fxml"));
        AnchorPane rootLayout = null;
        try {
            rootLayout = (AnchorPane) loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (rootLayout != null) {
            Scene scene = new Scene(rootLayout); 
            popupStage.setScene(scene);
            popupStage.setTitle("Create New Project");
            popupStage.show();

            popupStage.setOnHidden((WindowEvent we) -> {
                NewProjectController controller = loader.<NewProjectController>getController();
                this.PROJMETA = controller.getProjMeta();
                this.LOCATION = controller.getProjLoc() + utils.SEPARATOR + PROJMETA.getName();
                xmlMVC = new xmlController(this.LOCATION);
                xmlMVC.setPROJMETA(PROJMETA);
                xmlMVC.saveFile();
                setDisableTop(false);
            });

        }
    }
    
    private void addSuite() {
        final Stage popupStage = new Stage(StageStyle.UTILITY);    
        FXMLLoader loader = new FXMLLoader(SystemsQualityPlatform.class.getResource("views/addSuite.fxml"));
        AnchorPane rootLayout = null;
        try {
            rootLayout = (AnchorPane) loader.load();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        final AddSuiteController controller = loader.<AddSuiteController>getController();
        controller.init(LOCATION, this.hasEDAConfig, this.edaDefault);
        if (rootLayout != null) {
            Scene scene = new Scene(rootLayout); 
            popupStage.setScene(scene);
            popupStage.setTitle("Add Suite");
            popupStage.show();

            popupStage.setOnHidden((WindowEvent we) -> {
                casesStruct c = controller.getC();
                if(c != null) {
                    suiteList.add(c);
                    this.PROJMETA.setSuites(suiteList);
                    this.xmlMVC.setPROJMETA(PROJMETA);
                    List<String> suiteStatusList = utils.getSuiteStatusList(PROJMETA, LOCATION);
                    this.cellViewDS.setSuiteData(suiteListView, suiteStatusList);
                    saveFile();
                }
            });
        }
    }
    
    private String loadData(String path, String key) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(path)));
            for(String l = br.readLine(); l!= null; l = br.readLine()){
                if(l.contains(key)) {
                    return utils.getValueFromDataFile(l);
                }
            }
            br.close();
        }
        catch(Exception e) {            
        }
        return null;
    }
    
    private void runSuite() {
         if(!suiteListView.getSelectionModel().isEmpty()) {
             String suiteName1 = (String) suiteListView.getSelectionModel().getSelectedItem();
             String suiteName  = suiteName1.split(utils.LISTVIEW_SEP_KEY)[0];
             String suiteLoc = this.LOCATION + utils.SEPARATOR + "suites" + utils.SEPARATOR + suiteName;
             ArrayList<String> directories1 = utils.getListofTCsFromSuite(suiteName, PROJMETA);
             String[] tcs = directories1.parallelStream().toArray(String[]::new);
             //String [] tcs = utils.getListofFiles(new File(suiteLoc));
             ArrayList<String> TCLOCS = new ArrayList<>();
             ArrayList<String> LDFLOCS = new ArrayList<>();
             ArrayList<String> EDAS = new ArrayList<>();
             ArrayList<String> CONFLOCS = new ArrayList<>();
             boolean incompleteFlag = false;
             for(String s: tcs) {
                 String TCLOC = suiteLoc + utils.SEPARATOR + s;
                 String dataPath = TCLOC + utils.SEPARATOR + "sqpData" + utils.SEPARATOR + "data.txt";
                 String LDFLOC = loadData(dataPath, "ldf");
                 String EDA = loadData(dataPath, "eda");
                 String CONFLOC = loadData(dataPath, "conf");
                 if(LDFLOC == null || EDA == null || CONFLOC == null) {
                     incompleteFlag = true;
                     break;
                 }
                 TCLOCS.add(TCLOC);
                 LDFLOCS.add(LDFLOC);
                 EDAS.add(EDA);
                 CONFLOCS.add(CONFLOC);
             }
             if(incompleteFlag) {
                 Platform.runLater(new Runnable() {
                     @Override
                     public void run() {
                        utils.showErrorAlert("Error Running Suite: " + suiteName, "Missing Information", "Please make sure that each testcase's LDF, CONF, and EDA field are filled"); 
                     }
                 });
             }
             else {
                runFlowSuite rFS = new runFlowSuite(TCLOCS, LDFLOCS, EDAS, CONFLOCS);
                rFS.run(tcs); 
             }
         }
    }
    
    private void runSuiteAll() {
        ArrayList<String> suiteNames = new ArrayList<>(suiteListView.getItems());   
        for(String suiteName: suiteNames) {
            String sName = suiteName.split(utils.LISTVIEW_SEP_KEY)[0];
            String suiteLoc = this.LOCATION + utils.SEPARATOR + "suites" + utils.SEPARATOR + sName;
            //String [] tcs = utils.getListofFiles(new File(suiteLoc));
            ArrayList<String> directories1 = utils.getListofTCsFromSuite(sName, PROJMETA);
            String[] tcs = directories1.parallelStream().toArray(String[]::new);
            ArrayList<String> TCLOCS = new ArrayList<>();
            ArrayList<String> LDFLOCS = new ArrayList<>();
            ArrayList<String> EDAS = new ArrayList<>();
            ArrayList<String> CONFLOCS = new ArrayList<>();
            boolean incompleteFlag = false;
            String errorSN = null;
            for(String s: tcs) {
                String TCLOC = suiteLoc + utils.SEPARATOR + s;
                String dataPath = TCLOC + utils.SEPARATOR + "sqpData" + utils.SEPARATOR + "data.txt";
                String LDFLOC = loadData(dataPath, "ldf");
                String EDA = loadData(dataPath, "eda");
                String CONFLOC = loadData(dataPath, "conf");
                if(LDFLOC == null || EDA == null || CONFLOC == null) {
                     incompleteFlag = true;
                     errorSN = suiteName;
                     break;
                 }
                TCLOCS.add(TCLOC);
                LDFLOCS.add(LDFLOC);
                EDAS.add(EDA);
                CONFLOCS.add(CONFLOC);
            }
            if(incompleteFlag) {
                final String t = errorSN;
                Platform.runLater(new Runnable() {
                     @Override
                     public void run() {
                        utils.showErrorAlert("Error Running Suite: " + t, "Missing Information", "Please make sure that each testcase's LDF, CONF, and EDA field are filled"); 
                     }
                });
                break;
            }
            else {
                runFlowSuite rFS = new runFlowSuite(TCLOCS, LDFLOCS, EDAS, CONFLOCS);
                rFS.run(tcs);
            }
        }
    }
    
    private void initmenuBarFile() {
        menuBar.setPadding(new Insets(0,0,0,7));
        Menu menuFile = new Menu("File");
        menuBar.getMenus().addAll(menuFile);
        
        
        //New
        MenuItem fileNew = new MenuItem("New Project");
        fileNew.setAccelerator(KeyCombination.keyCombination("Ctrl+N"));
        fileNew.setOnAction((ActionEvent event) -> {
            newProject();
        });
        
        //Open
        MenuItem fileOpen = new MenuItem("Open Project");
        fileOpen.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
        fileOpen.setOnAction((ActionEvent event) -> {
            Stage node = (Stage) menuBar.getScene().getWindow();
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Project");
            String initDir = System.getProperty("user.dir");
            chooser.setInitialDirectory(new File(initDir));
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Projects", "*.sqp"));
            File file = chooser.showOpenDialog(node.getScene().getWindow());
            
            if(file != null) {
                //this.LOCATION = file.getAbsolutePath();
                this.LOCATION = file.getParent();
                xmlMVC = new xmlController(this.LOCATION);
                loadFile(file);
                
            }
        });
        
        //save
        this.fileSave = new MenuItem("Save");
        fileSave.setAccelerator(KeyCombination.keyCombination("Ctrl+S"));
        fileSave.setOnAction((ActionEvent event) -> {
            saveFile();
        });
        
        //Exit
        MenuItem fileExit = new MenuItem("Exit");
        fileExit.setAccelerator(KeyCombination.keyCombination("Ctrl+Q"));
        fileExit.setOnAction((ActionEvent event) -> {
            boolean flag = utils.promptYesOrNo("Saving before exiting", "Don't forget to save!", "Would you like to save before exiting?");
            if(flag) 
                saveFile();
            
            Platform.exit();
        });
        
        //add too subMenu
        menuFile.getItems().addAll(fileNew, fileOpen, fileSave, fileExit);
        
    }
    
    private void initmenuBarAction() {
        this.menuAction = new Menu("Action");
        menuBar.getMenus().addAll(menuAction);
        
        //Add Suite
        MenuItem AAdd = new MenuItem("Add Suite");
        AAdd.setAccelerator(KeyCombination.keyCombination("Ctrl+T"));
        AAdd.setOnAction((ActionEvent event) -> {
            addSuite();
        });
        
        
        //delete suite
        MenuItem ADel = new MenuItem("Delete Suite");
        ADel.setAccelerator(KeyCombination.keyCombination("Delete"));
        ADel.setOnAction((ActionEvent event) -> {
            if(!suiteListView.getSelectionModel().isEmpty()) {
                String data = (String) suiteListView.getSelectionModel().getSelectedItem();
                String sName = data.split(utils.LISTVIEW_SEP_KEY)[0];
                
                String path = this.LOCATION + utils.SEPARATOR + "suites" + utils.SEPARATOR + sName;
                Platform.runLater(new Runnable() {
                     @Override
                     public void run() {
                        boolean flag1 = utils.promptYesOrNo("Deleting Suite: " + sName, "Are you sure you want to Delete?", "Are you sure you want to delete suite " + sName + "?");
                        if(flag1) {
                            boolean flag = utils.deleteDirectory(new File(path));
                            if(flag) {
                                for(casesStruct c: suiteList) {
                                    if(c.getSuiteName().equals(sName)) {
                                        suiteListView.getItems().remove(sName);
                                        suiteList.remove(c);
                                        break;
                                    }
                                }
                                testCaseListView.setItems(null);
                                PROJMETA.setSuites(suiteList);
                                List<String> suiteStatusList = utils.getSuiteStatusList(PROJMETA, LOCATION);
                                cellViewDS.setSuiteData(suiteListView, suiteStatusList);  
                                saveFile();
                            }
                            else
                                utils.showErrorAlert("Error", "Failed to Delete Directory", "Directory failed to get deleted, please try again in a few mins");
                        }
                     }
                 });
            }
        });
        
        MenuItem ARun = new MenuItem("Run Suite(s)");
        ARun.setAccelerator(KeyCombination.keyCombination("Ctrl+R"));
        ARun.setOnAction((ActionEvent event) -> {
            Thread t1 = new Thread(new Runnable() {
                @Override
                public void run() {
                    runSuite();
                }
            });
            t1.start();
        });
        
        MenuItem ARunAll = new MenuItem("Run All Suites");
        ARunAll.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+R"));
        ARunAll.setOnAction((ActionEvent event) -> {
            Thread t1 = new Thread(new Runnable() {
                @Override
                public void run() {
                    runSuiteAll();
                }
            });
            t1.start();
        });
        
        MenuItem AMoveUp = new MenuItem("Move Up");
        AMoveUp.setAccelerator(KeyCombination.keyCombination("F3"));
        AMoveUp.setOnAction((ActionEvent event) -> {
            System.out.println("MOVING UP");
            dragTCcell_UP();
            saveFile();
        });
        
        MenuItem AMoveDown = new MenuItem("Move Down");
        AMoveDown.setAccelerator(KeyCombination.keyCombination("F4"));
        AMoveDown.setOnAction((ActionEvent event) -> {
            System.out.println("MOVING DOWN");
            dragTCcell_DOWN();
            saveFile();
        });
        
        MenuItem ARefresh = new MenuItem("Refresh");
        ARefresh.setAccelerator(KeyCombination.keyCombination("F5"));
        ARefresh.setOnAction((ActionEvent event) -> {
            setandgetStatus();
//            Thread t1 = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    setandgetStatus();
//                }
//            });
//            t1.start();
        });
        
        menuAction.getItems().addAll(AAdd, ADel, new SeparatorMenuItem(), ARun, ARunAll, new SeparatorMenuItem(), AMoveUp, AMoveDown, new SeparatorMenuItem(), ARefresh);
        
    }
    
    private void initmenuBarGenerate() {
        this.menuGenerate = new Menu("Generate");
        menuBar.getMenus().addAll(menuGenerate);
        
        MenuItem GGen1 = new MenuItem("Generate Input File");
        GGen1.setAccelerator(KeyCombination.keyCombination("Ctrl+G"));
        GGen1.setOnAction((ActionEvent event) -> {
            if(!suiteListView.getSelectionModel().isEmpty()) {
                ArrayList<String> selectedSuiteNames = new ArrayList<>(suiteListView.getSelectionModel().getSelectedItems());
                ArrayList<String> _selectedSuiteNames = new ArrayList<>();
                for(String s: selectedSuiteNames) {
                    String sName = s.split(utils.LISTVIEW_SEP_KEY)[0];
                    _selectedSuiteNames.add(sName);
                }
                utils.generateInputFile(_selectedSuiteNames, LOCATION, PROJMETA);
            }
        });
        
        MenuItem GGen2 = new MenuItem("Generate Input File (All)");
        GGen2.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+G"));
        GGen2.setOnAction((ActionEvent event) -> {
            ArrayList<String> suiteNames = new ArrayList<>(suiteListView.getItems());
            ArrayList<String> _selectedSuiteNames = new ArrayList<>();
                for(String s: suiteNames) {
                    String sName = s.split(utils.LISTVIEW_SEP_KEY)[0];
                    _selectedSuiteNames.add(sName);
                }
            utils.generateInputFile(_selectedSuiteNames, LOCATION, PROJMETA);
        });
        
        
        
        menuGenerate.getItems().addAll(GGen1, GGen2);
    }
 
    
    @FXML
    public void SuiteCellOnClick (MouseEvent e) {
        if(!suiteListView.getSelectionModel().isEmpty()) {
            String data = (String) suiteListView.getSelectionModel().getSelectedItem();
            int index = suiteListView.getSelectionModel().getSelectedIndex();
            
            //get initial starting position
            int layoutY = INIT_LAYOUT_Y + (index * MAIN_CELL_SIZE);
            double _layoutY = (double)layoutY;
            testCaseListView.setLayoutY(_layoutY);

            List<String> statusTCSList = utils.getTCStatusList(data, PROJMETA, LOCATION);
            this.cellViewDS.setTCData(testCaseListView, statusTCSList);
            testCaseTabPane.setVisible(false);
        }
    }
    
    @FXML
    public void TestCaseCellOnClick(MouseEvent e) {
        String sName;
        String tcName;
        if(!suiteListView.getSelectionModel().isEmpty()) {
            String sName1 = (String) suiteListView.getSelectionModel().getSelectedItem();
            sName = sName1.split(utils.LISTVIEW_SEP_KEY)[0];
            if(!testCaseListView.getSelectionModel().isEmpty()) {
                String tcName1 = (String) testCaseListView.getSelectionModel().getSelectedItem();
                tcName = tcName1.split(utils.LISTVIEW_SEP_KEY)[0];
                String path = this.LOCATION + utils.SEPARATOR + "suites" + utils.SEPARATOR + sName + utils.SEPARATOR + tcName;
                //String [] directories = utils.getListofFiles(new File(this.LOCATION + utils.SEPARATOR + "suites" + utils.SEPARATOR + sName));
                ArrayList<String> directories1 = utils.getListofTCsFromSuite(sName, PROJMETA);
                String[] directories = directories1.parallelStream().toArray(String[]::new);
                //String[] directories = directories1.toArray(new String[directories1.size()]);
                FXMLLoader container = new FXMLLoader(SystemsQualityPlatform.class.getResource("views/propPane.fxml"));
                try {
                    testCaseTabPane.setVisible(true);
                    testCaseTabPane.getSelectionModel().select(PropTab);
                    AnchorPane prop = container.load();
                    propAP.getChildren().setAll(prop);
                    PropPaneController controller = container.<PropPaneController>getController();
                    controller.init(tcName, path, directories);
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }
    
    public void ResultTabOnClick() {
        String sName;
        String tcName;
        if(!suiteListView.getSelectionModel().isEmpty()) {
            String sName1 = (String) suiteListView.getSelectionModel().getSelectedItem();
            sName = sName1.split(utils.LISTVIEW_SEP_KEY)[0];
            if(!testCaseListView.getSelectionModel().isEmpty()) {
                String tcName1 = (String) testCaseListView.getSelectionModel().getSelectedItem();
                tcName = tcName1.split(utils.LISTVIEW_SEP_KEY)[0];
                String path = this.LOCATION + utils.SEPARATOR + "suites" + utils.SEPARATOR + sName + utils.SEPARATOR + tcName;
                //String [] directories = utils.getListofFiles(new File(this.LOCATION + utils.SEPARATOR + "suites" + utils.SEPARATOR + sName));
                ArrayList<String> directories = utils.getListofTCsFromSuite(sName, PROJMETA);
                FXMLLoader container = new FXMLLoader(SystemsQualityPlatform.class.getResource("views/resultsPane.fxml"));
                try {
                    AnchorPane prop = container.load();
                    resAP.getChildren().setAll(prop);
                    ResultsPaneController controller = container.<ResultsPaneController>getController();
                    controller.init(path, tcName, directories);
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }
    
    private void dragTCcell_UP() {
        String sName;
        if(!suiteListView.getSelectionModel().isEmpty()) {
            String sName1 = (String) suiteListView.getSelectionModel().getSelectedItem();
            sName = sName1.split(utils.LISTVIEW_SEP_KEY)[0];
            //ArrayList<String> old = new ArrayList<>(suiteListView.getItems());
            ArrayList<String> old = utils.getListofTCsFromSuite(sName, PROJMETA);
            if(!testCaseListView.getSelectionModel().isEmpty()) {
                int origIndex = testCaseListView.getSelectionModel().getSelectedIndex();
                if(origIndex > 0) {
                    int newIndex = origIndex--;
                    Collections.swap(old, origIndex, newIndex);
                    utils.setNewList(PROJMETA, old, sName);
                    List<String> statusTCSList = utils.getTCStatusList(sName1, PROJMETA, LOCATION);
                    this.cellViewDS.setTCData(testCaseListView, statusTCSList);
                }
                    
            }
        }
    }
    
    private void dragTCcell_DOWN() {
        String sName;
        if(!suiteListView.getSelectionModel().isEmpty()) {
            String sName1 = (String) suiteListView.getSelectionModel().getSelectedItem();
            sName = sName1.split(utils.LISTVIEW_SEP_KEY)[0];
            //ArrayList<String> old = new ArrayList<>(suiteListView.getItems());
            ArrayList<String> old = utils.getListofTCsFromSuite(sName, PROJMETA);
            if(!testCaseListView.getSelectionModel().isEmpty()) {
                int origIndex = testCaseListView.getSelectionModel().getSelectedIndex();
                if(origIndex < (old.size() - 1)) {
                    int newIndex = origIndex++;
                    Collections.swap(old, origIndex, newIndex);
                    utils.setNewList(PROJMETA, old, sName);
                    List<String> statusTCSList = utils.getTCStatusList(sName1, PROJMETA, LOCATION);
                    this.cellViewDS.setTCData(testCaseListView, statusTCSList);
                }
                    
            }
        }
    }
    
    
    private void setAllEDAconfig() {
        assert this.hasEDAConfig : "incorrect call to setAllEDAconfig()";
        assert this.suiteList != null: "suiteList is null when setting all default EDA";
        
        String path = this.LOCATION + utils.SEPARATOR + "suites";
        String edaData = "eda=" + this.edaDefault;
        for(casesStruct c: this.suiteList) {
            String sName = c.getSuiteName();
            String path1 = path + utils.SEPARATOR + sName;
            ArrayList<String> tcs = c.getTestCases();
            for(String s: tcs) {
                String path2 = path1 + utils.SEPARATOR + s + utils.SEPARATOR + "sqpData" + utils.SEPARATOR + "data.txt";
                utils.writeToSQPDataFile("eda", path2, edaData);
            }
        }
    }
    
    
    @FXML
    public void globalEDAclick(ActionEvent e) {
        
        List<String> choices = new ArrayList<>();
        choices.add("diamond");
        choices.add("icecube");
        Collection<String> collection = choices;
        ChoiceDialog<String> dialog = new ChoiceDialog<>(this.edaDefault, collection);
        
        dialog.setResultConverter( ( ButtonType type ) ->
        {
            ButtonBar.ButtonData data = type == null ? null : type.getButtonData();
            if (data == ButtonBar.ButtonData.LEFT )
                return "A@" + dialog.getSelectedItem();
            else if(data == ButtonBar.ButtonData.FINISH) 
                return "B@" + dialog.getSelectedItem();
            else if(data == ButtonBar.ButtonData.NO)
                return "C@C";
            else
                return null;
        } );
        
        dialog.setTitle("Global EDA");
        dialog.setHeaderText("Choose the default EDA\n'Set' will set the selected EDA as the default for newly added suites\n'Set All' will set the selected EDA as default for all suites");
        dialog.setContentText("Select EDA:");
        
        ButtonType setBT = new ButtonType("Set", ButtonData.LEFT);
        ButtonType setAllBT = new ButtonType("Set All", ButtonData.FINISH);
        ButtonType remSetBT = new ButtonType("Reset", ButtonData.NO);
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        
        dialog.getDialogPane().getButtonTypes().setAll(setBT, setAllBT, remSetBT, buttonTypeCancel);
        
        Optional<String> result = dialog.showAndWait();
        if ( result.isPresent() ) {
            String c = result.get().split("@")[0];
            String selC = result.get().split("@")[1];
            
            if(c.equals("A")) {
                System.out.println("SET " + selC);
                this.edaDefault = selC;
                this.hasEDAConfig = true;
            }
            else if(c.equals("B")) {
                System.out.println("SET ALL " + selC);
                this.edaDefault = selC;
                this.hasEDAConfig = true;
                setAllEDAconfig();
            }
            else {
                System.out.println("Reset");
                this.edaDefault = null;
                this.hasEDAConfig = false;
            }
            
            PROJMETA.setDefEDA(this.edaDefault);
            saveFile();
        }
        
    }
    
    static public class UpdateableListViewSkin<T> extends ListViewSkin<T> {
        public UpdateableListViewSkin(ListView<T> arg0) {
		super(arg0);
	}
	
	public void refresh() {
            super.flow.rebuildCells();
	}
	
	@SuppressWarnings("unchecked")
        static <T> UpdateableListViewSkin<T> cast(Object obj) {
            return (UpdateableListViewSkin<T>)obj;
        }
    }
    
    
}
