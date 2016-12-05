/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sqp;

import java.io.File;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author nmallam1
 */
public class createHierarchy {
    
    
    final String outputLoc;
    
    
    public createHierarchy(String outputLoc) {
        this.outputLoc = new File(outputLoc).getAbsolutePath();
    }
    
    public void createSuite(String suiteName) {
        String suitePath = this.outputLoc + utils.SEPARATOR + suiteName;
        utils.createDirectory(suitePath);
    }
    
    public String copyTC(String suiteName, String TCName, String des) {
        if(des != null) {
            //String origDesPath = getPropKey(des);
            String origDesPath = des;
            File origDesFile = new File(origDesPath);
            if(origDesFile.exists()) {
                String TCDesPath = origDesPath + utils.SEPARATOR + TCName;
                File TCDesFile = new File(TCDesPath);
                if(TCDesFile.exists()) {
                    String tcLocPath = this.outputLoc + utils.SEPARATOR + suiteName + utils.SEPARATOR + TCName;
                    utils.createDirectory(tcLocPath);
                    File dest = new File(tcLocPath);
                    try {
                        FileUtils.copyDirectory(TCDesFile, dest);
                        return tcLocPath;
                    }
                    catch(Exception e) {
                        System.err.println("Error with copying design directory " + tcLocPath);
                    }
                }
                else {
                   System.err.println("Error: Test Case *" + TCName + " doesn't exisit in " + origDesPath); 
                }
            }
            else {
                System.err.println("Error: Invalid des path specified in input file for <" + suiteName + ">*" + TCName);
            }
            
        }
        else {
            System.err.println("Error: No des path specified in input file for <" + suiteName + ">*" + TCName);
        }
        
        return null;
    } 
    
    
}
