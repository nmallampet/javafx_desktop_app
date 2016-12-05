/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sqp;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author nmallam1
 */
public class trunkSetup {
    
    final private String testCaseLoc;
    final private String outputLoc;
    final private String suiteName;
    final private String testCaseName;
    //final ArrayList<String> props;
    final propStruct props;
    
    private String des;
    private String ldf;
    private String conf;
    private String eda;
    
    private boolean hasDaisyChain;
    int dasiyChainSize;
    
    //HashMap<String, ArrayList<String>> listTCProps = null;
    HashMap<String, propStruct> listTCProps = null;
    ArrayList<String> daisyListOfTCs;
    boolean firstTestCaseDaisy;
    
    public trunkSetup(String outputLoc, String suiteName, String testCaseName, propStruct props) {
        this.outputLoc = new File(outputLoc).getAbsolutePath();
        this.suiteName = suiteName;
        this.testCaseName = testCaseName;
        this.props = props;
        this.testCaseLoc = this.outputLoc + utils.SEPARATOR + suiteName + utils.SEPARATOR + testCaseName;
        //get file locations and store it in {des, ldf, conf, eda} variables 
        setPropValues();
        
    }
    
    public void setHasDaisyChain(boolean value, ArrayList<String> daisyListOfTCs, int size, HashMap<String, propStruct> listTCProps, boolean firstTestCaseDaisy) { 
        this.hasDaisyChain = value; 
        this.daisyListOfTCs = daisyListOfTCs;
        this.dasiyChainSize = size;
        this.listTCProps = listTCProps;
        this.firstTestCaseDaisy = firstTestCaseDaisy;
    }
    
    
    // run flow to set up testcase directory
    public void createFiles() {
        createBQSFiles();
        createLogsDir();
        createSimDir();  
    }
    
    public void createProgrammerFiles() {
        if(this.hasDaisyChain)
            createProgrammerDirDaisy();
        else 
            createProgrammerDir();
    }
    
    private void createBQSFiles() {
        createBQSInfo();
        createBQSConf();
    }
    
    private void createLogsDir() {
        String path = this.testCaseLoc + "\\logs";
        utils.createDirectory(path);
    }
    
    private void createSimDir() {
        String simDir = this.testCaseLoc +  utils.SEPARATOR + "sim";
        boolean simFlag = utils.createDirectory(simDir);
        if(simFlag) {
            String simInputDir = this.testCaseLoc + utils.SEPARATOR + "sim" +  utils.SEPARATOR + "input_files";
            String simOutputDir = this.testCaseLoc + utils.SEPARATOR + "sim" + utils.SEPARATOR + "output_files";
            boolean inFlag = utils.createDirectory(simInputDir);
            if(inFlag) {
                String dest = simInputDir + utils.SEPARATOR + "input.txt";
                if(!new File(dest).exists()) {
                    try {
                        FileUtils.copyFile(new File(this.conf), new File(dest));
                    }
                    catch(Exception e) {
                        System.err.println("Error in copying configuration file " + this.conf + " to input_files directory");
                    }
                }
            }
            utils.createDirectory(simOutputDir);
            
            
        }
    }
        
