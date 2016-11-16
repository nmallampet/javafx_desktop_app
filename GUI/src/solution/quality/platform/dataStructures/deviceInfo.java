/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.dataStructures;

import java.io.ByteArrayInputStream;
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import solution.quality.platform.parsers.LDFParser;
import solution.quality.platform.utils;

/**
 *
 * @author nmallam1
 */
public class deviceInfo {
    final String XDFLOCATION = "data" + utils.SEPARATOR + "vmdata" + utils.SEPARATOR + "database" + utils.SEPARATOR + "xpga";
    
    
    final String ldfLoc;
    File ldfFile;
    
    final String eda;
    
    private String vendor;
    private String family;
    private String devName;
    private String idCode;
    private String pon;
    
    
    public deviceInfo(String ldfLoc, String eda) {
        this.ldfLoc = ldfLoc;
        this.ldfFile = new File(this.ldfLoc);
        this.eda = eda;
        setDeviceInfo();
    }
    
    public void setVendor(String data) {this.vendor = data;}
    public String getVendor() {return vendor;}
    
    public void setFamily(String data) {this.family = data;}
    public String getFamily() {return family;}
    
    public void setDevName(String data) {this.devName = data;}
    public String getDevName() {return devName;}
    
    public void setIdCode(String data) {this.idCode = data;}
    public String getIdCode() {return idCode;}
    
    public void setPon(String data) {this.pon = data;}
    public String getPon() {return pon;}
    
    private String getXDFExtnFile(String path) {
        String extn = ".xdf";
        File dir = new File(path);
        
        String [] list = dir.list();
        for(String file: list) {
            if(file.endsWith(extn))
                return file;
        }
        return null;
    }
    
    private void setDeviceInfo() {
        String deviceName = LDFParser.getDeviceName(ldfFile);
        //System.out.println(deviceName);
        setDevName(deviceName);
        
        String XPGAFamily = utils.XPGAFamilyLUT(deviceName);
        String edaLocation = utils.getEDAEnvLocation(eda);
        String xdfPrePath = edaLocation + utils.SEPARATOR + this.XDFLOCATION + utils.SEPARATOR + XPGAFamily;
        String xdfFileName = getXDFExtnFile(xdfPrePath);
        String xdfPostPath = xdfPrePath + utils.SEPARATOR + xdfFileName;
        
        //System.out.println(xdfPostPath);
        parseXDF(deviceName, xdfPostPath);
            
        
        
    }
    
        
    
    //more info: http://www.mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
    private void parseXDF(String deviceName, String xdfPath) {
        
        String XDFVendor = null;
        String XDFId = null;
        String XDFFamily = null;
        
        boolean foundDeviceInfo = false;
        
        File file = new File(xdfPath);
        try {
            DocumentBuilderFactory dbFactory 
                = DocumentBuilderFactory.newInstance();
            
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            EntityResolver resolver = (String publicId, String systemId) -> {
                String empty = "";
                ByteArrayInputStream bais = new ByteArrayInputStream(empty.getBytes());
                return new InputSource(bais);
            };
            dBuilder.setEntityResolver(resolver);
            
            Document doc = dBuilder.parse(file);
            
            doc.getDocumentElement().normalize();
            
            NodeList nList = doc.getElementsByTagName("Family");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) nNode;
                    XDFFamily = e.getAttribute("name");
                    NodeList deviceList = e.getElementsByTagName("Device");
                    for(int i = 0; i  < deviceList.getLength(); i++) {
                        Node deviceNode = deviceList.item(i);
                        if(deviceNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element e2 = (Element) deviceNode;
                            String deviceNameXDF = e2.getAttribute("name");
                            if(deviceNameXDF.equals(deviceName)) {
                                XDFVendor = e2.getElementsByTagName("Vendor").item(0).getTextContent();
                                XDFId = e2.getElementsByTagName("JtagID").item(0).getTextContent();
                                foundDeviceInfo = true;
                                break;
                            }
                        }
                    }
                    if(foundDeviceInfo)
                        break;
                }
            }
            
            setVendor(XDFVendor);
            setFamily(XDFFamily);
            setIdCode(XDFId);

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
