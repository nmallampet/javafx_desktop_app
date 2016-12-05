/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sqp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author nmallam1
 */
public class utils {
    
    /*
     * This class contains all the commonly used functions
     */
    
    final static String SEPARATOR = File.separator;
    
    
    /* FILE IO FUNCTIONS */
    static public boolean checkFileExists(File fileName) {
        return fileName.exists();
    }
    static public boolean checkFileDir(File fileName) {
        return fileName.isDirectory();
    }
    static public void closeFile(BufferedReader br) {
        try {
            br.close();
        }
        catch(Exception e) {
            System.err.println("Failed to close file: " + br.toString());
        }
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
    static public boolean checkStringInFile(String key, String logLoc) {
        //String prgLogLoc = this.absPathOutPut + utils.SEPARATOR + sn + utils.SEPARATOR + t + utils.SEPARATOR + "logs" + utils.SEPARATOR + "ProgrammerOutput.txt";
        File file = new File(logLoc);
        try {
            Scanner s = new Scanner(file);
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if(line.equals(key)) {
                    return true;
                }
            }
        }
        catch(Exception e) {
            System.err.println("Error in checking for string in File "  + e.getMessage());
        }
        
        return false;
        
    }
    static public String getLastNLinesOfFile( File file, int lines) {
        java.io.RandomAccessFile fileHandler = null;
        try {
            fileHandler = 
                new java.io.RandomAccessFile( file, "r" );
            long fileLength = fileHandler.length() - 1;
            StringBuilder sb = new StringBuilder();
            int line = 0;

            for(long filePointer = fileLength; filePointer != -1; filePointer--){
                fileHandler.seek( filePointer );
                int readByte = fileHandler.readByte();

                 if( readByte == 0xA ) {
                    if (filePointer < fileLength) {
                        line = line + 1;
                    }
                } else if( readByte == 0xD ) {
                    if (filePointer < fileLength-1) {
                        line = line + 1;
                    }
                }
                if (line >= lines) {
                    break;
                }
                sb.append( ( char ) readByte );
            }

            String lastLine = sb.reverse().toString();
            return lastLine;
        } 
        catch(Exception e ) {
            e.printStackTrace();
            return null;
        }
        finally {
            if (fileHandler != null )
                try {
                    fileHandler.close();
                } 
                catch (IOException e) {
                }
        }
    }
    
    
    
    
    
    
    /* GET PROP VALUES FROM LIST */
    static public String getLDFFromList(ArrayList<String> tmp) {
        for(String s: tmp) {
            if(s.contains("ldf"))
                return s.substring(4);
        }
        return null;
    }
    static public String getConfFromList(ArrayList<String> props) {
        for(String s: props) {
            if(s.contains("conf"))
                return s.substring(5);
        }
        return null;
    }
    static public String getEDAFromList(ArrayList<String> props) {
        for(String s: props) {
            if(s.contains("eda"))
                return s.substring(4);
        }
        return null;
    }
    static public String getDesFromList(ArrayList<String> props) {
        for(String s: props) {
            if(s.contains("des"))
                return s.substring(4);
        }
        return null;
    }
    
    
    static public String getEDAEnvName(String eda) {
        return edaParser.getEnvFromEda(eda);
    }
    /* OTHER FUNCTIONS */
    static public String getEDAEnvLocation(String eda) {
        String envVar = edaParser.getEnvFromEda(eda);
        return System.getenv(envVar);
    }
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
    static public String getXCFFileLoc(String ldfLoc) {
        File file = new File(ldfLoc);
        try {
            DocumentBuilderFactory dbFactory 
                = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            NodeList nList = doc.getElementsByTagName("Source");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String name = eElement.getAttribute("name");
                    if(name.toLowerCase().endsWith(".xcf")) {
                        return name;
                    }
                }
            }
        }
        catch (Exception e) {
            System.err.println("Failed to parse xml File to get xcf location: " + e.getMessage());
        }
        return null;
    }
    
    
    /*Trunk Script related Functions*/
    public static String getJarLocation() {
        try {
            return new File(Sqp.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();
        }
        catch (Exception e) {
            System.err.println("pls wtf");
        }
        return null;
    }
    
    
}
