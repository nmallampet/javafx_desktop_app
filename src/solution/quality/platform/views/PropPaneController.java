/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.views;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import solution.quality.platform.SystemsQualityPlatform;
import solution.quality.platform.runFlow.runFlow;
import solution.quality.platform.runFlow.trunckSetupB;
import solution.quality.platform.utils;

/**
 * FXML Controller class
 *
 * @author nmallam1
 */
public class PropPaneController implements Initializable {

    @FXML Label tcNameL;
    
    @FXML Label statusL;
    
    @FXML TextField ldfFileTF;
    @FXML Button ldfBrowseB;
    
    @FXML TextField confFileTF;
    @FXML Button confBrowseB;
    @FXML Button confDefaultB;
    
    @FXML ComboBox edaCB;
    @FXML TextField edaTF;
    @FXML Button setEDAB;
    
    @FXML Button setB;
    
    @FXML Button runB;
    
    ObservableList<String> edaOptions = FXCollections.observableArrayList("diamond", "icecube");
    
    String TCLOCATION;
    String TCNAME;
    
    String initDesDir = null;
    String dataFile;
    
    String [] tcs;
    
    String LOAD_des = null;
    String LOAD_ldf = null;
    String LOAD_eda = null;
    String LOAD_conf = null;
    
    boolean hasEdaConf = true;
    boolean runFlag = false;
    boolean hasDaisy = false;
    boolean isFirstTC = false;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initVisibilityEDA(false);
        initVisibilitySet(false);
        edaCB.setItems(edaOptions);
        ldfFileTF.setEditable(false);
        confFileTF.setEditable(false);
        initRunVisibility();
        setVisibilityEDA();
        Timeline service = new Timeline(new KeyFrame(Duration.seconds(5), (ActionEvent event) -> {
            setStatusL();
        }));
        service.setCycleCount(Timeline.INDEFINITE);
        service.play();
    }    
    
    public void init(String name, String TCLOCATION, String [] tcs) {
        this.TCLOCATION = TCLOCATION;
        this.TCNAME = name;
        this.tcs = tcs;
        this.hasDaisy = this.tcs.length > 1;
        if(tcs[0].equals(name))
            this.isFirstTC = true;
        
        tcNameL.setText(this.TCNAME);
        getInitDesDir();
        loadData();
        checkStatus();
    }
    
    @FXML
    public void setVisibilityEDA() {
        String eda = (String) edaCB.getValue();
        if(eda != null) {
            String edaLoc = utils.getEDAEnvLocation(utils.getEnvFromEda(eda));
            String edaEntry = edaTF.getText();
            if(!edaLoc.equals(edaEntry)) {
                setEDAB.setVisible(true);
            }
            else {
                setEDAB.setVisible(false);
            }
        }
        else 
            setEDAB.setVisible(false);
    }
    
    private void setStatusLwithKey(String key) {
        switch(key) {
            case "RBT_A":
                statusL.setText("Running Bitgen");
                statusL.setTextFill(Color.ORANGE);
                break;
            case "RBT_B":
                statusL.setText("Bitgen Failed");
                statusL.setTextFill(Color.RED);
                break;
            case "RBT_C":
                statusL.setText("Bitgen Passed");
                statusL.setTextFill(Color.GREEN);
                break;
            case "XCF_A":
                statusL.setText("Running Programmer");
                statusL.setTextFill(Color.ORANGE);
                break;
            case "XCF_B":
                statusL.setText("Programmer Failed");
                statusL.setTextFill(Color.RED);
                break;
            case "XCF_C":
                statusL.setText("Programmer Passed");
                statusL.setTextFill(Color.GREEN);
                break;
            case "FTDI_A":
                statusL.setText("Sending Stimulus");
                statusL.setTextFill(Color.ORANGE);
                break;
            case "FTDI_B":
                statusL.setText("Sending Stimulus Failed");
                statusL.setTextFill(Color.RED);
                break;
            case "FTDI_C":
                statusL.setText("Sending Stimulus Passed");
                statusL.setTextFill(Color.GREEN);
                break;
                
        }
    }
    
    
    public void setStatusL() {
        String path = this.TCLOCATION + utils.SEPARATOR + "sqpData";
        String [] statusDataList = {"RBT_A", "RBT_B", "RBT_C", "XCF_A", "XCF_B", "XCF_C", "FTDI_A", "FTDI_B", "FTDI_C"};
        String key = null;
        for(String s: statusDataList) {
            String nPath = path + utils.SEPARATOR + s;
            if(utils.checkFileExists(new File(nPath))) {
                key = s;
                setStatusLwithKey(s);
                break;
            }
        }
        //return key;
    }
    
    
    private void initVisibilityEDA(boolean bool) {
        edaTF.setVisible(bool);
//        edaReloadB.setVisible(bool);
//        edaConfB.setVisible(bool);
    }
    
    private void initVisibilitySet(boolean bool) {
        setB.setVisible(bool);
    }
    
    private void initRunVisibility() {
        runB.setVisible(runFlag);
    }
    
    private void loadData() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(this.dataFile));
            for(String l = br.readLine(); l!= null; l = br.readLine()){
                if(l.contains("des")) {
                    this.LOAD_des = utils.getValueFromDataFile(l);
                    continue;
                }
                if(l.contains("ldf")) {
                    this.LOAD_ldf = utils.getValueFromDataFile(l);
                    continue;
                }
                if(l.contains("conf")) {
                    this.LOAD_conf = utils.getValueFromDataFile(l);
                    continue;
                }
                if(l.contains("eda")) 
                    this.LOAD_eda = utils.getValueFromDataFile(l);
                    
            }
            br.close();
        }
        catch(Exception e) {            
        }
        
        if(LOAD_ldf != null)
            ldfFileTF.setText(LOAD_ldf);
        if(LOAD_conf != null)
            confFileTF.setText(LOAD_conf);
        if(LOAD_eda != null) {
            edaCB.setValue(LOAD_eda);
            checkEDAfields();
        }
        
        checkAllFields();
        
    }
    
    @FXML
    public void checkEDAfields() {
        if(!edaCB.getValue().toString().isEmpty()) {
            String eda = (String) edaCB.getValue();
            String edaLoc = utils.getEDAEnvLocation(utils.getEnvFromEda(eda));
            initVisibilityEDA(true);
            edaTF.setText(edaLoc);
        }
        
        if(this.hasDaisy) {
            String suiteDir = new File(this.TCLOCATION).getParent();
            String edaData = "eda=" + (String) edaCB.getValue();
            for(String s: this.tcs) {
                String path = suiteDir + utils.SEPARATOR + s + utils.SEPARATOR + "sqpData" + utils.SEPARATOR + "data.txt";
                utils.writeToSQPDataFile("eda", path, edaData);
            }
        }
        
        
        checkAllFields();
    }
   
    
    @FXML
    public void setEdaButtonClick(ActionEvent e) {
    }
    
    
    private void checkAllFields() {
        boolean flag = false;
        
        if(!ldfFileTF.getText().isEmpty())
            flag = true;
        if(flag)
            flag = !confFileTF.getText().isEmpty();
        if(flag) {
            String a = (String) edaCB.getValue();
            if(a != null)
                flag = !a.isEmpty();
            else
                flag = false;
        }
        
        if(flag)
            initVisibilitySet(true);
    }
    
    
    private void getInitDesDir() {
        this.dataFile = this.TCLOCATION + utils.SEPARATOR + "sqpData" + utils.SEPARATOR + "data.txt";
        String pattern = "des=(.*)";
        Pattern p = Pattern.compile(pattern);
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(this.dataFile)));
            for(String l = br.readLine(); l!= null; l = br.readLine()){
                 Matcher m = p.matcher(l);
                 if(m.find())
                     this.initDesDir = m.group(1);
            }
        }
        catch (Exception e) {
            
        }
