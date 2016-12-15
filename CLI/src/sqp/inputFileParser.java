/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sqp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 * @author nmallam1
 */
public class inputFileParser {
    
    final static int PASS_CODE = 0;
    final static int FAIL_CODE = 1;
    
    int errCount = 0;
    
    final String HM_DES_KEY = "des";
    final String HM_CONF_KEY = "conf";
    final String HM_EDA_KEY = "eda";
    final String HM_LDF_KEY = "ldf";
    
    
    int BUFFER_SIZE = 100;
    
    //trunk log pass and fail keys
    String TRUNK_PASSKEY1 = "- In check_diamond_flow: True";
    
    //Programmer Log Pass and fail keys
    String PROGRAMMER_FAILKEY = "Failed.";
    String PROGRAMMER_PASSKEY1 = "Done.";
    String PROGRAMMER_PASSKEY2 = "Lattice Diamond Programmer has exited successfully.";
    String PROGRAMMER_MISSKEY = "Programming XCF Contents... Failed to Query Cable from the Lattice Cable Server.";
    
    //FTDI LOg pass and fail keys
    String FTDILOG_PASSKEY1 = "Passed";
    
    //Results Keys
    String RPASS = "Passed";
    String RFAIL = "Failed";
    String RFAIL_BITGEN = "Failed in generating bitstream";
    String RFAIL_PROGRAMMER = "Failed in Programmer";
    String RFAIL_FTDI = "Failed in stimulus package (in sending or mismatches)";
    
    File inputFile;
    String outputFileLoc;
    createHierarchy structure;
   
    //stores the absolute path
    String absPathRelative;
    String absPathOutPut;
    
    
    /*Stores the suitenames listed in the input file */
    ArrayList<String> suiteNames;  
    
    /* 
        Stores the testnames in the following format: suitename_tcname
        Example: inputfile contains suitename testSuite1 and contains the 
                 the testcase names tc1 and tc2. 
    
                They will be stored as testSuite1_tc1 and testSuite_tc2 
    */
    ArrayList<String> tcNames; 
    
    /* Stores all the testcases of a testsuite */
    HashMap <String, ArrayList<String>> HashSuiteTCMap;
    /* Store all the properties (file paths, eda) */
    HashMap <String, ArrayList<String>> HashSuiteTCPropsMap;
    /* stores if valid entry (if file exists etc.) */
    HashMap <String, Boolean> HashSuiteValidMap;
    
    HashMap <String, propStruct> HashSuiteTCPropsMap2;
    
    
    public inputFileParser(String inputFileName, String outputFileName) {
        this.inputFile = new File(inputFileName);
        this.outputFileLoc = outputFileName;
        if(!utils.checkFileExists(inputFile)) {
            System.err.println(String.format("Error: Input File : %s doesn't exist", inputFile));
            System.exit(1);
        }
        //init varaibles 
        this.absPathRelative = this.inputFile.getAbsolutePath();
        this.absPathOutPut = new File(this.outputFileLoc).getAbsolutePath();
        structure = new createHierarchy(this.outputFileLoc);
        initArrayLists();
        initHashMaps();
        //array tcs
        
        //get information from input file
        getInfo();
        
        //printMap();
        
        //create file heirarchy in output directory 
        createSuiteDirectories();
        
        //run flow: 1) generate bitgen 2) programmer 3) ftdi 
        runFlow();
    }
    
    private void createSuiteDirectories() {
        for(String s: this.suiteNames) {
            //make TestSuite Directories
            structure.createSuite(s);
            
            //make the testcase directories or suite
            ArrayList<String> tmpTcNames = this.HashSuiteTCMap.get(s);
            for(String a: tmpTcNames) {
                //get the des location
                String key = s + "_" + a;
                propStruct p = this.HashSuiteTCPropsMap2.get(key);
                String des = p.getOrigDes();
                String tcN= structure.copyTC(s, a, des);
                if(tcN != null) {
                    this.HashSuiteValidMap.put(key, true);
                }
                else {
                    this.HashSuiteValidMap.put(key, false);
                }
            }
        }
    }
    
