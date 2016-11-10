/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.views;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.DirectoryChooser;
import solution.quality.platform.dataStructures.casesStruct;
import solution.quality.platform.runFlow.trunckSetupA;
import solution.quality.platform.utils;

/**
 * FXML Controller class
 *
 * @author nmallam1
 */
public class AddSuiteController implements Initializable {

    @FXML TextField suiteNameTF;
    @FXML TextField suiteLocTF;
    
    @FXML Button browseB;
    
    @FXML TextArea InfoTA;
    
    @FXML Label daisyChainL;
    
    @FXML Button createB;
    
    
    
    String suiteLoc = null;
    
    String suiteName = null;
    String desLoc = null;
    
    casesStruct c = null;
    boolean hasDaisy = false;
    String [] testCases = null;
    
    private boolean hasDefEda;
    private String defEda;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        daisyChainL.setVisible(false);
    }


    public void init(String path, boolean hasDefEda, String defEda) { 
        this.suiteLoc = path + utils.SEPARATOR + "suites"; 
        this.hasDefEda = hasDefEda;
        this.defEda = defEda;
    }
    
    
    private void setSuiteName(String data) {this.suiteName = data;}
    public String getSuiteName() {return suiteName;}
    
    private void setDesLoc(String data) {this.desLoc = data;}
    public String getDesLoc() {return desLoc;}
    
    
    private void setC() {
        c = new casesStruct();
        c.setSuiteName(getSuiteName());
        this.testCases = getTestCasesDirectories();
        ArrayList<String> list = new ArrayList(Arrays.asList(testCases));
        c.setTestCases(list);
    }
    public casesStruct getC() {return c;}
    
    
    private void setAddInfo(String [] testCases) {
        InfoTA.clear();
        for(String s: testCases)
            InfoTA.appendText(s + "\n");
        if(testCases.length > 1) {
            this.hasDaisy = true;
            daisyChainL.setText("DAISYCHAIN = YES");
        }
        else {
            daisyChainL.setText("DAISYCHAIN = NO");
        }
        daisyChainL.setVisible(true);
    }
    @FXML
    public void onEnterPressed(KeyEvent ke) {
        if(ke.getCode().equals(KeyCode.ENTER))
            setSuiteName(suiteNameTF.getText());
    }
    
    @FXML
    public void chooseProjLocation(ActionEvent event) {
        Node node = (Node) event.getSource();
        DirectoryChooser chooser = new DirectoryChooser();
        
        if(getDesLoc() != null)
            chooser.setInitialDirectory(new File(getDesLoc()));
        
        chooser.setTitle("Select Design Directory");
        
        File dirSel = chooser.showDialog(node.getScene().getWindow());
        if(dirSel != null) {
            setDesLoc(dirSel.getAbsolutePath());
            suiteLocTF.setText(getDesLoc());
            this.testCases = getTestCasesDirectories();
            setAddInfo(testCases);
        }
    }
    
    
    @FXML
    public void createButtonClick(ActionEvent e) {
        boolean flag1 = false;
        boolean flag2 = false;
        
        setSuiteName(suiteNameTF.getText());
        
        if(getSuiteName() == null || getSuiteName().isEmpty())
            utils.showErrorAlert("Missing Suite Name", "Empty Suite Name", "Please input Suite's Name!");
        else
            flag1 = true;
        
        if(getDesLoc() == null || getDesLoc().isEmpty())
            utils.showErrorAlert("Missing Design Directory Location", "Empty Design Directory Location", "Please choose Design Directory!");
        else
            flag2 = true;
        
        if(flag1 && flag2) {
            trunckSetupA tSA = new trunckSetupA(this.suiteLoc, getSuiteName(), getDesLoc());
            boolean flag = tSA.createSuiteNameDir();
            if(flag) {
                setC();
                tSA.copyTestCases();
                tSA.createDirectories(testCases);
                if(this.hasDefEda)
                    tSA.defEDASQPdir(testCases, this.defEda);
            }
            ((Node)(e.getSource())).getScene().getWindow().hide();
        }
    }
    
    
    public String[] getTestCasesDirectories() {
        File file = new File(getDesLoc());
        //String[] directories = file.list((File current, String name) -> new File(current, name).isDirectory());
        String[] directories = utils.getListofFiles(file);
        return directories;
    }
    
    
    
    
}
