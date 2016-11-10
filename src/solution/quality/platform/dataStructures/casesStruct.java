/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.dataStructures;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 *
 * @author nmallam1
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class casesStruct {
    
    private String suiteName;
    private ArrayList<String> testCases;
    
    public casesStruct() {
    }
    
    public void setSuiteName(String data) { this.suiteName = data; }
    public String getSuiteName() {return suiteName;}
    
    public void setTestCases(ArrayList<String> data) { this.testCases = data; }
    public ArrayList<String> getTestCases() {return testCases;}
    
    
    
    @Override
    public String toString() {
        String r = this.suiteName + ":- ";
        for(String s: this.testCases)
            r += s + ", ";        
        return r;
    }
    
}
