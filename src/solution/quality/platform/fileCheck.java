/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author nmallam1
 */
public class fileCheck {
    
    //trunk log pass and fail keys
    //static public final String TRUNK_PASSKEY1 = "- In check_diamond_flow: True";
    static public final String TRUNK_PASSKEY1 = "Status: Pass.";
    
    //Programmer Log Pass and fail keys
    static public final String PROGRAMMER_FAILKEY = "Failed.";
    static public final String PROGRAMMER_PASSKEY1 = "Done.";
    static public final String PROGRAMMER_PASSKEY2 = "Lattice Diamond Programmer has exited successfully.";
    static public final String PROGRAMMER_MISSKEY = "Programming XCF Contents... Failed to Query Cable from the Lattice Cable Server.";
    
    //FTDI LOg pass and fail keys
    static public final String FTDILOG_PASSKEY1 = "Passed";
    
    //Results Keys
    static public final String RPASS = "Passed";
    static public final String RFAIL = "Failed";
    static public final String RFAIL_BITGEN = "Failed in generating bitstream";
    static public final String RFAIL_PROGRAMMER = "Failed in Programmer";
    static public final String RFAIL_FTDI = "Failed in stimulus package (in sending or mismatches)";
    

    static public boolean checkStringInFile(String key, String logLoc) {
        //String prgLogLoc = this.absPathOutPut + utils.SEPARATOR + sn + utils.SEPARATOR + t + utils.SEPARATOR + "logs" + utils.SEPARATOR + "ProgrammerOutput.txt";
        File file = new File(logLoc);
        try {
            Scanner s = new Scanner(file);
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if(line.equals(key)) {
                    return true;
                }
            }
        }
        catch(Exception e) {
            System.err.println("Error in checking for string in File "  + e.getMessage());
        }
        
