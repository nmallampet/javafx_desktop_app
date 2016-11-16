/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 *
 * @author nmallam1
 */
public class SystemsQualityPlatform extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            AnchorPane rootLayout = (AnchorPane) FXMLLoader.load(SystemsQualityPlatform.class.getResource("views/mainWindow.fxml"));
            Scene scene = new Scene(rootLayout, 1072, 600);
            
            //AeroFX.style();
            primaryStage.setTitle("Systems Quality Platform");
            //primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("views/imgs/LatticeLogo.png")));
            primaryStage.setScene(scene);
            primaryStage.show();
        }
        catch (Exception e) {
            e.printStackTrace();
            //System.out.println("Can't load Main Window");
        }
        
        primaryStage.setOnCloseRequest((WindowEvent e) -> {
            Platform.exit();
        });
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
