/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.runFlow;

import java.io.File;
import java.io.PrintWriter;
import org.apache.commons.io.FileUtils;
import solution.quality.platform.utils;

/**
 *
 * @author nmallam1
 */
public class trunckSetupA {
    
    final String suiteName;
    final String suiteLoc;
    final String desDir;
    
    public trunckSetupA(String projLoc, String suiteName, String desDir) {
        this.suiteName = suiteName;
        this.suiteLoc = projLoc + utils.SEPARATOR + suiteName;
        this.desDir = desDir; 
    }
    
    public boolean createSuiteNameDir() {
        return utils.createDirectory(this.suiteLoc);
    }
    
    public void copyTestCases() {
        File dest = new File(this.suiteLoc);
        File src = new File(this.desDir);
        try {
            FileUtils.copyDirectory(src, dest);
        }
        catch(Exception e) {
            System.err.println("Error with copying design directory " + src);
        }
    }
    
    public void createDirectories(String [] testCases) {
        for(String s: testCases) {
            boolean flag = createSimDir(s);
            if(flag)
                flag = createLogsDir(s);
            if(flag)
                createSQPDir(s);
        }
        
    }
    
    private boolean createSimDir(String testCase) {
        String path = this.suiteLoc + utils.SEPARATOR + testCase + utils.SEPARATOR + "sim";
        if(utils.createDirectory(path))
           if(utils.createDirectory(path + utils.SEPARATOR + "input_files"))
               return utils.createDirectory(path + utils.SEPARATOR + "output_files");
        
        return false;
    }

    private boolean createLogsDir(String testCase) {
        String path = this.suiteLoc + utils.SEPARATOR + testCase + utils.SEPARATOR + "logs";
        return utils.createDirectory(path);
    }
    
    private void createSQPDir(String testCase) {
        String path = this.suiteLoc + utils.SEPARATOR + testCase + utils.SEPARATOR + "sqpData";
        String path2 = this.suiteLoc + utils.SEPARATOR + testCase + utils.SEPARATOR + "sqpData" + utils.SEPARATOR + "data.txt";
        boolean flag =  utils.createDirectory(path);
        if(flag) {
            try (PrintWriter dataFile = new PrintWriter(path2, "UTF-8")) {
               //dataFile.append(String.format("des=%s",new File(this.desDir).getName()));
               dataFile.append(String.format("des=%s",this.suiteName));
            }
            catch(Exception e) {
                System.err.println("Error in creating data.txt: " + e.getMessage());
            }
        }   
    }
    
    public void defEDASQPdir(String [] testCases, String eda) {
        String edaData = "eda=" + eda;
        for(String s: testCases) {
            String path = this.suiteLoc + utils.SEPARATOR + s + utils.SEPARATOR + "sqpData" + utils.SEPARATOR + "data.txt";
            utils.writeToSQPDataFile("eda", path, edaData);
        }
    }
}
