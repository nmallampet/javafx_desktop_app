/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.runFlow;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import org.apache.commons.io.FileUtils;
import solution.quality.platform.utils;

/**
 *
 * @author nmallam1
 */
public class ftdi {
    
    //final String FTDIEXE_LOC = "res/ftdi/ftdi.exe";
    final String FTDIEXE_LOC = utils.getJarLocation() + utils.SEPARATOR + "res" + utils.SEPARATOR + "ftdi" + utils.SEPARATOR +  "ftdi.exe";
    
    final private String TCLOC;
    private String CONFLOC;
    final private String OUTLOC;
    final private String LOGLOC;
    
    
    public ftdi(String TCLOC, String CONFLOC) {
        this.TCLOC = TCLOC;
        this.CONFLOC = getConfLoc(CONFLOC);
        this.OUTLOC = getOutLoc();
        this.LOGLOC = this.TCLOC + utils.SEPARATOR + "logs" + utils.SEPARATOR + "ftdiLog.txt";
        copyConfFile();
    }
    
    
    private String getConfLoc(String data) {
        if(utils.checkFileExists(new File(data)))
            return data;
        else
            return this.TCLOC + utils.SEPARATOR + data;
    }
    
    private void copyConfFile() {
        String path = this.TCLOC + utils.SEPARATOR + "sim" + utils.SEPARATOR + "input_files" + utils.SEPARATOR + "input.txt";
        if(!path.equals(CONFLOC)) {
            try {
                FileUtils.copyFile(new File(this.CONFLOC), new File(path)); 
            }
            catch(Exception e) {
                System.out.println("Error in copying conf file");
            }
            this.CONFLOC = path;
        }
    }
    
    private String getOutLoc() {
        String path = this.TCLOC + utils.SEPARATOR + "sim" + utils.SEPARATOR + "output_files";
        utils.createDirectory(path);
        return path;
    }
    
    
    public void run() {
        try(PrintWriter ftdiLog = new PrintWriter(this.LOGLOC, "UTF-8")) {
            ProcessBuilder builder = new ProcessBuilder(FTDIEXE_LOC, this.CONFLOC, this.OUTLOC);
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
