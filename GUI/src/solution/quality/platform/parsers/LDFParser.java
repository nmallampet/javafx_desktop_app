/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.parsers;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author nmallam1
 */
public class LDFParser {
    
    static public String getBaliTitle(File file) {
        
        String baliTitle = null;
        
        try {
            DocumentBuilderFactory dbFactory 
                = DocumentBuilderFactory.newInstance();
            
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            
            NodeList nListImpl = doc.getElementsByTagName("BaliProject");
            for (int temp = 0; temp < nListImpl.getLength(); temp++) {
                Node nNode = nListImpl.item(temp);
                baliTitle = nNode.getAttributes().getNamedItem("title").getNodeValue();
            }
            
            if( baliTitle != null) {
                return baliTitle;
            }
            
            else {
                System.err.println(String.format("Couldn't parse ldf file for impl directory name: %s, retrived: %s", file.getName(), baliTitle));
                return null;
            }
            
        }
        catch (Exception e) {
            System.err.println("Error parsing xcf file to getPortNum " + e.getMessage());
        }

        return null;
    }
    
    static public String getImplDirectory(File file) {
        
        String implTitle = null;
        
        try {
            DocumentBuilderFactory dbFactory 
                = DocumentBuilderFactory.newInstance();
            
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            
            NodeList nListImpl = doc.getElementsByTagName("Implementation");
            for (int temp = 0; temp < nListImpl.getLength(); temp++) {
                Node nNode = nListImpl.item(temp);
                implTitle = nNode.getAttributes().getNamedItem("title").getNodeValue();
            }
            
            if( implTitle != null) {
                return implTitle;
            }
            
            else {
                System.err.println(String.format("Couldn't parse ldf file for impl directory name: %s, retrived: %s", file.getName(), implTitle));
                return null;
            }
            
        }
        catch (Exception e) {
            System.err.println("Error parsing xcf file to getPortNum " + e.getMessage());
        }

        return null;
    }
    
    static public String getDeviceName(File file) {
        
        String deviceName = null;
        
        try {
            DocumentBuilderFactory dbFactory 
                = DocumentBuilderFactory.newInstance();
            
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            NodeList nList = doc.getElementsByTagName("BaliProject");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                deviceName = nNode.getAttributes().getNamedItem("device").getNodeValue();
            }
            
            if(deviceName != null) {
                String pattern = "([A-Z0-9]+[-A-Z0-9]+?)-([A-Z0-9]+)";
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(deviceName);
                if(m.find())
                    return m.group(1);
            }

        }
        catch (Exception e) {
            System.err.println("Error parsing xcf file to getPortNum " + e.getMessage());
        }

        return null;
    }
    
    static public String getBitFileName(File file) {
        String baliTitle = getBaliTitle(file);
        String implTitle = getImplDirectory(file);
        String name = baliTitle + "_" + implTitle + ".bit";
        return name;
    }
    
    
    static public String getSynProjName(File file) {
        String baliTitle = getBaliTitle(file);
        String implTitle = getImplDirectory(file);
        String extn = "_lattice.synproj";
        String name = baliTitle + "_" + implTitle + extn;
        
        return name;
    }
    
}
