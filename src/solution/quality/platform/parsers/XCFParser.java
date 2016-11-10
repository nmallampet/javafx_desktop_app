/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.parsers;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import solution.quality.platform.dataStructures.deviceInfo;

/**
 *
 * @author nmallam1
 */
public class XCFParser {
    static public void setDeviceNames(deviceInfo dInfo, File file) {
        String family = dInfo.getFamily();
        String dev = dInfo.getDevName();
        
        try {
            DocumentBuilderFactory dbFactory 
                = DocumentBuilderFactory.newInstance();
            dbFactory.setValidating(false);
            dbFactory.setNamespaceAware(true);
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            Node PaddNodeFamily = doc.getElementsByTagName("Family").item(0);
            PaddNodeFamily.setTextContent(family);
            
            Node PadNodeName = doc.getElementsByTagName("Name").item(0);
            PadNodeName.setTextContent(dev);
            
            Node PadNodePon = doc.getElementsByTagName("PON").item(0);
            PadNodePon.setTextContent(dev);
            
              // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory
                    .newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "IspXCF.dtd");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);
        }
        catch (Exception e) {
            System.err.println("Error parsing xcf file to setPortAdd " + e.getMessage());
        }
    }
    
    static public void setDeviceNamesDaisy(deviceInfo dInfo, File file, int index) {
        String vendor = dInfo.getVendor();
        String family = dInfo.getFamily();
        String dev = dInfo.getDevName();
        String id = dInfo.getIdCode();
        try {
            DocumentBuilderFactory dbFactory 
                = DocumentBuilderFactory.newInstance();
            dbFactory.setValidating(false);
            dbFactory.setNamespaceAware(true);
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            
            Node chainRoot = doc.getElementsByTagName("Chain").item(0);
            
            Element DeviceE = doc.createElement("Device");
            
            Element SelectedProgE = doc.createElement("SelectedProg");
            SelectedProgE.setAttribute("value", "TRUE");
            DeviceE.appendChild(SelectedProgE);
            
            Element PosE = doc.createElement("Pos");
            PosE.appendChild(doc.createTextNode(String.valueOf(index + 1)));
            DeviceE.appendChild(PosE);
            
            Element VendorE = doc.createElement("Vendor");
            VendorE.appendChild(doc.createTextNode(vendor));
            DeviceE.appendChild(VendorE);
            
            Element FamilyE = doc.createElement("Family");
            FamilyE.appendChild(doc.createTextNode(family));
            DeviceE.appendChild(FamilyE);
            
            Element NameE = doc.createElement("Name");
            NameE.appendChild(doc.createTextNode(dev));
            DeviceE.appendChild(NameE);
            
            Element IDCodeE = doc.createElement("IDCode");
            IDCodeE.appendChild(doc.createTextNode(id));
            DeviceE.appendChild(IDCodeE);
            
            Element PackageE = doc.createElement("Package");
            PackageE.appendChild(doc.createTextNode("All"));
            DeviceE.appendChild(PackageE);
            
            
            
            Element BypassE = doc.createElement("Bypass");
            Element InstrLenE = doc.createElement("InstrLen");
            InstrLenE.appendChild(doc.createTextNode("8"));
            BypassE.appendChild(InstrLenE);
            Element InstrVal  = doc.createElement("InstrVal");
            InstrVal.appendChild(doc.createTextNode("11111111"));
            BypassE.appendChild(InstrVal);
            Element BScanLen  = doc.createElement("BScanLen");
            BScanLen.appendChild(doc.createTextNode("1"));
            BypassE.appendChild(BScanLen);
            Element BScanVal = doc.createElement("BScanVal");
            BScanVal.appendChild(doc.createTextNode("0"));
            BypassE.appendChild(BScanVal);
            DeviceE.appendChild(BypassE);
            
            Element FileE  = doc.createElement("File");
            DeviceE.appendChild(FileE);
            
            Element OperationE  = doc.createElement("Operation");
            OperationE.appendChild(doc.createTextNode("Fast Program"));
            DeviceE.appendChild(OperationE);
            
            chainRoot.appendChild(DeviceE);
            
              // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory
                    .newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "IspXCF.dtd");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);
        }
        catch (Exception e) {
            //System.err.println("Error parsing xcf file to setPortAdd " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static public String getPortNum(File file) {
       
        try {
            DocumentBuilderFactory dbFactory 
                = DocumentBuilderFactory.newInstance();
            dbFactory.setValidating(false);
            dbFactory.setNamespaceAware(true);
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            NodeList nList = doc.getElementsByTagName("CableOptions");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    //String CableName = eElement.getElementsByTagName("CableName").item(0).getTextContent();
                    String PortAdd = eElement.getElementsByTagName("PortAdd").item(0).getTextContent();
                    //String USBID = eElement.getElementsByTagName("USBID").item(0).getTextContent();
                    String USBID = null;
                    if(PortAdd != null)
                        return PortAdd;
                }
            }
        }
        catch (Exception e) {
            System.err.println("Error parsing xcf file to getPortNum " + e.getMessage());
        }

        return null;
    }
    
    static public void setPortAdd(String data, File file) {
        try {
            DocumentBuilderFactory dbFactory 
                = DocumentBuilderFactory.newInstance();
            dbFactory.setValidating(false);
            dbFactory.setNamespaceAware(true);
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            Node dataChild = doc.getFirstChild();
            Node PaddNode = doc.getElementsByTagName("PortAdd").item(0);
            PaddNode.setTextContent(data);
            
              // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory
                    .newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "IspXCF.dtd");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);
        }
        catch (Exception e) {
            System.err.println("Error parsing xcf file to setPortAdd " + e.getMessage());
        }
       
    }
    
    static public void setBitFile(String data, File file) {
        try {
            DocumentBuilderFactory dbFactory 
                = DocumentBuilderFactory.newInstance();
            dbFactory.setValidating(false);
            dbFactory.setNamespaceAware(true);
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            Node dataChild = doc.getFirstChild();
            Node PaddNode = doc.getElementsByTagName("File").item(0);
            String dataNew = data.replace("\\", "/");
            PaddNode.setTextContent(dataNew);
            
              // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory
                    .newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "IspXCF.dtd");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);
        }
        catch (Exception e) {
            System.err.println("Error parsing xcf file to setXCFFile " + e.getMessage());
        }
       
    }
    
    static public void setBitFileDaisy(String data, File file, int index) {
        try {
            DocumentBuilderFactory dbFactory 
                = DocumentBuilderFactory.newInstance();
            dbFactory.setValidating(false);
            dbFactory.setNamespaceAware(true);
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            NodeList nList = doc.getElementsByTagName("File");
            Node PaddNode = doc.getElementsByTagName("File").item(index);
            String dataNew = data.replace("\\", "/");
            PaddNode.setTextContent(dataNew);
            
              // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory
                    .newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "IspXCF.dtd");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);
        }
        catch (Exception e) {
            System.err.println("Error parsing xcf file to setXCFFile " + e.getMessage());
        }
       
    }
}
