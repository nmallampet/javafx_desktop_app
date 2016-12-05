/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sqp;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 *
 * @author nmallam1
 */
public class genBit {
    
    
    final String separator = File.separator;
    
    
    String TRUNK_LATTICE_LOC =   utils.getJarLocation() + utils.SEPARATOR + "res" + utils.SEPARATOR + "trunk" + utils.SEPARATOR +  "bin" + 
                                    utils.SEPARATOR + "run_lattice.py";
    //String TRUNK_LATTICE_LOC =   utils.getJarLocation() + utils.SEPARATOR + + "res/trunk/bin/run_lattice.py";
    
    String BIT_COMMAND = "--run-export-bitstream";
    String DES_COMMAND;
    String EDA_COMMAND; 
    
    String trunkLogLoc;
    
    String desLoc;
    String edaLoc;
    
    propStruct props;
    
    public genBit(String suiteName, String tcName, propStruct props, String absPath){
        //des
        this.desLoc = absPath + separator + suiteName + separator + tcName;
        
        this.DES_COMMAND = "--design=" + desLoc;
        
        //eda
        String eda = props.getEda();
        this.edaLoc = utils.getEDAEnvLocation(eda);
        this.EDA_COMMAND = "--diamond=" + edaLoc;
        
        //logs
        this.trunkLogLoc = desLoc + separator + "logs" + separator + "trunkLog.txt";
    }
    
    public void run() {
        try(PrintWriter logFile = new PrintWriter(trunkLogLoc, "UTF-8")) {
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
