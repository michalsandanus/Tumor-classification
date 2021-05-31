/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Michal Sandanus
 */
public class CoregistratedData {
    private String name;
    private String id;
    private String birthDate;
    private String sex;
    private int age;
    private int weight;
    
    private ArrayList<String> metabolites = new ArrayList<String>();
    
    private String AIClass;
    private String doctorClass;
    private String doctorNote;
    
    private File patientDir; 
    private ArrayList<BufferedImage> mriImages = new ArrayList<BufferedImage>();
    private BufferedImage[] spectroscopyImages;

    public CoregistratedData(File patientDir) throws ParserConfigurationException, SAXException, IOException {
        this.patientDir = patientDir;
        getData();
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getSex() {
        return sex;
    }

    public int getAge() {
        return age;
    }

    public int getWeight() {
        return weight;
    }

    public ArrayList<String> getMetabolites() {
        return metabolites;
    }

    public String getDoctorClass() {
        return doctorClass;
    }

    public File getPatientDir() {
        return patientDir;
    }

    public ArrayList<BufferedImage> getMriImages() {
        return mriImages;
    }

    public BufferedImage[] getSpectroscopyImages() {
        return spectroscopyImages;
    }

    public String getDoctorNote() {
        return doctorNote;
    }

    public void setDoctorNote(String doctorNote) {
        this.doctorNote = doctorNote;
    }

    public void setDoctorClass(String doctorClass) {
        this.doctorClass = doctorClass;
    }

    public String getAIClass() {
        return AIClass;
    }

    public void setAIClass(String AIClass) {
        this.AIClass = AIClass;
    }
    
    

    private void getData() throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new File(patientDir.getAbsolutePath() + "/patientInfo.xml"));
        doc.getDocumentElement().normalize();
        
        Element elementPersonalData = (Element) doc.getElementsByTagName("personalData").item(0);
        name = elementPersonalData.getElementsByTagName("name").item(0).getTextContent();
        id = elementPersonalData.getElementsByTagName("id").item(0).getTextContent();
        birthDate = elementPersonalData.getElementsByTagName("birthDate").item(0).getTextContent();
        sex = elementPersonalData.getElementsByTagName("sex").item(0).getTextContent();
        age = Integer.parseInt(elementPersonalData.getElementsByTagName("age").item(0).getTextContent());
        weight = Integer.parseInt(elementPersonalData.getElementsByTagName("weight").item(0).getTextContent());
        
        Element elementCoregistrationData = (Element) doc.getElementsByTagName("coregistrationData").item(0);
        Element elementMetabolites = (Element) elementCoregistrationData.getElementsByTagName("metabolites").item(0);
        NodeList nodeListMetabolites = elementMetabolites.getElementsByTagName("metabolite");
        for (int m = 0; m < nodeListMetabolites.getLength(); m++) {
            Element elementMetabolite = (Element) nodeListMetabolites.item(m);
            metabolites.add(elementMetabolite.getTextContent());
        }
        
        Element elementClassificationData = (Element) doc.getElementsByTagName("classificationData").item(0);
        AIClass = elementClassificationData.getElementsByTagName("AIResult").item(0).getTextContent();
        doctorClass = elementClassificationData.getElementsByTagName("doctorResult").item(0).getTextContent();
        doctorNote= elementClassificationData.getElementsByTagName("doctorNote").item(0).getTextContent();
    }
    
    public void setData() throws ParserConfigurationException, TransformerConfigurationException, TransformerException {
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
        
        for (int i = 0; i < metabolites.size(); i++) {
            Element metaboliteElement = document.createElement("metabolite");
            metaboliteElement.appendChild(document.createTextNode(metabolites.get(i)));
            metabolitesElement.appendChild(metaboliteElement);
        }
        
        //classification data
        Element classificationData = document.createElement("classificationData");
        root.appendChild(classificationData);
        
        Element AIResult = document.createElement("AIResult");
        AIResult.appendChild(document.createTextNode(AIClass));
        classificationData.appendChild(AIResult);
        
        Element doctorResult = document.createElement("doctorResult");
        doctorResult.appendChild(document.createTextNode(doctorClass));
        classificationData.appendChild(doctorResult);
        
        Element doctorNote = document.createElement("doctorNote");
        doctorNote.appendChild(document.createTextNode(this.doctorNote));
        classificationData.appendChild(doctorNote);
        
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource domSource = new DOMSource(document);
        StreamResult streamResult = new StreamResult(new File(patientDir.getAbsolutePath() + "/patientInfo.xml"));
        transformer.transform(domSource, streamResult);
    }
    
    public void loadMriImages() throws IOException {
        File mriDir = new File(patientDir.getAbsolutePath() + "/MRI_Images");
        int frames = mriDir.list().length;
        mriImages.clear();
        
        for (int i = 0; i < frames; i++) {
            mriImages.add(ImageIO.read(new File(mriDir.getAbsolutePath() + "/frame_" + String.valueOf(i) + ".png")));
        }
    }
    
    public void loadSpectroscopicImages(int metaboliteSelected) throws IOException {
        File spectroscopyDir = new File(patientDir.getAbsolutePath() + "/Metabolites_Images");
        int frames = mriImages.size();

        spectroscopyImages = new BufferedImage[frames];
        for (int j = 0; j < frames; j++) {
            spectroscopyImages[j] = ImageIO.read(new File(spectroscopyDir.getAbsolutePath() + "/" + metabolites.get(metaboliteSelected).replace(' ', '_') + "/frame_" + String.valueOf(j) + ".png"));
        }
    }
    
    public String[] getStringMetabolites() {
        String[] metabolitesArray = new String[metabolites.size()];
        for (int i = 0; i < metabolites.size(); i++) {
            metabolitesArray[i] = metabolites.get(i).replace('_', ' ');
        }
        return metabolitesArray;
    }
}
