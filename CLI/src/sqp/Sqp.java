/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sqp;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

/**
 *
 * @author nmallam1
 */
public class Sqp {
    /**
     * @param args the command line arguments
     */
    
    public static void main(String[] args) {

        /*
        -infile C:\\Users\\nmallam1\\Desktop\\tmtptmp\\input.txt -out C:\\Users\\nmallam1\\Desktop\\tmtptmp\\out
        -infile C:\\Users\\nmallam1\\Desktop\\tmtptmp\\inputwb.txt -out C:\\Users\\nmallam1\\Desktop\\tmtptmp\\outwb
        -infile C:\\Users\\nmallam1\\Desktop\\tmtptmp\\daisytest\\inputtest.txt -out C:\\Users\\nmallam1\\Desktop\\tmtptmp\\daisytest\\out
        -infile C:\\Users\\nmallam1\\Desktop\\testrn\\input.txt -out C:\\Users\\nmallam1\\Desktop\\testrn\\out
        */
        
        
        //git comment test
        

        //create parser
        CommandLineParser parser = new DefaultParser();
        
        Options options = new Options();
        
        //arg support
        options.addOption("infile", true, "input config file with locations of design directory, config file, EDA SW bin, ldf file, and testcase name");
        options.addOption("sqpfile", true, "input SQP file that was generated from GUI");
        options.addOption("out", true, "output directory");
        
        try {
            CommandLine line = parser.parse(options, args);
            HelpFormatter help = new HelpFormatter();
            
            if(line.hasOption("infile")) {
                if(line.hasOption("out")) {
                    String inFile = line.getOptionValue("infile");
                    String outFile = line.getOptionValue("out");
                    inputFileParser in = new inputFileParser(inFile, outFile);
                    //System.out.println(in);  
                }
                else {
                    System.err.println("Error: No output directory specified");
                    help.printHelp("java -jar sqp.jar", options);
                    System.exit(1);
                }
            }
            
            else if(line.hasOption("sqpfile")) {
                String sqpFile = line.getOptionValue("sqpfile");
                System.out.println(sqpFile);
            }
            
            else {
               // HelpFormatter help = new HelpFormatter();
                help.printHelp("java -jar sqp.jar", options);
            }

        }
        catch (Exception e) {
            System.err.println("Unexpected exception:"  + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
}