    private void runFlow() {
        TrunkSetup1();

        System.out.println("[START] Generating Bitstream(s)");
        generateBitgen();
        TrunkSetup2();
        System.out.println("[START] Running Programmer");
        runProgrammer();
        System.out.println("[START] Sending Configuration File(s) to Board(s)");
        sendConfFile();
        
        boolean flag = createResultsLog();
        if(this.errCount > 0)
            System.out.println(String.format("%d testcases failed", this.errCount));
        else 
            System.out.println("All testcases passed!");
        
        if(flag)
            System.exit(PASS_CODE);
        else
            System.exit(FAIL_CODE);
        
        //System.exit(PASS_CODE);
    }
    
    
    private void initHashMaps() {
        this.HashSuiteTCMap = new HashMap<>();
        this.HashSuiteTCPropsMap = new HashMap<>();
        this.HashSuiteValidMap = new HashMap<>();
        this.HashSuiteTCPropsMap2 = new HashMap<>();
    }
    
    private void initArrayLists() {
        this.tcNames = new ArrayList<>();
        this.suiteNames = new ArrayList<>();
    }
    
    private boolean checkLastStringInFile(String sn, String t) {
        String prgLogLoc = this.absPathOutPut + utils.SEPARATOR + sn + utils.SEPARATOR + t + utils.SEPARATOR + "logs" + utils.SEPARATOR + "ProgrammerOutput.txt"; 
        String lastLines = utils.getLastNLinesOfFile(new File(prgLogLoc), 5);

        boolean flag1 = lastLines.contains(PROGRAMMER_PASSKEY1);
        boolean flag2 = lastLines.contains(PROGRAMMER_PASSKEY2);
        
        return (flag1 && flag2);  
    }

    private void setInfo(BufferedReader br, String key, String value, ArrayList<String> propList, propStruct prpStruct) {
        switch(key) {
            case "des":
                prpStruct.setOrigDes(value);
                prpStruct.setDes(value);
                break;
            case "conf":
                prpStruct.setConf(value);
                break;
            case "eda":
                prpStruct.setEda(value);
                break;
            case "ldf":
                prpStruct.setLdf(value);
                break;
            default:
                System.err.println(String.format("Error: Incorrect Parameter: %s in input file %s", key, this.inputFile));
                utils.closeFile(br);
                System.exit(FAIL_CODE);
        }
        
    }
    
    private void getInfo() {
        String patternTitle = "<(.*)>";
        String patternTCName = "\\*(.*)";
        String patternComment = "#(.*)";
        String pattern = "(\\w+)=(.*)";
        
        Pattern pTitle = Pattern.compile(patternTitle);
        Pattern pTCName = Pattern.compile(patternTCName);
        Pattern pComment = Pattern.compile(patternComment);
        Pattern p = Pattern.compile(pattern);
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            for(String l = br.readLine(); l!= null; l = br.readLine()){
                Matcher mTitle = pTitle.matcher(l);
                Matcher mComment = pComment.matcher(l);
                //ignore comments
                if(mComment.find())
                    continue;
                
                if(mTitle.find()) {
                    
                    String title = mTitle.group(1);
                    //add title
                    this.suiteNames.add(title);
                    
                    boolean newTestSuiteFound = false;
                    String a = br.readLine();
                    ArrayList<String> tmp = new ArrayList<>();
                    String tcName = null;
                    while(!newTestSuiteFound) {
                        //reached EOF
                        if(a == null) {
                            this.HashSuiteTCMap.put(title, tmp);
                            break;
                        }
                        
                        //skip empty lines
                        while(a.trim().length() == 0) 
                            a = br.readLine();
                        
                        
                        //check if start of new TestSuite File
                        mTitle = pTitle.matcher(a);
                        if(mTitle.find()) {
                            newTestSuiteFound = true;
                            this.HashSuiteTCMap.put(title, tmp);
                            break;
                        }

                        //next line should be a testcase name
                        mComment = pComment.matcher(a);
                        while(mComment.find()) { /*ignore comments and go to next line */
                            a = br.readLine();
                        }

                        Matcher mTCName = pTCName.matcher(a);
                        if(mTCName.find()) {
                            tcName = mTCName.group(1);
                            tmp.add(tcName);
                            this.tcNames.add(tcName);
                        }
                        else {
                            System.err.println("Error: Incorrect inputfile format\nTC title (*tcname) must follow the suitename (<suitename>) but found " + a + " instead");
                            System.exit(FAIL_CODE);
                        }
                        ArrayList<String> props = new ArrayList<>();
                        propStruct prpStruct = new propStruct(title, tcName, this.absPathOutPut, this.absPathRelative);
                        for(a = br.readLine(); a != null; a = br.readLine()) {
                            //skip empty lines
                            while(a.length() == 0) {
                                a = br.readLine();
                            }
                            
                            //skip comments
                            mComment = pComment.matcher(a);
                            while(mComment.find()) { /*ignore comments and go to next line */
                                a = br.readLine();
                            }
                            
                            
                            Matcher m = p.matcher(a);
                            if(m.find()) {
                                //System.out.println(String.format("%s_%s:%s -> %s", title, tcName, m.group(1), m.group(2)));
                                setInfo(br, m.group(1), m.group(2), props, prpStruct);
                                br.mark(BUFFER_SIZE);
                            }
                            else {
                                break;
                            }
                        }
                        String nameKey = title + "_" + tcName;
                        this.HashSuiteTCPropsMap.put(nameKey, props);
                        this.HashSuiteTCPropsMap2.put(nameKey, prpStruct);
                    }
                    br.reset();
                    
                }
            }
            br.close();
        }

