/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.views;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import solution.quality.platform.utils;

/**
 * FXML Controller class
 *
 * @author nmallam1
 */
public class ResultsPaneController implements Initializable {

    @FXML Button TrunkB;
    @FXML Button PrgB;
    @FXML Button FtdiB;
    @FXML Button OutB;
    
    @FXML TextArea viewTA;
    
    
    private String TCLOC;
    private String TCNAME;
    
    //private String[] TCS;
    ArrayList<String> TCS;
    private boolean hasDaisy;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
    }    
    
    public void init(String TCLOC, String TCNAME, ArrayList<String> TCS) {
        this.TCLOC = TCLOC;
        this.TCNAME = TCNAME;
        this.TCS = TCS;
        this.hasDaisy = TCS.size() > 1;
    }
   
    private void showData(File file) {
        viewTA.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            for(String l = br.readLine(); l!= null; l = br.readLine()){
                String l2 = l + "\n";
                viewTA.appendText(l2);
            }
        }
        catch(Exception e) {    
            e.printStackTrace();
        }
    }
    
    private void showDataInUse() {
        viewTA.setText("The file is currently in use!\nPlease try again!!");
    }
    
    
    @FXML
    public void TrunkBClick(ActionEvent e) {
        String path = this.TCLOC + utils.SEPARATOR + "logs" + utils.SEPARATOR + "trunkLog.txt";
        File file = new File(path);
        boolean flag = utils.checkFileInUse(file);
        if(flag) 
            showData(file);
        else
            showDataInUse();
            
    }
    
    @FXML
    public void PrgBClick(ActionEvent e) {
        
        if(this.hasDaisy) {
            String sDir = new File(this.TCLOC).getParent();
            String path = sDir + utils.SEPARATOR + this.TCS.get(0) + utils.SEPARATOR + "logs" + utils.SEPARATOR + "ProgrammerOutput.txt";
            File file = new File(path);
            boolean flag = utils.checkFileInUse(file);
            if(flag) 
                showData(file);
            else
                showDataInUse();
        }
        else {
            String path = this.TCLOC + utils.SEPARATOR + "logs" + utils.SEPARATOR + "ProgrammerOutput.txt";
            File file = new File(path);
            boolean flag = utils.checkFileInUse(file);
            if(flag) 
                showData(file);
            else
                showDataInUse();
        }
            
    }
    
    @FXML
    public void FtdiBClick(ActionEvent e) {
        String path = this.TCLOC + utils.SEPARATOR + "logs" + utils.SEPARATOR + "ftdiLog.txt";
        File file = new File(path);
        boolean flag = utils.checkFileInUse(file);
        if(flag) 
            showData(file);
        else
            showDataInUse();
            
    }
    
    @FXML
    public void OutBClick(ActionEvent e) {
        String path = this.TCLOC + utils.SEPARATOR + "sim" + utils.SEPARATOR + "output_files" + utils.SEPARATOR + "output.txt";
        File file = new File(path);
        boolean flag = utils.checkFileInUse(file);
        if(flag) 
            showData(file);
        else
            showDataInUse();
            
    }
    
    
    
}
