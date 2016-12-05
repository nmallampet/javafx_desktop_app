/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sqp;

/**
 *
 * @author nmallam1
 */
public class edaParser {
    
    final static String [] EDALIST = {"diamond", "icecube"};
    
    final static String DIAMOND_ENV = "EXTERNAL_DIAMOND_PATH";
    final static String ICECUBE_ENV = "EXTERNAL_ICECUBE_PATH";
    
    
    
    
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
    
    static public boolean isValidEda(String eda) {
        for(String s: EDALIST) {
            if(eda.equals(s))
                return true;
        }
        
        System.out.print("eda:{");
        for(String s: EDALIST) {
            if(s.equals(EDALIST[EDALIST.length - 1]))
                System.out.print(s);
            else
                System.out.print(s +", ");
        }
            
        System.out.print("}\n");
        return false;
    }
 
    
}
