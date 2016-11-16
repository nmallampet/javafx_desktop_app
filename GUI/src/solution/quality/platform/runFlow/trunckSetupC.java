/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.runFlow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import org.apache.commons.io.FileUtils;
import solution.quality.platform.dataStructures.deviceInfo;
import solution.quality.platform.parsers.LDFParser;
import solution.quality.platform.parsers.XCFParser;
import solution.quality.platform.utils;

/**
 *
 * @author nmallam1
 */
public class trunckSetupC {
    
    
    final String TCLOC;
    final String LDFLOC;
    final String EDAENV;
    
    String [] testcases;
    
    public trunckSetupC(String TCLOC, String LDFLOC, String eda) {
        this.TCLOC = TCLOC;
        String ldfNew = utils.getFullLDFLoc(TCLOC, LDFLOC);
        this.LDFLOC = ldfNew;
        this.EDAENV = utils.getEnvFromEda(eda);
    }
    
    
    public void createProgrammerHeirarchy(boolean hasDaisy, String [] testcases) {
        if(hasDaisy) {
            this.testcases = testcases;
            createXCFnPY_DAISY();
        }
        else {
            createXCFnPY(); 
        }
    }
    
    private boolean createProgrammerDir() {
        String path = this.TCLOC + utils.SEPARATOR + "programmer";
        return utils.createDirectory(path);
    }
    
    
    private void createXCFnPY() {
        String path = this.TCLOC + utils.SEPARATOR + "programmer";
        String implDirectory = LDFParser.getImplDirectory(new File(this.LDFLOC));
        String bitFileName = LDFParser.getBitFileName(new File(this.LDFLOC));
        String xcfFileName = implDirectory + ".xcf";
        String bitFileLocation = this.TCLOC + utils.SEPARATOR + "_scratch" + utils.SEPARATOR + implDirectory + utils.SEPARATOR + bitFileName;
        String xcfFileLocation = new File(this.LDFLOC).getParent() + utils.SEPARATOR + implDirectory + utils.SEPARATOR + xcfFileName;
        String xcfDes = this.TCLOC + utils.SEPARATOR + "programmer" + utils.SEPARATOR + "test.xcf";
        
        boolean flag = createProgrammerDir();
        if(flag) {
            boolean xcfFileExistsFlag = utils.checkFileExists(new File(xcfFileLocation));
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
            else {
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
                
                deviceInfo dInfo = new deviceInfo(this.LDFLOC, this.EDAENV);
               
                XCFParser.setDeviceNames(dInfo, new File(xcfDes));
                
                XCFParser.setBitFile(bitFileLocation, new File(xcfDes));       
            }
        }   
    }
    
    private void createXCFnPY_DAISY() {
        boolean flag = createProgrammerDir();
        if(flag) {
       
            //copy the testxcf template
            //String xcfTemplate = "res/XCFTemplates/daisyChainXCFTemplate.xcf";
            String xcfTemplate = utils.getJarLocation() + utils.SEPARATOR + "res" + utils.SEPARATOR + "XCFTemplates" + utils.SEPARATOR +  "daisyChainXCFTemplate.xcf";
            String xcfDes = this.TCLOC + utils.SEPARATOR + "programmer" + utils.SEPARATOR + "test.xcf";
            try {
                FileUtils.copyFile(new File(xcfTemplate), new File(xcfDes));
            }
            catch(Exception e) {
                System.err.println("createXCFnPY_DAISY error");
            }
            
            //set bitfile location
            daisyChainBitFileLocationPerTestCase(xcfDes);

            //create PostProces.py
            createPostProcessPyDefault();

        }
    }
    
    
    private void daisyChainBitFileLocationPerTestCase(String xcfFileLoc) {
        File file = new File(xcfFileLoc);
        int i = 0;
        
        for(String s:this.testcases) {
            File t = new File(this.TCLOC);
            String ldfT, edaT;
            String pathNew = t.getParent() + utils.SEPARATOR + s;
            String dataFileLoc = pathNew + utils.SEPARATOR + "sqpData" + utils.SEPARATOR + "data.txt";
            ldfT = utils.getFullLDFLoc(pathNew, getInfoFromFile(dataFileLoc, "ldf"));
            edaT = getInfoFromFile(dataFileLoc, "eda");
            String implDirectory = LDFParser.getImplDirectory(new File(ldfT));
            String bitFileName = LDFParser.getBitFileName(new File(ldfT)); 
            deviceInfo dInfo = new deviceInfo(ldfT, utils.getEnvFromEda(edaT));
            XCFParser.setDeviceNamesDaisy(dInfo, file, i);

            String bitFileLocation = pathNew + utils.SEPARATOR + "_scratch" + utils.SEPARATOR + implDirectory + utils.SEPARATOR + bitFileName;
            //set bitfile location in test.xcf
            XCFParser.setBitFileDaisy(bitFileLocation, file, i);
            i++;
            
        }
    }
    
    private void createPostProcessPyDefault() {
        String path = this.TCLOC + utils.SEPARATOR + "programmer" + utils.SEPARATOR + "post_process.py";
        try (PrintWriter confFile = new PrintWriter(path, "UTF-8")) {
            confFile.println("# -*- coding: utf-8 -*-\n");
            confFile.println("import os\nimport platform\n");
            confFile.println("eda=os.environ['" + this.EDAENV + "']");
            confFile.println("xcfLoc=os.path.dirname(os.path.realpath(__file__))\n");
            confFile.println("os.system('%s/bin/nt64/pgrcmd -infile %s/test.xcf'% (eda, xcfLoc))");
        }
        catch(Exception e) {
            System.err.println("Error in creating post_process.py: " + e.getMessage());
        }
    }
    
    private void createPostProcessPy(String portAddr) {
        String path = this.TCLOC + utils.SEPARATOR + "programmer" + utils.SEPARATOR + "post_process.py";
        
        try (PrintWriter confFile = new PrintWriter(path, "UTF-8")) {
            confFile.println("# -*- coding: utf-8 -*-\n");
            confFile.println("import os\nimport platform\n");
            confFile.println("eda=os.environ['" + this.EDAENV + "']");
            confFile.println("xcfLoc=os.path.dirname(os.path.realpath(__file__))\n");
            confFile.println("os.system('%s/bin/nt64/pgrcmd -infile %s/test.xcf –cabletype “USB2” –portaddress “" + portAddr + "”'% (eda, xcfLoc))");
        }
        catch(Exception e) {
            System.err.println("Error in creating post_process.py: " + e.getMessage());
        }
    }
    
    private String getInfoFromFile(String path, String key) {
        File f = new File(path);
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            for(String l = br.readLine(); l!= null; l = br.readLine()){
                if(l.contains(key)) {
                    return utils.getValueFromDataFile(l); 
                }
            }
            br.close();
        }
        catch(Exception e) {            
        }
        return null;
    }
}
