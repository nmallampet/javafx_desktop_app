/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.views;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Circle;

/**
 * FXML Controller class
 *
 * @author nmallam1
 */
public class ListViewCellController implements Initializable {

    @FXML AnchorPane cellAP;
    @FXML Label nameL;
    @FXML Circle statusC;
    
    private int value;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cellAP.setPadding(new Insets(0,0,0,-10));
  
    }    
    
    
    public void setName(String name) { nameL.setText(name);}
    
    public AnchorPane getAP() {return cellAP;}
    
    public void setStatus(int value) {
        this.value = value;
        switch(value) {
            case 0:
                statusC.getStyleClass().add("status-progress");
                break;
            case 1:
                statusC.getStyleClass().add("status-fail");
                break;
            case 2:
                statusC.getStyleClass().add("status-pass");
                break;
        }
    }
}
