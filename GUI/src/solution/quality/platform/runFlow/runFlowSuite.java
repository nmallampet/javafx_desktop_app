/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.runFlow;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import solution.quality.platform.fileCheck;
import solution.quality.platform.utils;

/**
 *
 * @author nmallam1
 */
public class runFlowSuite {
    
    
    final private ArrayList<String> TCLOCS;
    final private ArrayList<String> LDFLOCS;
    final private ArrayList<String> EDAS;
    final private ArrayList<String> CONFLOCS;
    
    private String [] tcs;
    
    
    public runFlowSuite(ArrayList<String> TCLOCS, ArrayList<String> LDFLOCS, ArrayList<String> EDAS, ArrayList<String> CONFLOCS) {
        this.TCLOCS = TCLOCS;
        this.LDFLOCS = LDFLOCS;
        this.EDAS = EDAS;
        this.CONFLOCS = CONFLOCS;
    }
    
    
    public void run(String[] tcs) {
        for(String s: this.TCLOCS)
            fileCheck.cleanStatusFiles(s);
        
        this.tcs = tcs;
        boolean flag = runBitgen();
        if(flag) {
            flag = programmerSetUp();
            if(flag)
                sendConfFile();
            
        }
    }
    
    
    private boolean runBitgen() {
        int i = 0;
        boolean flag;
        boolean finalFlag = true;
        for(String s: this.TCLOCS) {
            fileCheck.cleanStatusFiles(s);
            fileCheck.createBitgenFile_A(s);
            String EDA = this.EDAS.get(i);
            ExecutorService executor = Executors.newCachedThreadPool();
            genBitgen rbt = new genBitgen(s, EDA);
            Runnable r = rbt::run;
            executor.submit(r);
            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            }
            catch (InterruptedException e) {
                System.err.println("error in waiting for bitgen threads to finish " + e.getMessage());
            }
            
            flag = fileCheck.checkBitGen(s);
            if(flag)
                fileCheck.createBitgenFile_C(s);
            else {
                fileCheck.createBitgenFile_B(s);
                finalFlag = false;
            }
            i++;
        }
        
        return finalFlag;
    }
    
    private boolean programmerSetUp() {
        int i = 0;
        //String [] tcs;
        boolean flag;
        boolean finalFlag = true;
        for(String s: this.TCLOCS) {
            String EDA = this.EDAS.get(i);
            String LDFLOC = this.LDFLOCS.get(i);
            boolean hasDaisy = this.TCLOCS.size() > 1;
            boolean isFirstTC = (i == 0);
            if(hasDaisy) {
                if(isFirstTC) {
                    String path = new File(s).getParent();
                    //tcs = utils.getListofFiles(new File(path));
                    trunckSetupC tC = new trunckSetupC(s, LDFLOC, EDA);
                    tC.createProgrammerHeirarchy(hasDaisy, this.tcs);
                    
                    fileCheck.cleanBitgenFiles(s);
                    fileCheck.createPrgFile_A(s);
                    fileCheck.daisyCreatePrgFiles_A(this.TCLOCS);
                    
                    runProgrammer(s, EDA);
                    
                    flag = fileCheck.checkProgrammer(s);
                    if(flag) {
                        fileCheck.createPrgFile_C(s);
                        fileCheck.daisyPrgFiles(this.TCLOCS, flag);
                    }
                    else {
                        fileCheck.createPrgFile_B(s);
                        fileCheck.daisyPrgFiles(this.TCLOCS, flag);
                        finalFlag = false;
                    }
                }
            }
            else {
                trunckSetupC tC = new trunckSetupC(s, LDFLOC, EDA);
                tC.createProgrammerHeirarchy(hasDaisy, null);
                
                fileCheck.cleanBitgenFiles(s);
                fileCheck.createPrgFile_A(s);
                    
                runProgrammer(s,EDA);
                flag = fileCheck.checkProgrammer(s);
                if(flag)
                    fileCheck.createPrgFile_C(s);
                else {
                    fileCheck.createPrgFile_B(s);
                    finalFlag = false;
                }
            }
            i++;
        }
        return finalFlag;
    }
    
    private void runProgrammer(String TCLOC, String EDA) {
        ExecutorService executor = Executors.newCachedThreadPool();
        Programmer p = new Programmer(TCLOC, false);
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
        String prgLogLoc = TCLOC + utils.SEPARATOR +  "logs" + utils.SEPARATOR + "ProgrammerOutput.txt";
        boolean flag = fileCheck.checkStringInFile(fileCheck.PROGRAMMER_MISSKEY, prgLogLoc);
        boolean flagFail = false;
        if(flag) {
            System.err.println("\t\tERROR: Programmer Cable not connected");
        }
        else {
            flagFail = fileCheck.checkStringInFile(fileCheck.PROGRAMMER_FAILKEY, prgLogLoc);
        }
        if(flagFail) {
            System.out.println(String.format("\t[NOTE]: Rerunning Programmer for %s",TCLOC));
            Programmer rp = new Programmer(TCLOC, true);
            String [] portList = rp.getPortList();
            for(int k = 0; k < portList.length; k++) {

                ExecutorService Rexecutor = Executors.newCachedThreadPool();
                System.out.println("\t\t:-Rerunning on port " + portList[k]);
                rp.modifyProgrammerScript(EDA, portList[k]);
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
    
    
    private boolean sendConfFile() {
        int i = 0;
        boolean flag;
        boolean finalFlag = true;
        for(String s: this.TCLOCS) {
            fileCheck.cleanPrgFiles(s);
            fileCheck.createFTDIFile_A(s);
            String CONFLOC = this.CONFLOCS.get(i);
            ExecutorService executor = Executors.newCachedThreadPool();
            ftdi f = new ftdi(s, CONFLOC);
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
            
            flag = fileCheck.checkFtdi(s);
            if(flag)
                fileCheck.createFTDIFile_C(s);
            else {
                fileCheck.createFTDIFile_B(s);
                finalFlag = false;
            }
           
            i++;
        }
        return finalFlag;
    }
    
}
