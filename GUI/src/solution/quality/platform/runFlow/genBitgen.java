/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.runFlow;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import solution.quality.platform.utils;

/**
 *
 * @author nmallam1
 */
public class genBitgen {
    
    final String TRUNK_LATTICE_LOC =   utils.getJarLocation() + utils.SEPARATOR + "res" + utils.SEPARATOR + "trunk" + utils.SEPARATOR +  "bin" + 
                                    utils.SEPARATOR + "run_lattice.py";
    
    //final String TRUNK_LATTICE_LOC = "res/trunk/bin/run_lattice.py";
    final String loc;
    
    final private String BIT_COMMAND = "--run-export-bitstream";
    final private String DES_COMMAND;
    final private String EDA_COMMAND;
    
    final private String LOG_LOC;
    
    
    public genBitgen(String location, String eda) {
        this.loc = location;
        
        this.DES_COMMAND = "--design=" + this.loc;
        
        String edaEnv = utils.getEnvFromEda(eda);
        String edaLoc = utils.getEDAEnvLocation(edaEnv);
        this.EDA_COMMAND = "--diamond=" + edaLoc;
        
        this.LOG_LOC = location + utils.SEPARATOR + "logs" + utils.SEPARATOR + "trunkLog.txt";
    }
    
    public void run() {
        try(PrintWriter logFile = new PrintWriter(this.LOG_LOC, "UTF-8")) {
                ProcessBuilder builder = new ProcessBuilder("python", "-u", this.TRUNK_LATTICE_LOC, this.DES_COMMAND, this.EDA_COMMAND, this.BIT_COMMAND);
                builder.inheritIO().redirectOutput(ProcessBuilder.Redirect.PIPE);
                Process process = builder.start();
                final long startTime = System.currentTimeMillis();
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                        reader.lines().forEach((String line) -> {
                            if(line != null) {
                                logFile.println(line);
                                //System.out.println(line);
                            }
                        });
                }
                catch(Exception ex) {
                    System.err.println("Error in writing to trunklog file: " + ex.getMessage());
                }
                finally {
                    final long endTime = System.currentTimeMillis();
                    final long timeElapsed = endTime - startTime;
                    long secs = (timeElapsed / 1000) % 60;
                    long mins = (timeElapsed / (1000 * 60) ) % 60;
                    long hrs = (timeElapsed / (1000 * 60 * 60) ) % 60;
                    String timeStr;
                    if (hrs > 0)
                        timeStr = String.format("%02d:%02d:%02d", hrs, mins, secs);
                    else 
                        timeStr = String.format("%02d min, %02d sec", mins, secs);
                    
                    logFile.println("Time Elapsed: " + timeStr);
                    logFile.close();
                }
            }
            catch (Exception e) {
                System.err.println("Error in generating bitgen: " + e.getMessage());
            }
    }
    
}
