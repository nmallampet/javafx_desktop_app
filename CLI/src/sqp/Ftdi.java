/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sqp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 *
 * @author nmallam1
 */
public class Ftdi {
    
    

    
    //final String FTDIEXE_LOC = "res/ftdi/ftdi.exe";
    final String FTDIEXE_LOC = utils.getJarLocation() + utils.SEPARATOR + "res" + utils.SEPARATOR + "ftdi" + utils.SEPARATOR +  "ftdi.exe";
    
    final String confLoc;
    final String logLoc;
    final String outputLoc;
    
    public Ftdi(String s, String t, String absPath, String confLoc) {
        String tcN = absPath + utils.SEPARATOR + s + utils.SEPARATOR + t;
        this.confLoc = confLoc;
        this.outputLoc = tcN + utils.SEPARATOR + "sim" + utils.SEPARATOR + "output_files";
        this.logLoc = tcN + utils.SEPARATOR + "logs" + utils.SEPARATOR + "ftdiLog.txt";
    }

    public void run() {
        try(PrintWriter ftdiLog = new PrintWriter(this.logLoc, "UTF-8")) {
            ProcessBuilder builder = new ProcessBuilder(FTDIEXE_LOC, this.confLoc, this.outputLoc);
            builder.inheritIO().redirectOutput(ProcessBuilder.Redirect.PIPE);
            Process process = builder.start();
            final long startTime = System.currentTimeMillis();
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
                    reader.lines().forEach((line) -> {
                        if(line != null) {
                            ftdiLog.println(line);
                        }
                    });
                }
            catch(Exception ex) {
                System.err.println("Error in writing to ftdiLog file " + ex.getMessage());
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
                    
                ftdiLog.println("Time Elapsed: " + timeStr);
                ftdiLog.close();
            }
        }
        catch(Exception e) {
            System.err.println("Error in running ftdi.exe(sending stimulus to board) " + e.getMessage());
        }
    }
    
    
}
