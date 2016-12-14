/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import solution.quality.platform.dataStructures.casesStruct;
import solution.quality.platform.saveXML.sqpXML;

/**
 *
 * @author nmallam1
 */
public class utils {
     /*
     * This class contains all the commonly used functions
     */
    
    final public static String SEPARATOR = File.separator;
    
    final public static String DIAMOND_ENV = "EXTERNAL_DIAMOND_PATH";
    final public static String ICECUBE_ENV = "EXTERNAL_ICECUBE_PATH";
    
    final public static String LISTVIEW_SEP_KEY = "%#%";
    
    
    /* FILE IO FUNCTIONS */
    static public boolean createFile(String path) {
        File file = new File(path);
        try {
            if(file.createNewFile())
                return true;
        }
        catch(Exception e) {
            return false;
        }
        return true;
    }
    static public boolean deleteFile(String path) {
        File file = new File(path);
        try {
            if(file.delete())
                return true;
        }
        catch(Exception e) {
            
        }
        return false;
    }
    static public boolean checkFileExists(File fileName) {
        return fileName.exists();
    }
    static public boolean checkFileDir(File fileName) {
        return fileName.isDirectory();
    }
    static public boolean checkFileInUse(File file) {
        boolean isFileUnlocked;
        try {
            org.apache.commons.io.FileUtils.touch(file);
            isFileUnlocked = true;
        } catch (IOException e) {
            isFileUnlocked = false;
        }
        return isFileUnlocked;
    }
    static public boolean createDirectory(String path) {
        File dir = new File(path);
        if(!dir.exists()) {
            try {
                dir.mkdir();
            }
            catch(SecurityException e) {
                System.err.println("Error in creating Directory: " + dir.getName());
                return false;
            }
        }
        return true;
    }
    static public boolean deleteDirectory(File directory) {
        if(directory.exists()){
            File[] files = directory.listFiles();
            if(null!=files){
                for(int i=0; i<files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    }
                    else {
                        files[i].delete();
                    }
                }
            }
        }
        return(directory.delete());
    }
    static public String[] getListofFiles(File file) {
        String[] directories = file.list((File current, String name) -> new File(current, name).isDirectory());
        return directories;
    }
    
    /*Trunk Script related Functions*/
    public static String getJarLocation() {
        try {
            return new File(SystemsQualityPlatform.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();
        }
        catch (Exception e) {
            System.err.println("pls wtf");
        }
        return null;
    }
    
    
    static public String getCurrentDate() {
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        Date date = new Date();  
        return dateFormat.format(date);
    }
    
    
    
    static public void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    static public boolean promptYesOrNo(String title, String header, String content) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK)
            return true;
        else 
            return false;
        
    }
    
    /*
        EDA Functions
    */
    static public String getEnvFromEda(String eda) {
        switch(eda) {
            case "diamond":
                return DIAMOND_ENV;
               
            case "icecube":
                return ICECUBE_ENV;
                
            default: 
                return null;
        }
    }
    static public String getEDAEnvLocation(String eda) {
        return System.getenv(eda);
    }

    static public String getFullLDFLoc(String path, String data) {
        if(utils.checkFileExists(new File(data)))
            return data;
        else
            return path + utils.SEPARATOR + data;
    }
    
    
    /*
        sqpData/data.txt functions
    */
    static public void writeToSQPDataFile(String key, String path, String newKey) {
        ArrayList<String> oldInfo = new ArrayList<>();
        boolean keyFound = false;
        
        //read file and get contents
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            for(String l = br.readLine(); l!= null; l = br.readLine()){
                if(l.contains(key)) {
                    keyFound = true;
                    oldInfo.add(newKey);
                }
                else {
                    oldInfo.add(l);
                }
            }
            br.close();
        }
        catch(Exception e) {            
        }
        
        if(!keyFound)
            oldInfo.add(newKey);
        
        //write to file
        try (PrintWriter dataFile = new PrintWriter(path, "UTF-8")) {
            for(String s: oldInfo)
                dataFile.println(s);
        }
        catch(Exception e) {
            System.err.println("Error in writing to data file " + e.getMessage());
        }
        
    }
    static public String getValueFromDataFile(String key) {
        if(key.contains("conf"))
            return key.substring(5);
        else
            return key.substring(4);
    }
    static public void generateInputFile(ArrayList<String> suiteNames, String location, sqpXML PROJMETA) {
        String path = location + utils.SEPARATOR + "suites";
        for(String s: suiteNames) {
            ArrayList<String> fileData = new ArrayList<>();
            fileData.add("<"+s+">");
            String pathT = path + utils.SEPARATOR + s;
            //File files = new File(pathT);
            //String [] directories = files.list((File current, String name) -> new File(current, name).isDirectory());
            ArrayList<String> directories1 = getListofTCsFromSuite(s, PROJMETA);
            String[] tcs = directories1.parallelStream().toArray(String[]::new);
            for(String t: tcs) {
                fileData.add("*"+t);
                String dataFilePath = pathT + utils.SEPARATOR + t + utils.SEPARATOR + "sqpData" + utils.SEPARATOR + "data.txt";
                try {
                    BufferedReader br = new BufferedReader(new FileReader(dataFilePath));
                    for(String l = br.readLine(); l!= null; l = br.readLine()){
                        fileData.add(l);
                    }
                    br.close();
                }
                catch(Exception e) {            
                }
            }
            //write to file
            try (PrintWriter dataFile = new PrintWriter(pathT + utils.SEPARATOR + "input.txt", "UTF-8")) {
                for(String d: fileData)
                    dataFile.println(d);
            }
            catch(Exception e) {
                System.err.println("Error in writing to data file " + e.getMessage());
            }
        }
    }
    
    /*DeviceInfo*/
    static public String XPGAFamilyLUT(String deviceName) {
        //ecp5, macgyver, snow, x03
        //ecp5
        String pattern = "L[A-Z]E5";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(deviceName);
        if(m.find())
            return "ecp5";
        
        //ecp3
        pattern = "L[A-Z]E3";
        p = Pattern.compile(pattern);
        m = p.matcher(deviceName);
        if(m.find())
            return "ecp3";
        
        //ecp2
        pattern = "L[A-Z]E2";
        p = Pattern.compile(pattern);
        m = p.matcher(deviceName);
        if(m.find())
            return "ecp2";
        
        //ecp
        pattern = "L[A-Z]EC";
        p = Pattern.compile(pattern);
        m = p.matcher(deviceName);
        if(m.find())
            return "ecp";
        
        //macgyver
        pattern = "SII[0-9]+";
        p = Pattern.compile(pattern);
        m = p.matcher(deviceName);
        if(m.find())
            return "macgyver";
        
        //xo3
        pattern = "L[A-Z]MX03";
        p = Pattern.compile(pattern);
        m = p.matcher(deviceName);
        if(m.find())
            return "xo3";
        
        //xo2
        pattern = "L[A-Z]MX02";
        p = Pattern.compile(pattern);
        m = p.matcher(deviceName);
        if(m.find())
            return "xo2";
        
        //machxo
        pattern = "L[A-Z]MXO";
        p = Pattern.compile(pattern);
        m = p.matcher(deviceName);
        if(m.find())
            return "machxo";

        //snow
        pattern = "LIF-";
        p = Pattern.compile(pattern);
        m = p.matcher(deviceName);
        if(m.find())
            return "snow";
        
        
        return null;
    }
    
    /*sqpData [A-Z]{3}[A-Z]?_[A|B|C] status parser */
    static public int getStatusFromTC(String loc) {
        String path = loc + utils.SEPARATOR + "sqpData";
        File [] files = new File(path).listFiles();
        //System.out.println(loc + "\n" + path + "\nOMG");
        String fPattern = "([A-Z]{3}[A-Z]?)_([A|B|C])";
        Pattern fPPattern = Pattern.compile(fPattern);
        for(File f: files) {
            String s = f.getName();
            Matcher m = fPPattern.matcher(s);
            if(m.matches()) {
                if(m.group(2).equals("B"))
                    return 1;
                if(m.group(2).equals("C")) {
                    if(m.group(1).equals("FTDI"))
                        return 2;
                }
                break;
            }
        }
        return 0;
    }
    
    static public List<String> getSuiteStatusList(sqpXML PROJMETA, String loc) {
        String path = loc + utils.SEPARATOR + "suites" + utils.SEPARATOR;
        
        List<String> statusList = new ArrayList<>();
        List<casesStruct> list = PROJMETA.getSuites();
        
        for(casesStruct c : list) {
            String sName = c.getSuiteName();
            int num = 0;
            String path1 = path + sName + utils.SEPARATOR;
            ArrayList<String> tcs = c.getTestCases();
            for(String s: tcs) {
                String path2 = path1 + s;
                num = utils.getStatusFromTC(path2);
                if(num < 2)
                    break;
            }
            String key = sName + utils.LISTVIEW_SEP_KEY + Integer.toString(num);
            statusList.add(key);
        }
        
        return statusList;
    }
    
    static public void setNewList(sqpXML PROJMETA, List<String> newList, String sname) {
        List<casesStruct> list = PROJMETA.getSuites();
        
        for(casesStruct c : list) {
            String sName = c.getSuiteName();
            int num = 0;
            if(sName.equals(sname)) {
                ArrayList<String> tcs = new ArrayList<>(newList);
                c.setTestCases(tcs);
            }
        }
    }
    
    
    static public List<String> getTCStatusList(String name, sqpXML PROJMETA, String loc) {
        
        String sName = name.split(utils.LISTVIEW_SEP_KEY)[0];
        
        String path = loc + utils.SEPARATOR + "suites" + utils.SEPARATOR + sName + utils.SEPARATOR;
        
        List<String> statusList = new ArrayList<>();
        List<casesStruct> list = PROJMETA.getSuites();
        
        for(casesStruct c: list) {
            if(c.getSuiteName().equals(sName)) {
                ArrayList<String> tcs = c.getTestCases();
                for(String s: tcs) {
                    String path1 = path + s;
                    int stat = utils.getStatusFromTC(path1);
                    String key = s + utils.LISTVIEW_SEP_KEY + Integer.toString(stat);
                    statusList.add(key);
                }
                break;
            }
        }
        
        return statusList;
    }
    
    static public ArrayList<String> getListofTCsFromSuite(String sName, sqpXML PROJMETA) {
        List<casesStruct> list = PROJMETA.getSuites();
        for(casesStruct c: list) {
            if(c.getSuiteName().equals(sName)) 
                return c.getTestCases();
        }
        return null;
    }
    
    static public String getNameFromKey(String s) {
        String p = "(.+)%#%([0-9])";
        Pattern pp = Pattern.compile(p);
        Matcher m = pp.matcher(s);
        if(m.matches())
            return m.group(1);
        else
            return null;
    }
    
    static public int getStatusFromKey(String s) {
        String p = "(.+)%#%([0-9])";
        Pattern pp = Pattern.compile(p);
        Matcher m = pp.matcher(s);
        if(m.find()) 
            return Integer.parseInt(m.group(2));
        else
            return 0;
    }
    
}
