/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.saveXML;

import java.io.File;
import javafx.scene.control.Alert;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import solution.quality.platform.utils;

/**
 *
 * @author nmallam1
 */
public class xmlController {
    
    private sqpXML PROJMETA;
    final private String path;
    
    public xmlController(String path) {
        PROJMETA = null;
        this.path = path;
    }
    
    public void setPROJMETA(sqpXML data) {this.PROJMETA = data;}
    public sqpXML getPROJMETA() {return PROJMETA;}
    
    
    public void saveFile() {
        System.out.println("Saved");
        if(PROJMETA != null) {
            String fileName = PROJMETA.getName() + ".sqp";
            String loc = path + utils.SEPARATOR + fileName;
            File file = new File(loc);
                try {
                    JAXBContext context = JAXBContext
                            .newInstance(sqpXML.class);
                    Marshaller m = context.createMarshaller();
                    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

                    // Marshalling and saving XML to the file.
                    m.marshal(PROJMETA, file);

                    // Save the file path to the registry.
                    //setPersonFilePath(file);
                } catch (Exception e) { // catches ANY exception
                    utils.showErrorAlert("Error in Saving", "Failed to Save", "Failed to write to File");
                }
        }
    }
    
    public void loadFile(File file) {
        try {
            JAXBContext context = JAXBContext
                    .newInstance(sqpXML.class);
            Unmarshaller um = context.createUnmarshaller();
            // Reading XML from the file and unmarshalling.
            sqpXML wrapper = (sqpXML) um.unmarshal(file);
            setPROJMETA(wrapper);
            // Save the file path to the registry.
            //setPersonFilePath(file);

        } catch (Exception e) { // catches ANY exception
            utils.showErrorAlert("Error in loading", "Failed to Load Data", "Failed to load data");
        }
    }
    
    
    
    
    }
    