        return false;
        
    }
    
    
    static public String getLastNLinesOfFile( File file, int lines) {
        java.io.RandomAccessFile fileHandler = null;
        try {
            fileHandler = 
                new java.io.RandomAccessFile( file, "r" );
            long fileLength = fileHandler.length() - 1;
            StringBuilder sb = new StringBuilder();
            int line = 0;

            for(long filePointer = fileLength; filePointer != -1; filePointer--){
                fileHandler.seek( filePointer );
                int readByte = fileHandler.readByte();

                 if( readByte == 0xA ) {
                    if (filePointer < fileLength) {
                        line = line + 1;
                    }
                } else if( readByte == 0xD ) {
                    if (filePointer < fileLength-1) {
                        line = line + 1;
                    }
                }
                if (line >= lines) {
                    break;
                }
                sb.append( ( char ) readByte );
            }

            String lastLine = sb.reverse().toString();
            return lastLine;
        } 
        catch(Exception e ) {
            e.printStackTrace();
            return null;
        }
        finally {
            if (fileHandler != null )
                try {
                    fileHandler.close();
                } 
                catch (IOException e) {
                }
        }
    }
    
    static public boolean checkLastStringInFile(String prgLogLoc) {
        //String prgLogLoc = this.absPathOutPut + utils.SEPARATOR + sn + utils.SEPARATOR + t + utils.SEPARATOR + "logs" + utils.SEPARATOR + "ProgrammerOutput.txt"; 
        String lastLines = getLastNLinesOfFile(new File(prgLogLoc), 5);

        boolean flag1 = lastLines.contains(PROGRAMMER_PASSKEY1);
        boolean flag2 = lastLines.contains(PROGRAMMER_PASSKEY2);
        
        return (flag1 && flag2);  
    }
    
    static public boolean checkBitGen(String loc) {
        String logLoc = loc + utils.SEPARATOR + "logs" + utils.SEPARATOR + "trunkLog.txt";
        File file = new File(logLoc);
        int passCount = 0;
        try {
            Scanner s = new Scanner(file);
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if(line.contains(TRUNK_PASSKEY1)) {
                    passCount++;
                }
                if(passCount == 2)
                    return true;
            }
        }
        catch(Exception e) {
            System.err.println("Error in checking for string in File "  + e.getMessage());
        }
        return false;
    }
    static public boolean createBitgenFile_A(String loc) {
        String path = loc + utils.SEPARATOR + "sqpData" + utils.SEPARATOR;
        String path1 = path + "RBT_A";
        String path2 = path + "RBT_B";
        String path3 = path + "RBT_C";
        if(utils.checkFileExists(new File(path2)))
            utils.deleteFile(path2);
        if(utils.checkFileExists(new File(path3)))
            utils.deleteFile(path3);
        return utils.createFile(path1);
    }
    static public boolean createBitgenFile_B(String loc) {
        String path = loc + utils.SEPARATOR + "sqpData" + utils.SEPARATOR;
        String path1 = path + "RBT_A";
        String path2 = path + "RBT_B";
        if(!utils.checkFileExists(new File(path1)))
            return utils.createFile(path2);
        else {
            utils.deleteFile(path1);
            return utils.createFile(path2);
        }
    }
    static public boolean createBitgenFile_C(String loc) {
        String path = loc + utils.SEPARATOR + "sqpData" + utils.SEPARATOR;
        String path1 = path + "RBT_A";
        String path2 = path + "RBT_C";
        if(!utils.checkFileExists(new File(path1)))
            return utils.createFile(path2);
        else {
            utils.deleteFile(path1);
            return utils.createFile(path2);
        }
            
    }
    static public void cleanBitgenFiles(String loc) {
        String path = loc + utils.SEPARATOR + "sqpData" + utils.SEPARATOR;
        String path1 = path + "RBT_A";
        String path2 = path + "RBT_B";
        String path3 = path + "RBT_C";
        if(utils.checkFileExists(new File(path2)))
            utils.deleteFile(path2);
        if(utils.checkFileExists(new File(path3)))
            utils.deleteFile(path3);
        if(utils.checkFileExists(new File(path1)))
            utils.deleteFile(path1);
    }
    
    
    static public boolean checkProgrammer(String loc) {
        String path = loc + utils.SEPARATOR + "logs" + utils.SEPARATOR + "ProgrammerOutput.txt";
        return checkLastStringInFile(path);
    }
    static public boolean createPrgFile_A(String loc) {
        String path = loc + utils.SEPARATOR + "sqpData" + utils.SEPARATOR;
        String path1 = path + "XCF_A";
        String path2 = path + "XCF_B";
        String path3 = path + "XCF_C";
        if(utils.checkFileExists(new File(path2)))
            utils.deleteFile(path2);
        if(utils.checkFileExists(new File(path3)))
            utils.deleteFile(path3);
        return utils.createFile(path1);
    }
    static public void daisyCreatePrgFiles_A(ArrayList<String> locs) {
       for(int i = 1; i < locs.size(); i++) {
            String loc = locs.get(i);
            fileCheck.cleanBitgenFiles(loc);
            fileCheck.createPrgFile_A(loc);
        } 
    }
    static public boolean createPrgFile_B(String loc) {
        String path = loc + utils.SEPARATOR + "sqpData" + utils.SEPARATOR;
        String path1 = path + "XCF_A";
        String path2 = path + "XCF_B";
        if(!utils.checkFileExists(new File(path1)))
            return utils.createFile(path2);
        else {
            utils.deleteFile(path1);
            return utils.createFile(path2);
        }
    }
    static public boolean createPrgFile_C(String loc) {
        String path = loc + utils.SEPARATOR + "sqpData" + utils.SEPARATOR;
        String path1 = path + "XCF_A";
        String path2 = path + "XCF_C";
        if(!utils.checkFileExists(new File(path1)))
            return utils.createFile(path2);
        else {
            utils.deleteFile(path1);
            return utils.createFile(path2);
        }
            
    }
    static public void daisyPrgFiles(ArrayList<String> locs, boolean flag) {
        for(int i = 1; i < locs.size(); i++) {
            String loc = locs.get(i);
            fileCheck.cleanBitgenFiles(loc);
            if(flag)
                fileCheck.createPrgFile_C(loc);
            else
                fileCheck.createPrgFile_B(loc);
        }
        
    }
    static public void cleanPrgFiles(String loc) {
        String path = loc + utils.SEPARATOR + "sqpData" + utils.SEPARATOR;
        String path1 = path + "XCF_A";
        String path2 = path + "XCF_B";
        String path3 = path + "XCF_C";
        if(utils.checkFileExists(new File(path2)))
            utils.deleteFile(path2);
        if(utils.checkFileExists(new File(path3)))
            utils.deleteFile(path3);
        if(utils.checkFileExists(new File(path1)))
            utils.deleteFile(path1);
    }
    
    
    /* FTDI STATUS FILES */
    static public boolean checkFtdi(String loc) {
        String path = loc + utils.SEPARATOR + "sim" + utils.SEPARATOR + "output_files" + utils.SEPARATOR + "output.txt";
        return checkStringInFile(FTDILOG_PASSKEY1, path);
    }
    static public boolean createFTDIFile_A(String loc) {
        String path = loc + utils.SEPARATOR + "sqpData" + utils.SEPARATOR;
        String path1 = path + "FTDI_A";
        String path2 = path + "FTDI_B";
        String path3 = path + "FTDI_C";
        if(utils.checkFileExists(new File(path2)))
            utils.deleteFile(path2);
        if(utils.checkFileExists(new File(path3)))
            utils.deleteFile(path3);
        return utils.createFile(path1);
    }
    static public boolean createFTDIFile_B(String loc) {
        String path = loc + utils.SEPARATOR + "sqpData" + utils.SEPARATOR;
        String path1 = path + "FTDI_A";
        String path2 = path + "FTDI_B";
        if(!utils.checkFileExists(new File(path1)))
            return utils.createFile(path2);
        else {
            utils.deleteFile(path1);
            return utils.createFile(path2);
        }
    }
    static public boolean createFTDIFile_C(String loc) {
        String path = loc + utils.SEPARATOR + "sqpData" + utils.SEPARATOR;
        String path1 = path + "FTDI_A";
        String path2 = path + "FTDI_C";
        if(!utils.checkFileExists(new File(path1)))
            return utils.createFile(path2);
        else {
            utils.deleteFile(path1);
            return utils.createFile(path2);
        }
            
    }
    static public void cleanFTDIFiles(String loc) {
        String path = loc + utils.SEPARATOR + "sqpData" + utils.SEPARATOR;
        String path1 = path + "FTDI_A";
        String path2 = path + "FTDI_B";
        String path3 = path + "FTDI_C";
        if(utils.checkFileExists(new File(path2)))
            utils.deleteFile(path2);
        if(utils.checkFileExists(new File(path3)))
            utils.deleteFile(path3);
        if(utils.checkFileExists(new File(path1)))
            utils.deleteFile(path1);
    }
    
    static public void cleanStatusFiles(String loc) {
        String path = loc + utils.SEPARATOR + "sqpData";
        File [] files = new File(path).listFiles();
        String fileChoice = null;
        
        String fPattern = "[A-Z]{3}[A-Z]?_[A|B|C]";
        Pattern fPPattern = Pattern.compile(fPattern);
        for(File f: files) {
            String s = f.getName();
            Matcher m = fPPattern.matcher(s);
            if(m.matches())
                fileChoice = f.toString();
        }
        
        if(fileChoice != null) {
            utils.deleteFile(fileChoice);
        }
    }
    
}
