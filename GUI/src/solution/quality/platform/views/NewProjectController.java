/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.views;

import java.io.File;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.DirectoryChooser;
import solution.quality.platform.saveXML.sqpXML;
import solution.quality.platform.utils;

/**
 * FXML Controller class
 *
 * @author nmallam1
 */
public class NewProjectController implements Initializable {

    @FXML TextField projNameTF;
    @FXML TextField projLocationTF;
    
    @FXML Button browseB;
    @FXML Button createB;
    
    private String projName = null;
    private String projLoc = null;
    
    private sqpXML projMeta = null;
    
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        init();
    }

    private void init() {
        projLocationTF.setEditable(false);
    }
    
    private void setProjName(String data) {this.projName = data; }
    private void setProjLoc(String data) {this.projLoc = data; }
    
    private String getProjName() {return projName;}
    public String getProjLoc() {return projLoc;}
    
    public sqpXML getProjMeta() {return projMeta;}
    
    private void createProjDir() {
        String loc = getProjLoc() + utils.SEPARATOR + getProjName();
        boolean flag = utils.createDirectory(loc);
        
        String locTC = loc + utils.SEPARATOR + "suites";
        flag = utils.createDirectory(locTC);
        if(!flag)
            utils.showErrorAlert("Error in creating project", "Project's Directory failed to be created", 
                                "Projec'ts Directory failed to be created. Please try again!");
    }
    
    @FXML
    public void onEnterPressed(KeyEvent ke) {
        if(ke.getCode().equals(KeyCode.ENTER))
            setProjName(projNameTF.getText());
    }
    
    @FXML
    public void chooseProjLocation(ActionEvent event) {
        Node node = (Node) event.getSource();
        DirectoryChooser chooser = new DirectoryChooser();
        
        if(getProjLoc() != null)
            chooser.setInitialDirectory(new File(getProjLoc()));
        
        chooser.setTitle("Project Destination");
        
        File dirSel = chooser.showDialog(node.getScene().getWindow());
        if(dirSel != null) {
            setProjLoc(dirSel.getAbsolutePath());
            projLocationTF.setText(getProjLoc());
        }
    }
    
    @FXML
    public void createProj(ActionEvent event) {
        boolean flag1 = false;
        boolean flag2 = false;
        
        setProjName(projNameTF.getText());
        
        if(getProjName() == null)
            utils.showErrorAlert("Missing Project Name", "Empty Project Name", "Please input Project's Name!");
        else
            flag1 = true;
        
        if(getProjLoc() == null)
            utils.showErrorAlert("Missing Project Location", "Empty Project Location", "Please choose Project's Location!");
        else
            flag2 = true;
        
        if(flag1 && flag2) {
            projMeta = new sqpXML();
            projMeta.setName(getProjName());
            projMeta.setDate(utils.getCurrentDate());
            projMeta.setModDate(utils.getCurrentDate());
            createProjDir();
            ((Node)(event.getSource())).getScene().getWindow().hide();
        }
    }
    
}
