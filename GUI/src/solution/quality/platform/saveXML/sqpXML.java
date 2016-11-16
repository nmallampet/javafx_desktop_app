/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.saveXML;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import solution.quality.platform.dataStructures.casesStruct;

/**
 *
 * @author nmallam1
 */
@XmlRootElement(name = "SQP")
@XmlSeeAlso({casesStruct.class})
public class sqpXML {
    
    private String name;
    private String date;
    private String modDate;
    private String defEDA;
    private List<casesStruct> suites;
    
    @XmlElement(name = "Name")
    public String getName() {return name;}
    public void setName(String data) {this.name = data;}

    @XmlElement(name = "Created")
    public String getDate() {return date;}
    public void setDate(String data) {this.date = data;}
    
    @XmlElement(name = "LastMod")
    public String getModDate() {return modDate;}
    public void setModDate(String data) {this.modDate = data;}
    
    @XmlElement(name = "defaultEDA")
    public String getDefEDA() {return defEDA;}
    public void setDefEDA(String data) {this.defEDA = data;}
    
    @XmlElement(name = "Suites")
    public List<casesStruct> getSuites() {return suites;}
    public void setSuites(List<casesStruct> data) {this.suites = data;}

    
}
