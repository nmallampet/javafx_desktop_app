/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sqp;

import java.io.File;
import static sqp.inputFileParser.FAIL_CODE;
//-infile C:\\Users\\nmallam1\\Desktop\\tmpToday\\input.txt -out C:\\Users\\nmallam1\\Desktop\\tmpToday\\out
/**
 *
 * @author nmallam1
 */
public class propStruct {
    
    final String suiteName;
    final String testCaseName;
    final String relativePath;
    final String inRelativePath;
    
    private String origDes;
    private String des;
    private String conf;
    private String eda;
    private String ldf;
    
    private boolean passBitgen = false;
    private boolean passProgrammer = false;

    public propStruct(String suiteName, String testCaseName, String relativePath, String inRelativePath) {
        this.suiteName = suiteName;
        this.testCaseName = testCaseName;
        this.relativePath = relativePath;
        this.inRelativePath = inRelativePath;
        //System.out.println(String.format("Relative Path = %s%s%s%s%s", this.relativePath, utils.SEPARATOR, this.suiteName, utils.SEPARATOR, this.testCaseName));
    }
    
    public String getOrigDes() {return origDes;}
    public String getDes() {return des;}
    public String getConf() {return conf;}
    public String getEda() {return eda;}
    public String getLdf() {return ldf;}
    
    
    public boolean getPassBitgen() {return passBitgen;}
    public boolean getPassProgrammer() {return passProgrammer;}
    public void setPassBitgen() {passBitgen = true;}
    public void setPassProgrammer() {passProgrammer = true;}
    
    public void setOrigDes(String data) {
        String newData = new File(this.inRelativePath).getParent() +  utils.SEPARATOR + data;
        if(utils.checkFileExists(new File(newData)))
            this.origDes = newData;
        else {
            System.err.println("Error: Design Directory " + data + " is invalid");
            System.exit(FAIL_CODE);
        }
    }
    public void setDes(String data) {
        String newData = this.relativePath + utils.SEPARATOR + this.suiteName;
        this.des=newData;
    }
    
    
    public void setConf(String data) {
        
//        if(data.trim().isEmpty()) {
//            String defaultValue = getDes() + utils.SEPARATOR + this.testCaseName 
//                            + utils.SEPARATOR + "sim" + utils.SEPARATOR + "input_files" + utils.SEPARATOR + "input.txt";
//            if(utils.checkFileExists(new File(defaultValue)))
//                this.conf = defaultValue;
//            else 
//                System.err.println("Error: No conf file in default location (sim/input_files/input.txt) for " + this.testCaseName);
//        }
//        else {
//            String newValueConf = new File(data).getAbsolutePath();
//            if(!utils.checkFileExists(new File(newValueConf))) {
//                    String newValue = getDes() + utils.SEPARATOR + this.suiteName + utils.SEPARATOR + "sim" + utils.SEPARATOR + "input_files" + utils.SEPARATOR + data;
//                    if(utils.checkFileExists(new File(newValue))) {
//                        this.conf = newValue;
//                    }
//                    else {
//                        System.err.println("Error: Conf file " + data + " not found");                    
//                        System.exit(FAIL_CODE);
//                    }
//                }
//                else {
//                    this.conf = newValueConf;
//                }
//        }
        if(data.trim().isEmpty()) {
            String defaultValue = getDes() + utils.SEPARATOR + this.testCaseName 
                            + utils.SEPARATOR + "sim" + utils.SEPARATOR + "input_files" + utils.SEPARATOR + "input.txt";
            this.conf = defaultValue;
        }
        else {
            if(!utils.checkFileExists(new File(data))) {
                String newValue = getDes() + utils.SEPARATOR + this.testCaseName + utils.SEPARATOR + data;
                this.conf = data;
            }
            else 
                this.conf = data;
               
        }
    }
    
    public void setEda(String data) {
        if(edaParser.isValidEda(data)) 
            this.eda = data;        
        else {
            System.err.println("Error: EDA '" + eda + "' is not a valid EDA SW");            
            System.exit(FAIL_CODE);
        }
    }
    
    public void setLdf(String data) {
        /*
        String newValueLdf = new File(data).getAbsolutePath();
        if(!utils.checkFileExists(new File(newValueLdf))) {
            String newValue = getDes() + utils.SEPARATOR + this.testCaseName + utils.SEPARATOR + data;
            if(utils.checkFileExists(new File(newValue)))
                this.ldf = newValue;           
            else {
                System.err.println("Error: ldf file " + data + " not found");                
                System.exit(FAIL_CODE);
            }
        }
        else {
            this.ldf = newValueLdf;
        }
        */
        if(!utils.checkFileExists(new File(data))) {
            String newValue = getDes() + utils.SEPARATOR + this.testCaseName + utils.SEPARATOR + data;
            this.ldf = newValue;           
        }
        else {
            this.ldf = data;
        }
    }
    
    @Override
    public String toString() {
        return String.format("[%s]:\n%s\n\t%s\n\t%s\n\t%s\n\t%s\n", this.suiteName, this.testCaseName, getDes(), getConf(), getEda(), getLdf());
    }
    
    
}