//        String desData = "des=" + this.TCNAME;
//        utils.writeToSQPDataFile("des", this.dataFile, desData);
    }
    
    private void checkStatus() {
        File bqsInfo = new File(this.TCLOCATION + utils.SEPARATOR + "bqs.info");
        File bqsConf = new File(this.TCLOCATION + utils.SEPARATOR + "bqs.conf");
        this.runFlag = utils.checkFileExists(bqsInfo) && utils.checkFileExists(bqsConf);
        if(runFlag) {
            statusL.setText("Ready To Run Flow");
            statusL.setTextFill(Color.GREEN);
        }
        else {
            statusL.setText("Not Ready To Run Flow");
            statusL.setTextFill(Color.RED);
        }
        
        initRunVisibility();
        setStatusL();
    }
    
    
    private String trimLDFLoc(String path) {
        String t = path.replace(utils.SEPARATOR, utils.LISTVIEW_SEP_KEY);
        String k = this.TCLOCATION.replace(utils.SEPARATOR, utils.LISTVIEW_SEP_KEY);

        String [] l = t.split(k);
        
        if(l.length == 2) {
            String n = l[1];
            String [] n2 = n.split(utils.LISTVIEW_SEP_KEY);
            String a = n2[1];
            for(int i = 2; i < n2.length; i++) {
                a += utils.SEPARATOR + n2[i];
            }
            return a;
        }
        else 
            return path;
        
    }
    
    @FXML
    public void chooseLDFLocation(ActionEvent event) {
        Node node = (Node) event.getSource();
        FileChooser chooser = new FileChooser();
      
        chooser.setInitialDirectory(new File(this.TCLOCATION));
        
        chooser.setTitle("Select LDF File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("LDF File", "*.ldf"));
        File ldfFile = chooser.showOpenDialog(node.getScene().getWindow());
        if(ldfFile != null) {
            String path = trimLDFLoc(ldfFile.getAbsolutePath());
            //ldfFileTF.setText(ldfFile.getAbsolutePath());
            ldfFileTF.setText(path);
            checkAllFields();
        }
    }
    
    
    @FXML
    public void chooseConfLocation(ActionEvent event) {
        Node node = (Node) event.getSource();
        FileChooser chooser = new FileChooser();
      
        chooser.setInitialDirectory(new File(this.TCLOCATION));
        
        chooser.setTitle("Select Configuration File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Conf File", "*.txt"));
        File confFile = chooser.showOpenDialog(node.getScene().getWindow());
        if(confFile != null) {
            String path = trimLDFLoc(confFile.getAbsolutePath());
            confFileTF.setText(path);
            checkAllFields();   
        }
    }
    
    @FXML
    public void chooseDefaultLocation(ActionEvent event) {
        String path = "sim" + utils.SEPARATOR + "input_files" + utils.SEPARATOR + "input.txt";
        confFileTF.setText(path);
        checkAllFields();
    }
    
    @FXML
    public void editConfigFile(ActionEvent event) {
        final Stage popupStage = new Stage(StageStyle.UTILITY);    
        FXMLLoader loader = new FXMLLoader(SystemsQualityPlatform.class.getResource("views/editConfig.fxml"));
        AnchorPane rootLayout = null;
        try {
            rootLayout = (AnchorPane) loader.load();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (rootLayout != null) {
            Scene scene = new Scene(rootLayout); 
            popupStage.setScene(scene);
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("Create Configuration File");
            
            EditConfigController controller = loader.<EditConfigController>getController();
            controller.init(this.TCLOCATION);

            popupStage.show();
            
            popupStage.setOnHidden((WindowEvent we) -> {
                String path = controller.getConfPath();
                if(path != null) {
                    path = trimLDFLoc(path);
                    confFileTF.setText(path);
                }
            });
        }
    }
    
    
    private void writeToFile() {
        String ldfData = "ldf=" + ldfFileTF.getText();
        String edaData = "eda=" + (String) edaCB.getValue();
        String confData = "conf="+ confFileTF.getText();
        utils.writeToSQPDataFile("ldf", this.dataFile, ldfData);
        utils.writeToSQPDataFile("conf", this.dataFile, confData);
        utils.writeToSQPDataFile("eda", this.dataFile, edaData);
    }
    
    
    @FXML 
    public void setButtonClick(ActionEvent event) {
        writeToFile();
        String ldfLoc = ldfFileTF.getText();
        trunckSetupB bqsSetup = new trunckSetupB(this.TCLOCATION);
        bqsSetup.createBQSFiles(ldfLoc);
        checkStatus();
    }
    
    private String getLDFLoc(String data) {
        if(utils.checkFileExists(new File(data)))
            return data;
        else
            return this.TCLOCATION + utils.SEPARATOR + data;
    }
    
    @FXML
    public void runFlow(ActionEvent event) {
        Thread t1 = new Thread(new Runnable() {
           @Override
           public void run() {
               //String ldf = ldfFileTF.getText();
               String ldf = getLDFLoc(ldfFileTF.getText());
               String eda = (String) edaCB.getValue();
               String conf = confFileTF.getText();
               
               runFlow rF = new runFlow(TCLOCATION, ldf, eda, conf);
               rF.run(hasDaisy, isFirstTC, tcs);
               
           } 
        });
        t1.start();
       
    }
    
    
    
    
    
}