    private void createProgrammerDir() {
        
        String path = this.testCaseLoc + utils.SEPARATOR + "programmer";
        String implDirectory = LDFParser.getImplDirectory(new File(this.ldf));
        String bitFileName = LDFParser.getBitFileName(new File(this.ldf));
        String xcfFileName = implDirectory + ".xcf";
        //String bitFileLocation = new File(this.ldf).getParent() + utils.SEPARATOR + implDirectory + utils.SEPARATOR + bitFileName;
        String bitFileLocation = this.testCaseLoc + utils.SEPARATOR + "_scratch" + utils.SEPARATOR + implDirectory + utils.SEPARATOR + bitFileName;
        String xcfFileLocation = new File(this.ldf).getParent() + utils.SEPARATOR + implDirectory + utils.SEPARATOR + xcfFileName;
        String xcfDes = this.testCaseLoc + utils.SEPARATOR + "programmer" + utils.SEPARATOR + "test.xcf";
        
        boolean flag = utils.createDirectory(path);
        if(flag) {
            boolean xcfFileExistsFlag = utils.checkFileExists(new File(xcfFileLocation));
            //System.out.println(String.format("%s exists: %s", xcfFileLocation, xcfFileExistsFlag));
            if(xcfFileExistsFlag) {
                String portAddr = XCFParser.getPortNum(new File(xcfFileLocation));
                createPostProcessPy(portAddr);
                try {
                    FileUtils.copyFile(new File(xcfFileLocation), new File(xcfDes));
                }
                catch(Exception e) {
                    System.err.println(String.format("Error copying XCF file %s to %s", xcfFileLocation, xcfDes));
                }
                XCFParser.setBitFile(bitFileLocation, new File(xcfDes));
            }
            else { //xcf file not found so copy template
                //String templateXCFFile = "res/XCFTemplates/XCFTemplate.xcf";
                String templateXCFFile = utils.getJarLocation() + utils.SEPARATOR + "res" + utils.SEPARATOR + "XCFTemplates" + utils.SEPARATOR +  "XCFTemplates.xcf";
                String portAddr = "FTUSB-0";
                createPostProcessPy(portAddr);
                try {
                    FileUtils.copyFile(new File(templateXCFFile), new File(xcfDes));
                }
                catch(Exception e) {
                    System.err.println(String.format("Error copying template XCF file %s to %s", templateXCFFile, xcfDes));
                }
                
                //set deviceNames
                String synProjName = LDFParser.getSynProjName(new File(this.ldf));
                String synProjLoc = new File(this.ldf).getParent() + utils.SEPARATOR + implDirectory + utils.SEPARATOR + synProjName;
                
                
                
                
                //deviceInfo dInfo = new deviceInfo(synProjLoc); 
                //deviceInfo dInfo = null;
                deviceInfo dInfo = new deviceInfo(this.ldf, this.eda);
                
                
                XCFParser.setDeviceNames(dInfo, new File(xcfDes));
                
                
                XCFParser.setBitFile(bitFileLocation, new File(xcfDes));
            }
        }
    }
    
    private void createProgrammerDirDaisy() {
        String path = this.testCaseLoc + utils.SEPARATOR + "programmer";
        boolean flag = utils.createDirectory(path);
        String key = this.suiteName + "_" + this.testCaseName;
        if(flag && this.firstTestCaseDaisy) {
            //String deviceName = XCFParser.getDeviceName(new File(this.ldf));
       
            //copy the testxcf template
            //String xcfTemplate = "res/XCFTemplates/daisyChainXCFTemplate.xcf";
            String xcfTemplate = utils.getJarLocation() + utils.SEPARATOR + "res" + utils.SEPARATOR + "XCFTemplates" + utils.SEPARATOR +  "daisyChainXCFTemplate.xcf";
            String xcfDes = this.testCaseLoc + utils.SEPARATOR + "programmer" + utils.SEPARATOR + "test.xcf";
            try {
                FileUtils.copyFile(new File(xcfTemplate), new File(xcfDes));
            }
            catch(Exception e) {
                System.err.println("Error in copying configuration file " + this.conf + " to input_files directory");
            }
            
            //set bitfile location
            daisyChainBitFileLocationPerTestCase(xcfDes);

            //create PostProces.py
            createPostProcessPyDefault();

        }
        
    }
    
