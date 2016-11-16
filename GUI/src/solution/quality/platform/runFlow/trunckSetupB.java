/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.runFlow;

import java.io.PrintWriter;

/**
 *
 * @author nmallam1
 */
public class trunckSetupB {
    
    
    final private String testCaseLoc;    
    private String ldfLoc;
    
    public trunckSetupB(String testCaseLoc) {
        this.testCaseLoc = testCaseLoc;
    }
    
    
    public void createBQSFiles(String ldf) {
        this.ldfLoc = ldf;
        createBQSInfo();
        createBQSConf();
        
    }
    
    private void createBQSInfo() {
        String path = this.testCaseLoc + "\\bqs.info";
        try (PrintWriter bqsInfoFile = new PrintWriter(path, "UTF-8")) {
            bqsInfoFile.println("[qa]");
            bqsInfoFile.println("ldf_file = " + this.ldfLoc);
        }
        catch(Exception e) {
            System.err.println("Error in creating bqs.info: " + e.getMessage());
        }
    }
    
    private void createBQSConf() {
        String path = this.testCaseLoc + "\\bqs.conf";
        try (PrintWriter bqsConfFile = new PrintWriter(path, "UTF-8")) {
            bqsConfFile.println("[configuration information]\n" +
                                "area = Download\n" +
                                "type = File\n");
            bqsConfFile.println("[method]\n" +
                            "check_diamond_flow  = 1\n\n" +
                            "[check_diamond_flow]\n" +
                            "check_flow = bitstream\n");
        }
        catch(Exception e) {
            System.err.println("Error in creating bqs.conf: " + e.getMessage());
        }
    }
}
