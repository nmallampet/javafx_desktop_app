/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sqp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 *
 * @author nmallam1
 */
public class Programmer {
    
    final String separator = File.separator;
    
    int PORT_SIZE = 15;
    
    
    String programmerLoc;
    String xcfLoc;
    String pyLoc;
    
    String programmerLogLoc;
    boolean appendToLog;
    
    String port;
    
    
    
    
    public Programmer(String suiteName, String tcName, String absPath, boolean modPy) {
        //this.programmerLoc = testCaseLoc + "\\programmer";
        String tcPath = absPath + separator + suiteName + separator + tcName;
        this.programmerLoc = tcPath + separator + "programmer";
        this.xcfLoc = this.programmerLoc + separator + "test.xcf";
        this.appendToLog = modPy;
        if(modPy) 
            this.pyLoc = this.programmerLoc + separator + "mod_post_process.py";
        else 
            this.pyLoc = this.programmerLoc + separator + "post_process.py";
        
        this.programmerLogLoc = tcPath + separator + "logs" + separator + "ProgrammerOutput.txt";
        //System.out.println(String.format("TestcaseLoc = %s\nPyLog = %s\n", testCaseLoc, this.programmerLogLoc));
    }
    
    public void run() {
        try(PrintWriter logFile = new PrintWriter(new BufferedWriter(new FileWriter(this.programmerLogLoc, this.appendToLog)))) {
            ProcessBuilder builder = new ProcessBuilder("python", "-u", this.pyLoc);
            builder.inheritIO().redirectOutput(ProcessBuilder.Redirect.PIPE);
            Process process = builder.start();
            if(this.appendToLog) {
                logFile.println(String.format("------------PROGRAMMER RERUN FOR %s--------------", this.port));
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                        reader.lines().forEach((line) -> {
                            if(line != null) {
                                logFile.println(line);
                                //System.out.println(line);
                            }
                        });
            }
            
            catch(Exception ex) {
                System.err.println("Error in writing to programmer log file " + ex.getMessage());
            }
            finally {
                
                logFile.println();
                logFile.close();
            }
        }
        catch (Exception e) {
            System.err.println("Error in running programmer for " + this.programmerLoc + ": " + e.getMessage());
        }
    }
    
    
    private void modifyXCFFile(String newPortAddr) {
        XCFParser.setPortAdd(newPortAddr, new File(this.xcfLoc));
    }
    
    private String getEnvLocation(String eda) {
        return edaParser.getEnvFromEda(eda);
    }
    
    private void createModPostProcessPy(String newPortAddr, String edaLoc) {
        try (PrintWriter confFile = new PrintWriter(this.pyLoc, "UTF-8")) {
            confFile.println("# -*- coding: utf-8 -*-\n");
            confFile.println("import os\nimport platform\n");
           
            String eda = getEnvLocation(edaLoc);
            confFile.println("eda=os.environ['" + eda + "']");
            confFile.println("xcfLoc=os.path.dirname(os.path.realpath(__file__))\n");
            confFile.println("os.system('%s/bin/nt64/pgrcmd -infile %s/test.xcf –cabletype “USB2” –portaddress “" + newPortAddr + "”'% (eda, xcfLoc))");
        }
        catch(Exception e) {
            System.err.println("Error in creating post_process.py: " + e.getMessage());
        }
    }
    
    public void modifyProgrammerScript(String edaLoc, String newPortAddr) {
        if(this.appendToLog) {
            //String origPortAddr = XCFParser.getPortNum(new File(this.xcfLoc));
            //String newPortAddr = (origPortAddr.equals("FTUSB-0")) ? "FTUSB-1":"FTUSB-0";
            this.port = newPortAddr;
            modifyXCFFile(newPortAddr);
            createModPostProcessPy(newPortAddr, edaLoc);
        }
    }
    
    public String [] getPortList() {
        String [] ports = {"FTUSB-0", "FTUSB-1", "FTUSB-2", "FTUSB-3", "FTUSB-4", "FTUSB-5", "FTUSB-6", "FTUSB-7", "FTUSB-8",
                            "FTUSB-9", "FTUSB-10", "FTUSB-11", "FTUSB-12", "FTUSB-13", "FTUSB-14", "FTUSB-15"}; 
        //String [] ports = {"FTUSB-4", "FTUSB-2", "FTUSB-1", "FTUSB-3", "FTUSB-0"};
        String [] newPortList = new String[PORT_SIZE];
        
        
        this.port = XCFParser.getPortNum(new File(this.xcfLoc));
        
        //String [] newPortList = new String[15];
        int j = 0;
        for(int i = 0; i < ports.length; i++) {
            if(!port.equals(ports[i])) {
                newPortList[j] = ports[i];
                j++;
            }
        }
        
        return newPortList;
    }
    
    
}
