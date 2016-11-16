/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.runFlow;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import solution.quality.platform.fileCheck;
import solution.quality.platform.utils;

/**
 *
 * @author nmallam1
 */
public class runFlow {
    
    
    final private String FILE_BITGEN = "RBT_YELLOW";
    final private String FILE_BITGEN_PASS = "RBT_GREEN";
    final private String FILE_BITGEN_FAIL = "RBT_RED";
    
    final private String FILE_PRG = "PRG_YELLOW";
    final private String FILE_PRG_PASS = "PRG_GREEN";
    final private String FILE_PRG_FAIL = "PRG_RED";
    
    final private String FILE_FTDI = "FTDI_YELLOW";
    final private String FILE_FTDI_PASS = "FTDI_GREEN";
    final private String FILE_FTDI_FAIL = "FTDI_RED";
   
    
    
    
    final private String TCLOC;
    final private String LDFLOC;
    final private String EDA;
    final private String CONFLOC;
    
    final private String SQPDATALOC;
    
    
    public runFlow(String TCLOC, String LDFLOC, String EDA, String CONFLOC) {
        this.TCLOC = TCLOC;
        this.LDFLOC = LDFLOC;
        this.EDA = EDA;
        this.CONFLOC = CONFLOC;
        
        this.SQPDATALOC = this.TCLOC + utils.SEPARATOR + "sqpData";
        
    }
    
    
    public void run(boolean hasDaisy, boolean isFirstTC, String [] tcs) {
        //remove previous status file
        fileCheck.cleanStatusFiles(TCLOC);
        
        
        fileCheck.createBitgenFile_A(TCLOC);
        runBitgen();
        boolean flag = fileCheck.checkBitGen(TCLOC);
        if(flag)
            fileCheck.createBitgenFile_C(TCLOC);
        else
            fileCheck.createBitgenFile_B(TCLOC);
        
        
        
        if(flag) {
            if(hasDaisy) {
                if(isFirstTC) {
                    trunckSetupC tC = new trunckSetupC(this.TCLOC, this.LDFLOC, this.EDA);
                    tC.createProgrammerHeirarchy(hasDaisy, tcs);
                    fileCheck.cleanBitgenFiles(TCLOC);
                    fileCheck.createPrgFile_A(TCLOC);
                    runProgrammer();
                    flag = fileCheck.checkProgrammer(TCLOC);
                    if(flag)
                        fileCheck.createPrgFile_C(TCLOC);
                    else
                        fileCheck.createPrgFile_B(TCLOC);
                }
            }
            else {
                trunckSetupC tC = new trunckSetupC(this.TCLOC, this.LDFLOC, this.EDA);
                tC.createProgrammerHeirarchy(hasDaisy, null);
                fileCheck.cleanBitgenFiles(TCLOC);
                fileCheck.createPrgFile_A(TCLOC);
                runProgrammer();
                flag = fileCheck.checkProgrammer(TCLOC);
                if(flag)
                    fileCheck.createPrgFile_C(TCLOC);
                else
                    fileCheck.createPrgFile_B(TCLOC);
                
            }
        }
        
        if(flag) {
            fileCheck.cleanPrgFiles(TCLOC);
            fileCheck.createFTDIFile_A(TCLOC);
            sendConfFile();
            flag = fileCheck.checkFtdi(TCLOC);
            if(flag)
                fileCheck.createFTDIFile_C(TCLOC);
            else
                fileCheck.createFTDIFile_B(TCLOC);
        }
        
//        if(flag) 
//            fileCheck.cleanFTDIFiles(TCLOC);
        
    }
    
    
    
    
    private void runBitgen() {
        ExecutorService executor = Executors.newCachedThreadPool();
        genBitgen rbt = new genBitgen(this.TCLOC, this.EDA);
        Runnable r = rbt::run;
        executor.submit(r);
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            System.err.println("error in waiting for bitgen threads to finish " + e.getMessage());
        }
    }
    
    
    private void runProgrammer() {
        ExecutorService executor = Executors.newCachedThreadPool();
        Programmer p = new Programmer(this.TCLOC, false);
        Runnable r = p::run;
        executor.submit(r);

        //wait for thread to finish
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            System.err.println("error in waiting for programmer threads to finish " + e.getMessage());
        }

        //wait for that thread to finish
        while(!executor.isShutdown()) {

        }

        try {
            Thread.sleep(1000);  
        }
        catch (Exception e) {
            System.err.println("Failed to pause when running programmer multiple times");
        }

        //recheck Programmer
        String prgLogLoc = this.TCLOC + utils.SEPARATOR +  "logs" + utils.SEPARATOR + "ProgrammerOutput.txt";
        boolean flag = fileCheck.checkStringInFile(fileCheck.PROGRAMMER_MISSKEY, prgLogLoc);
        boolean flagFail = false;
        if(flag) {
            System.err.println("\t\tERROR: Programmer Cable not connected");
        }
        else {
            flagFail = fileCheck.checkStringInFile(fileCheck.PROGRAMMER_FAILKEY, prgLogLoc);
        }
        if(flagFail) {
            System.out.println(String.format("\t[NOTE]: Rerunning Programmer for %s",this.TCLOC));
            Programmer rp = new Programmer(this.TCLOC, true);
            String [] portList = rp.getPortList();
            for(int k = 0; k < portList.length; k++) {

                ExecutorService Rexecutor = Executors.newCachedThreadPool();
                System.out.println("\t\t:-Rerunning on port " + portList[k]);
                rp.modifyProgrammerScript(this.EDA, portList[k]);
                Runnable r2 = rp::run;
                Rexecutor.submit(r2);

                //wait for programmer rerun threads to finish 
                Rexecutor.shutdown();
                try {
                    Rexecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
                }
                catch (InterruptedException e) {
                    System.err.println("error in waiting for programmer threads to finish " + e.getMessage());
                }

                //wait for that thread to finish
                while(!Rexecutor.isShutdown()) {
                }

                if(fileCheck.checkLastStringInFile(prgLogLoc)) {
                    break;
                }

            }
        }
        else {
            System.out.println("Passed Programmer");
        }
    }
    
    private void sendConfFile() {
        
        ExecutorService executor = Executors.newCachedThreadPool();

        ftdi f = new ftdi(this.TCLOC, this.CONFLOC);
        Runnable r = f::run;
        executor.submit(r);

        //wait for all the ftdi.exe threads to finish
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            System.err.println("error in waiting for programmer threads to finish " + e.getMessage());
        }

        //wait for that thread to finish
        while(!executor.isShutdown()) {

        }

        try {
            Thread.sleep(1000);  
        }
        catch (Exception e) {
            System.err.println("Failed to pause when running programmer multiple times");
        }   
       
        
        System.out.println("[DONE] Sending Configuration File(s) to Board(s)");
    }
    
}
