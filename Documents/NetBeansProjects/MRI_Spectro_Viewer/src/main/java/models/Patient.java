/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models;

import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import controllers.Coregistration;
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Michal Sandanus
 */
public class Patient {
    private String name;
    private String id;
    private String birthDate;
    private String sex;
    private int age;
    private int weight;
    
    private MRI mri;
    private Spectroscopy spectroscopy;
    
    private File segmentationMask;

    public Patient() {
        this.mri = new MRI();
        this.spectroscopy = new Spectroscopy();
    }

    public File getSegmentationMask() {
        return segmentationMask;
    }

    public void setSegmentationMask(File segmentationMask) {
        this.segmentationMask = segmentationMask;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public MRI getMri() {
        return mri;
    }

    public void setMri(MRI mri) {
        this.mri = mri;
    }

    public Spectroscopy getSpectroscopy() {
        return spectroscopy;
    }

    public void setSpectroscopy(Spectroscopy spectroscopy) {
        this.spectroscopy = spectroscopy;
    } 
    
    public void getPersonalData(){
        for (final File fileDicom : mri.getMriDir().listFiles()) {
            if (fileDicom.isDirectory()) continue;
            else {
                try {
                    AttributeList list = new AttributeList();
                    list.read(fileDicom);
                    
                    //name 
                    AttributeTag tagName = new AttributeTag("(0x0010,0x0010)");  
                    String[] nameString = list.get(tagName).getStringValues();
                    
                    name = nameString[0];
                    name = name.replace('^', ' ');
                    
                    //id
                    AttributeTag tagId = new AttributeTag("(0x0010,0x0020)");
                    String idString[] = list.get(tagId).getStringValues();
                    
                    id = idString[0];
                    
                    //birth date
                    AttributeTag tagBirthDate = new AttributeTag("(0x0010,0x0030)"); 
                    String birthDateString[] = list.get(tagBirthDate).getStringValues();
                    birthDate = birthDateString[0].substring(6) + "/" + birthDateString[0].substring(4, 6) + "/" + birthDateString[0].substring(0, 4);
                    
                    //sex
                    AttributeTag tagSex = new AttributeTag("(0x0010,0x0040)"); 
                    String sexString[] = list.get(tagSex).getStringValues();
                    
                    sex = sexString[0];
                    
                    //age
                    AttributeTag tagAge = new AttributeTag("(0x0010,0x1010)"); 
                    String ageString[] = list.get(tagAge).getStringValues();
                    
                    String ageStr = ageString[0];
                    age = Integer.parseInt(ageStr.substring(0, 3));
                    
                    //weight
                    AttributeTag tagWeight = new AttributeTag("(0x0010,0x1030)"); 
                    String weightString[] = list.get(tagWeight).getStringValues();
                    
                    weight = Integer.parseInt(weightString[0]);
                    
                    break;
                } 
                catch (Exception e) {
                    System.out.println("Error");
                }
            }
        }
    }
    
    public void generatePatientXml(File patientDir) throws ParserConfigurationException, TransformerConfigurationException, TransformerException{
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();
        
        Element root = document.createElement("patient");
        document.appendChild(root);       
        
        //personal data
        Element personalData = document.createElement("personalData");
        root.appendChild(personalData);
        
        Element patientName = document.createElement("name");
        patientName.appendChild(document.createTextNode(name));
        personalData.appendChild(patientName);
        
        Element patientId = document.createElement("id");
        patientId.appendChild(document.createTextNode(id));
        personalData.appendChild(patientId);
        
        Element patientBirthDate = document.createElement("birthDate");
        patientBirthDate.appendChild(document.createTextNode(birthDate));
        personalData.appendChild(patientBirthDate);
        
        Element patientSex = document.createElement("sex");
        patientSex.appendChild(document.createTextNode(sex));
        personalData.appendChild(patientSex);
        
        Element patientAge = document.createElement("age");
        patientAge.appendChild(document.createTextNode(String.valueOf(age)));
        personalData.appendChild(patientAge);
        
        Element patientWeight = document.createElement("weight");
        patientWeight.appendChild(document.createTextNode(String.valueOf(weight)));
        personalData.appendChild(patientWeight);
        
        
        //coregistration data
        Element coregistrationData = document.createElement("coregistrationData");
        root.appendChild(coregistrationData);
        
        Element metabolitesElement = document.createElement("metabolites");
        coregistrationData.appendChild(metabolitesElement);
        
        for (int i = 0; i < spectroscopy.getMetabolites().size(); i++) {
            Element metaboliteElement = document.createElement("metabolite");
            metaboliteElement.appendChild(document.createTextNode(spectroscopy.getMetabolites().get(i)));
            metabolitesElement.appendChild(metaboliteElement);
        }
        
        
        //classification data
        Element classificationData = document.createElement("classificationData");
        root.appendChild(classificationData);

        Element AIResult = document.createElement("AIResult");
        AIResult.appendChild(document.createTextNode("Neurčené"));
        classificationData.appendChild(AIResult);
        
        Element doctorResult = document.createElement("doctorResult");
        doctorResult.appendChild(document.createTextNode("Neurčené"));
        classificationData.appendChild(doctorResult);
        
        Element doctorNote = document.createElement("doctorNote");
        doctorNote.appendChild(document.createTextNode("Bez poznámky"));
        classificationData.appendChild(doctorNote);
        
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource domSource = new DOMSource(document);
        StreamResult streamResult = new StreamResult(new File(patientDir.getAbsolutePath() + "/patientInfo.xml"));
        transformer.transform(domSource, streamResult);
    }
}