        catch(Exception e) {
            e.printStackTrace();
            //System.err.println("Error with inputFileParser/getInfo : " + e.getMessage());
            System.exit(FAIL_CODE);
        }
        
    }
    
    private void TrunkSetup1() {
        for(String s: this.suiteNames) {
            ArrayList<String> tmpTcNames = this.HashSuiteTCMap.get(s);
            for(String t: tmpTcNames) {
                String k = s + "_" + t;
                System.out.println(k);
                if(this.HashSuiteValidMap.get(k)) {
                    //ArrayList<String> props = this.HashSuiteTCPropsMap.get(k);
                    propStruct props = this.HashSuiteTCPropsMap2.get(k);
                    trunkSetup ts = new trunkSetup(this.outputFileLoc, s, t, props);
                    //trunkSetup ts = new trunkSetup(this.absPathRelative, s, t, props);
                    ts.createFiles();
                }
            }
        }
    }
    
    private void TrunkSetup2() {
        for(String s: this.suiteNames) {
            ArrayList<String> tmpTcNames = this.HashSuiteTCMap.get(s);
            boolean hasDaisyChain = (tmpTcNames.size() > 1);
            
            //HashMap<String, ArrayList<String>> listTCProps = getPropListFromTCs(s, tmpTcNames);
            HashMap<String, propStruct> listTCProps = getPropListFromTCs(s, tmpTcNames);
            boolean firstTestCaseDaisy = true;
            int l = 0;
            for(String t: tmpTcNames) {
                if(l != 0)
                    firstTestCaseDaisy = false;
                String k = s + "_" + t;
                if(this.HashSuiteValidMap.get(k) && firstTestCaseDaisy) {
                    //ArrayList<String> props = this.HashSuiteTCPropsMap.get(k);
                    propStruct props = this.HashSuiteTCPropsMap2.get(k);
                    trunkSetup ts = new trunkSetup(this.outputFileLoc, s, t, props);
                    if(hasDaisyChain) 
                        ts.setHasDaisyChain(hasDaisyChain, tmpTcNames, tmpTcNames.size(), listTCProps, firstTestCaseDaisy);
                    ts.createProgrammerFiles();
                }
                l++;
            }
        }
    }
    
    private HashMap<String, propStruct> getPropListFromTCs(String suiteName, ArrayList<String> tmpTcNames) {
        //HashMap<String, ArrayList<String>> map = new HashMap<>();
        HashMap<String, propStruct> map = new HashMap<>();
        
        for(String t: tmpTcNames) {
            String key = suiteName + "_" + t;
            propStruct props = this.HashSuiteTCPropsMap2.get(key);
            map.put(key, props);
        }
        return map;
    } 
    
    private void generateBitgen() {
        ExecutorService executor = Executors.newCachedThreadPool();
//        for(String s: this.outDirs) {
//            genBit g = new genBit(s, this.tcNames.get(i), this.edaLocs.get(i));
////            Runnable r = new Runnable() {
////                public void run() {
////                    g.run();
////                }
////            };
//        }
        for(String s: this.suiteNames) {
            ArrayList<String> tmpTcNames = this.HashSuiteTCMap.get(s);
            for(String t: tmpTcNames) {
                String key = s+"_"+t;
                //ArrayList<String> props = this.HashSuiteTCPropsMap.get(key);
                propStruct props = this.HashSuiteTCPropsMap2.get(key);
                genBit g = new genBit(s,t,props, this.absPathOutPut);
                Runnable r = g::run;
                executor.submit(r);
            }
        }
        
        //wait for all threads to finish
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            System.err.println("error in waiting for bitgen threads to finish " + e.getMessage());
        }
        
        for(String s: this.suiteNames) {
            ArrayList<String> tmpTcNames = this.HashSuiteTCMap.get(s);
            for(String t: tmpTcNames) {
                String key = s+"_"+t;
                propStruct props = this.HashSuiteTCPropsMap2.get(key);
                String scratchLoc = this.absPathOutPut + utils.SEPARATOR + s + utils.SEPARATOR + t + utils.SEPARATOR + "_scratch";
                if(utils.checkFileDir(new File(scratchLoc)))
                    props.setPassBitgen();
            }
        }
        
        System.out.println("[DONE] Generating Bitstream(s)");
    }
    
    
    
    
    
    private void setPassProgrammerSuper(String sN) {
        ArrayList<String> tmpTcNames = this.HashSuiteTCMap.get(sN);
        for(String t: tmpTcNames) {
            String key = sN + "_" + t;
            propStruct p = this.HashSuiteTCPropsMap2.get(key);
            p.setPassProgrammer();
        }
    }
    
    private void runProgrammer() {
        for(String s: this.suiteNames) {
            ArrayList<String> tmpTcNames = this.HashSuiteTCMap.get(s);
            boolean moreThanOneTestCasesFlag = true;
            for(String t: tmpTcNames) {
                if(moreThanOneTestCasesFlag) {
                    String key = s + "_" + t;
                    propStruct props = this.HashSuiteTCPropsMap2.get(key);
                    if(!props.getPassBitgen()) {
                        System.out.println(String.format("\t  Skipping Programmer for %s/%s - Bitgen Failed", s, t));
                        continue;
                    }
                    System.out.println(String.format("\t  Running Programmer for %s/%s", s, t));
                    ExecutorService executor = Executors.newCachedThreadPool();
                    Programmer p = new Programmer(s, t, this.absPathOutPut, false);
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
                    
                    
                    //check if programmer failed, rerun on different ports if it did fail. 
                    String prgLogLoc = this.absPathOutPut + utils.SEPARATOR + s + utils.SEPARATOR + t + utils.SEPARATOR + "logs" + utils.SEPARATOR + "ProgrammerOutput.txt";
                    boolean flag = utils.checkStringInFile(PROGRAMMER_MISSKEY, prgLogLoc);
                    boolean flagFail;
                    if(flag) {
                        moreThanOneTestCasesFlag = false;
                        System.err.println("\t\tERROR: Programmer Cable not connected");
                        continue;
                    }
                    else {
                        flagFail = utils.checkStringInFile(PROGRAMMER_FAILKEY, prgLogLoc);
                    }
                    if(flagFail) {
                        System.out.println(String.format("\t[NOTE]: Rerunning Programmer for %s/%s",s,t));
                        Programmer rp = new Programmer(s, t, this.absPathOutPut, true);
                        String [] portList = rp.getPortList();
                        for(int k = 0; k < portList.length; k++) {
                            //String key = s + "_" + t;
                            //ArrayList <String> props = this.HashSuiteTCPropsMap.get(key);
                            //propStruct props = this.HashSuiteTCPropsMap2.get(key);
                            ExecutorService Rexecutor = Executors.newCachedThreadPool();
                            System.out.println("\t\t:-Rerunning on port " + portList[k]);
                            //rp.modifyProgrammerScript(utils.getEDAFromList(props), portList[k]);
                            rp.modifyProgrammerScript(props.getEda(), portList[k]);
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

                            if(checkLastStringInFile(s, t)) {
                                setPassProgrammerSuper(s);
                                break;
                            }

                        }
                    }
                    else {
                        setPassProgrammerSuper(s);
                    }
                    
                    moreThanOneTestCasesFlag = false;
                }
            }
        }
   
        System.out.println("[DONE] Running Programmer");
    }
    
    
    
    
  
    private void sendConfFile() {
        for(String s: this.suiteNames) {
            ArrayList<String> tmpTcNames = this.HashSuiteTCMap.get(s);
            
            for(String t: tmpTcNames) {
                ExecutorService executor = Executors.newCachedThreadPool();
                String key = s + "_" + t;
                propStruct props = this.HashSuiteTCPropsMap2.get(key);
                if(!props.getPassProgrammer()) {
                    System.out.println(String.format("\tSkipping sending packets to %s/%s - Programmer Failed", s ,t));
                    continue;
                }
                Ftdi f = new Ftdi(s, t, this.absPathOutPut, props.getConf());
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
            }
        }
        
        System.out.println("[DONE] Sending Configuration File(s) to Board(s)");
    }
    
    private boolean createResultsLog() {
        String path = this.absPathOutPut + utils.SEPARATOR + "results.txt";
        try(PrintWriter logFile = new PrintWriter(path, "UTF-8")) {
            for(String s: this.suiteNames) {
                ArrayList<String> tmpTcNames = this.HashSuiteTCMap.get(s);
                logFile.println(s);
                boolean manyTestCases = tmpTcNames.size() > 1;
                for(String t: tmpTcNames) {
                    String logPath = this.absPathOutPut + utils.SEPARATOR + s + utils.SEPARATOR + t + utils.SEPARATOR + "logs";
                    boolean PassOrFail = utils.checkStringInFile(this.TRUNK_PASSKEY1, logPath + utils.SEPARATOR + "trunkLog.txt");
                    if(PassOrFail) {
                        String newLogPath;
                        if(manyTestCases) {
                            newLogPath = this.absPathOutPut + utils.SEPARATOR + s + utils.SEPARATOR + tmpTcNames.get(0) + utils.SEPARATOR + "logs";
                        }
                        else {
                            newLogPath = logPath;
                        }
                        PassOrFail = utils.checkStringInFile(this.PROGRAMMER_PASSKEY1, newLogPath + utils.SEPARATOR + "ProgrammerOutput.txt") 
                                     && utils.checkStringInFile(this.PROGRAMMER_PASSKEY2, newLogPath + utils.SEPARATOR + "ProgrammerOutput.txt");
                        if(PassOrFail) {
                            String simOutPutFile = this.absPathOutPut + utils.SEPARATOR + s + utils.SEPARATOR + t 
                                                   + utils.SEPARATOR + "sim" + utils.SEPARATOR + "output_files" + utils.SEPARATOR + "output.txt";
                            if(utils.checkFileExists(new File(simOutPutFile)))
                                PassOrFail = utils.checkStringInFile(this.FTDILOG_PASSKEY1, simOutPutFile);
                            else 
                                PassOrFail = false;
                            if(PassOrFail) {
                                logFile.println(String.format("\t%s:%s", t, this.RPASS));
                            }
                            else {
                                this.errCount++;
                                logFile.println(String.format("\t%s:%s (%s)", t, this.RFAIL, this.RFAIL_FTDI));  
                            }
                        }
                        else {
                            this.errCount++;
                            logFile.println(String.format("\t%s:%s (%s)", t, this.RFAIL, this.RFAIL_PROGRAMMER));
                        }
                    }
                    else {
                        this.errCount++;
                        logFile.println(String.format("\t%s:%s (%s)", t, this.RFAIL, this.RFAIL_BITGEN));
                    }
                }
                logFile.println();
            }
            logFile.close();
        }
        catch(Exception e) {
            System.err.println("Error creating results.txt");
        }
        
        if(this.errCount > 0)
            return false;
        else
            return true;
    }
    
    @Override
    public String toString() {
       String tmp = "";
       for(String s: this.suiteNames)
           tmp = tmp + s + ";";
       return tmp;
    }
    
}
