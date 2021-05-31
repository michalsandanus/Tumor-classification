/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 *
 * @author Michal Sandanus
 */
public class Classification {
    private File patientDir;
    private ArrayList<String> classes;

    public Classification(File patientDir) {
        this.patientDir = patientDir;
    }

    public String classify() throws URISyntaxException {
        String s = null;

        try {           
            String finalArrayPath = patientDir.getAbsolutePath() + "/Arrays/CoregArray.txt";
            String arrayIndexesPath = patientDir.getAbsolutePath() + "/Arrays/ArrayIndexes.txt";
            String maskPath = patientDir.getAbsolutePath() + "/Arrays/Mask.nii";
            String dimensionsPath = patientDir.getAbsolutePath() + "/Arrays/Dimensions.txt";
            
	    // run the Unix "ps -ef" command
            // using the Runtime exec method:
            //Process p = Runtime.getRuntime().exec("ps -ef");
            
            boolean finalClassReading = false;
            String path = new File(Classification.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            
            Process p = Runtime.getRuntime().exec("python " + path + "\\classification.py " + finalArrayPath + " " + arrayIndexesPath + " " + maskPath + " " + dimensionsPath);
            
            BufferedReader stdInput = new BufferedReader(new 
                 InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new 
                 InputStreamReader(p.getErrorStream()));

            // read the output from the command
            System.out.println("Here is the standard output of the command:\n");
            while ((s = stdInput.readLine()) != null) {
                if (finalClassReading){
                    System.out.println(s);
                    return s;
                }
                if (s.equalsIgnoreCase("final result")) finalClassReading = true;
                System.out.println(s);
            }
            
            // read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
            
        }
        catch (IOException e) {
            System.out.println("exception happened - here's what I know: ");
            e.printStackTrace();
            System.exit(-1);
        }
        
        return s;
    }
    
}
