/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controllers;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.parsers.ParserConfigurationException;
import models.CoregistratedData;
import org.xml.sax.SAXException;

/**
 *
 * @author Michal Sandanus
 */
public class Application implements Serializable{
    
    private File workingDir;
    private Coregistration coregistration;
    private ArrayList<CoregistratedData> coregistratedData = new ArrayList<CoregistratedData>();
    
    private Application() {}
    
    public static Application getInstance() {
        return ApplicationHolder.INSTANCE;
    }
    
    private static class ApplicationHolder {
        private static final Application INSTANCE = new Application();
    }

    public ArrayList<CoregistratedData> getCoregistratedData() {
        return coregistratedData;
    }
       
    public void setWorkingDir(File workingDir) {
        this.workingDir = workingDir;
    }

    public File getWorkingDir() {
        return workingDir;
    }

    public Coregistration getCoregistration() {
        return coregistration;
    }

    public void setCoregistration(Coregistration coregistration) {
        this.coregistration = coregistration;
    }
    
    public void loadCoregistratedData() throws ParserConfigurationException, SAXException, IOException {
        coregistratedData.clear();
        
        String[] subdirectories = workingDir.list();
        
        for(String subdirectory : subdirectories) {
            if (new File(workingDir + "/" + subdirectory).isDirectory()) {
                coregistratedData.add(new CoregistratedData(new File(workingDir + "/" + subdirectory)));
            }
        }
    }
    
    public ArrayList<CoregistratedData> getCoregistratedDataByName(String patientName) {
        ArrayList<CoregistratedData> coregDataFiltered = new ArrayList<CoregistratedData>();
        Iterator<CoregistratedData> iterCoregistratedData = coregistratedData.iterator();
        
        while (iterCoregistratedData.hasNext()) {
            CoregistratedData patient = iterCoregistratedData.next();
            if (patient.getName().toLowerCase().matches("(.*)" + patientName.toLowerCase() + "(.*)")) coregDataFiltered.add(patient);
        }
        
        return coregDataFiltered;
    }
    
    public CoregistratedData getCoregistratedDataByNameAndId(String patientName, String patientId) {
        Iterator<CoregistratedData> iterCoregistratedData = coregistratedData.iterator();
        
        while (iterCoregistratedData.hasNext()) {
            CoregistratedData coregData = iterCoregistratedData.next();
            if (coregData.getName() == patientName && coregData.getId() == patientId) return coregData;
        }
        
        return null;
    }
    
    public String classify(File patientDir) throws URISyntaxException {
        Classification classification = new Classification(patientDir);
        return classification.classify();
    }

    
}