    private void daisyChainBitFileLocationPerTestCase(String xcfFileLoc) {
        if(this.listTCProps != null) {
            File file = new File(xcfFileLoc);
            for(int i = 0; i < this.dasiyChainSize; i++) {
                String key = this.suiteName + "_" + this.daisyListOfTCs.get(i);
                
                String tmpTestCaseLoc = this.outputLoc + utils.SEPARATOR + this.suiteName + utils.SEPARATOR + this.daisyListOfTCs.get(i);
                
                propStruct tmp = this.listTCProps.get(key);
                //String ldfFile = utils.getLDFFromList(tmp);
                String ldfFile = tmp.getLdf();
                //String tmpEda = utils.getEDAFromList(tmp);
                String tmpEda = tmp.getEda();
                String implDirectory = LDFParser.getImplDirectory(new File(ldfFile));
                String bitFileName = LDFParser.getBitFileName(new File(ldfFile));
                String synFileName = LDFParser.getSynProjName(new File(ldfFile));
                
                String synFileLoc = new File(ldfFile).getParent() + utils.SEPARATOR + implDirectory + utils.SEPARATOR + synFileName;
                
                
                
                //deviceInfo dInfo = new deviceInfo(synFileLoc);
                //deviceInfo dInfo = null;
                deviceInfo dInfo = new deviceInfo(ldfFile, tmpEda);
                
                
                XCFParser.setDeviceNamesDaisy(dInfo, file, i);
                
                //String bitFileLocation = new File(ldfFile).getParent() + utils.SEPARATOR + implDirectory + utils.SEPARATOR + bitFileName;
                String bitFileLocation = tmpTestCaseLoc + utils.SEPARATOR + "_scratch" + utils.SEPARATOR + implDirectory + utils.SEPARATOR + bitFileName;
                //set bitfile location in test.xcf
                XCFParser.setBitFileDaisy(bitFileLocation, file, i);
                
            }
        }
    }
    
    private String getEnvLocation(String eda) {
        return edaParser.getEnvFromEda(eda);
    }
    
    private void setPropValues() {
        this.des = props.getDes();
        this.conf = props.getConf();
        this.eda = props.getEda();
        this.ldf = props.getLdf();
    }
    
    private void createBQSInfo() {
        String path = this.testCaseLoc + "\\bqs.info";
        try (PrintWriter confFile = new PrintWriter(path, "UTF-8")) {
            confFile.println("[qa]");
            confFile.println("ldf_file = " + this.ldf);
        }
        catch(Exception e) {
            System.err.println("Error in creating bqs.info: " + e.getMessage());
        }
    }
        
    private void createBQSConf() {
        String path = this.testCaseLoc + "\\bqs.conf";
        try (PrintWriter confFile = new PrintWriter(path, "UTF-8")) {
            confFile.println("[configuration information]\n" +
                                "area = Download\n" +
                                "type = File\n");
            confFile.println("[method]\n" +
                            "check_diamond_flow  = 1\n\n" +
                            "[check_diamond_flow]\n" +
                            "check_flow = bitstream\n");
        }
        catch(Exception e) {
            System.err.println("Error in creating bqs.conf: " + e.getMessage());
        }
    }
    
    private void createPostProcessPyDefault() {
        String path = this.testCaseLoc + utils.SEPARATOR + "programmer" + utils.SEPARATOR + "post_process.py";
        String edaT = getEnvLocation(this.eda);
        try (PrintWriter confFile = new PrintWriter(path, "UTF-8")) {
            confFile.println("# -*- coding: utf-8 -*-\n");
            confFile.println("import os\nimport platform\n");
            confFile.println("eda=os.environ['" + edaT + "']");
            confFile.println("xcfLoc=os.path.dirname(os.path.realpath(__file__))\n");
            confFile.println("os.system('%s/bin/nt64/pgrcmd -infile %s/test.xcf'% (eda, xcfLoc))");
        }
        catch(Exception e) {
            System.err.println("Error in creating post_process.py: " + e.getMessage());
        }
    }
    
    private void createPostProcessPy(String portAddr) {
        String path = this.testCaseLoc + utils.SEPARATOR + "programmer" + utils.SEPARATOR + "post_process.py";
        String edaT = getEnvLocation(this.eda);
        
        try (PrintWriter confFile = new PrintWriter(path, "UTF-8")) {
            confFile.println("# -*- coding: utf-8 -*-\n");
            confFile.println("import os\nimport platform\n");
            confFile.println("eda=os.environ['" + edaT + "']");
            confFile.println("xcfLoc=os.path.dirname(os.path.realpath(__file__))\n");
            confFile.println("os.system('%s/bin/nt64/pgrcmd -infile %s/test.xcf –cabletype “USB2” –portaddress “" + portAddr + "”'% (eda, xcfLoc))");
        }
        catch(Exception e) {
            System.err.println("Error in creating post_process.py: " + e.getMessage());
        }
    }
   
}
