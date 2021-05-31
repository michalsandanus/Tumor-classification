/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models;

import com.pixelmed.dicom.DicomException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

/**
 *
 * @author Michal Sandanus
 */
public class Spectroscopy {
    private File spectroscopyExcel;
    private File spectroscopyDICOM;
    
    private double[] position = new double[3];
    private double[] vectorX = new double[3];
    private double[] vectorY = new double[3];
    private double[] vectorZ = new double[3];
    
    private double[] voxelDimensions = new double[3];
    
    private ArrayList<String> metabolites = new ArrayList<String>();
    
    private int numberOfVoxels = 0;
    private float[][] spectroscopicValues;
    
    private float[][] minValues;
    private float[][] maxValues;

    public float[][] getMinValues() {
        return minValues;
    }

    public void setMinValues(float[][] minValues) {
        this.minValues = minValues;
    }

    public float[][] getMaxValues() {
        return maxValues;
    }

    public void setMaxValues(float[][] maxValues) {
        this.maxValues = maxValues;
    }
    
    public File getSpectroscopyExcel() {
        return spectroscopyExcel;
    }

    public double[] getPosition() {
        return position;
    }

    public double[] getVectorX() {
        return vectorX;
    }

    public double[] getVectorY() {
        return vectorY;
    }

    public double[] getVectorZ() {
        return vectorZ;
    }

    public double[] getVoxelDimensions() {
        return voxelDimensions;
    }

    public ArrayList<String> getMetabolites() {
        return metabolites;
    }

    public int getNumberOfVoxels() {
        return numberOfVoxels;
    }

    public float[][] getSpectroscopicValues() {
        return spectroscopicValues;
    }
    
    public void setSpectroscopyExcel(File spectroscopyFile) {
        this.spectroscopyExcel = spectroscopyFile;
    }

    public void setPosition(double[] position) {
        this.position = position;
    }

    public void setVectorX(double[] vectorX) {
        this.vectorX = vectorX;
    }

    public void setVectorY(double[] vectorY) {
        this.vectorY = vectorY;
    }

    public void setVoxelDimensions(double[] voxelDimensions) {
        this.voxelDimensions = voxelDimensions;
    }

    public File getSpectroscopyDICOM() {
        return spectroscopyDICOM;
    }

    public void setSpectroscopyDICOM(File spectroscopyDICOM) {
        this.spectroscopyDICOM = spectroscopyDICOM;
    }
    
    public void readSpectroscopyDICOM() throws DicomException, IOException {
        String imagePositionPatient = "ImagePositionPatient";
        String imageOrientationPatient = "ImageOrientationPatient";
        String dimZ = "sSliceArray.asSlice[0].dThickness";
        String dimX = "sSliceArray.asSlice[0].dReadoutFOV";
        String dimY = "sSliceArray.asSlice[0].dPhaseFOV";
        
        String searchedString = "";
        
        String[] valuesStr = new String[12]; 
        
        int counter = 0;
        boolean flagReading = false;
        String numberStr = "";
        
        int counterM = 0;
        
      
        boolean IPP = false;        
        boolean IOP = false;
        boolean DIMZ = false;
        boolean DIMY = false;
        boolean DIMX = false;
        
        FileReader fr = new FileReader(spectroscopyDICOM);
        BufferedReader br = new BufferedReader(fr);
                
        int c = 0;
        
        //reading ImagePositionPatient
        while((c = br.read()) != -1)         
        {
            char character = (char) c;
            
            if (!IPP) {
                if (character == 'I') {
                    searchedString = "I";
                }
                else if (searchedString == "") continue;
                else {
                    if (character < '0' || (character > '9' && character < 'A') || (character > 'Z' && character < 'a') || character > 'z' || character == 'd') searchedString += "\\";
                    searchedString = searchedString + character;
                    if (imagePositionPatient.matches(searchedString)) {
                        IPP = true;
                        searchedString = "";
                    }
                    else if (!imagePositionPatient.matches(searchedString + "(.*)")) {
                        searchedString = "";
                    }
                }
            }
            
            else {
                if (character == 'M') {
                    counterM++;
                }
                if (counterM >= 2) {
                    if (!flagReading){
                        if (character == '-' || (character >= '0' && character <= '9')) {
                            flagReading = true;
                            numberStr += character;
                        }
                    }
                    else {
                        if (c == 0) {
                            valuesStr[counter] = numberStr;
                            counter++;
                            numberStr = "";
                            flagReading = false;
                            if (counter == 3) break;
                        }
                        else {
                            numberStr += character;
                        }
                    }
                }
            }
        }
        
        counterM = 0;
        
        //reading ImageOrientationPatient
        while((c = br.read()) != -1)         
        {
            char character = (char) c;
            
            if (!IOP) {
                if (character == 'I') {
                    searchedString = "I";
                }
                else if (searchedString == "") continue;
                else {
                    if (character < '0' || (character > '9' && character < 'A') || (character > 'Z' && character < 'a') || character > 'z') searchedString += "\\";
                    searchedString = searchedString + character;
                    if (imageOrientationPatient.matches(searchedString)) {
                        IOP = true;
                        searchedString = "";
                    }
                    else if (!imageOrientationPatient.matches(searchedString + "(.*)")) {
                        searchedString = "";
                    }
                }
            }
            
            else {
                if (character == 'M') {
                    counterM++;
                }
                if (counterM >= 2) {
                    if (!flagReading){
                        if (character == '-' || (character >= '0' && character <= '9')) {
                            flagReading = true;
                            numberStr += character;
                        }
                    }
                    else {
                        if (c == 0) {
                            valuesStr[counter] = numberStr;
                            counter++;
                            numberStr = "";
                            flagReading = false;
                            if (counter == 9) break;
                        }
                        else {
                            numberStr += character;
                        }
                    }                    
                }
            }
        }
        
        //reading spectroscopy dimension Z
        while((c = br.read()) != -1)         
        {
            char character = (char) c;
            
            if (!DIMZ) {
                if (character == 's' && searchedString.equals("")) {
                    searchedString = "s";
                }
                else if (searchedString == "") continue;
                else {
                    if (character < '0' || (character > '9' && character < 'A') || (character > 'Z' && character < 'a') || character > 'z') searchedString += "\\";
                    searchedString = searchedString + character;
                    if (dimZ.matches(searchedString)) {
                        DIMZ = true;
                        searchedString = "";
                    }
                    else if (!dimZ.matches(searchedString + "(.*)")) {
                        searchedString = "";
                    }
                }
            }
            
            else {
                if (!flagReading){
                    if (character >= '0' && character <= '9') {
                        flagReading = true;
                        numberStr += character;
                    }
                }
                else {
                    if (c == 0 || character == '\n') {
                        valuesStr[counter] = numberStr;
                        counter++;
                        numberStr = "";
                        flagReading = false;
                        break;
                    }
                    else {
                        numberStr += character;
                    }
                }
            }
        }
        
        //reading spectroscopy dimension Y
        while((c = br.read()) != -1)         
        {
            char character = (char) c;
            
            if (!DIMY) {
                if (character == 's' && searchedString.equals("")) {
                    searchedString = "s";
                }
                else if (searchedString == "") continue;
                else {
                    if (character < '0' || (character > '9' && character < 'A') || (character > 'Z' && character < 'a') || character > 'z') searchedString += "\\";
                    searchedString = searchedString + character;
                    if (dimY.matches(searchedString)) {
                        DIMY = true;
                        searchedString = "";
                    }
                    else if (!dimY.matches(searchedString + "(.*)")) {
                        searchedString = "";
                    }
                }
            }
            
            else {
                if (!flagReading){
                    if (character >= '0' && character <= '9') {
                        flagReading = true;
                        numberStr += character;
                    }
                }
                else {
                    if (c == 0 || character == '\n') {
                        valuesStr[counter] = numberStr;
                        counter++;
                        numberStr = "";
                        flagReading = false;
                        break;
                    }
                    else {
                        numberStr += character;
                    }
                }
            }
        }
        
        //reading spectroscopy dimension X
        while((c = br.read()) != -1) {
            char character = (char) c;
            
            if (!DIMX) {
                if (character == 's' && searchedString.equals("")) {
                    searchedString = "s";
                }
                else if (searchedString == "") continue;
                else {
                    if (character < '0' || (character > '9' && character < 'A') || (character > 'Z' && character < 'a') || character > 'z') searchedString += "\\";
                    searchedString = searchedString + character;
                    if (dimX.matches(searchedString)) {
                        DIMX = true;
                        searchedString = "";
                    }
                    else if (!dimX.matches(searchedString + "(.*)")) {
                        searchedString = "";
                    }
                }
            }
            
            else {
                if (!flagReading){
                    if (character >= '0' && character <= '9') {
                        flagReading = true;
                        numberStr += character;
                    }
                }
                else {
                    if (c == 0 || character == '\n') {
                        valuesStr[counter] = numberStr;
                        counter++;
                        numberStr = "";
                        flagReading = false;
                        break;
                    }
                    else {
                        numberStr += character;
                    }
                }
            }
        }
        
        position[0] = Double.parseDouble(valuesStr[0]);
        position[1] = Double.parseDouble(valuesStr[1]);
        position[2] = Double.parseDouble(valuesStr[2]);
        vectorX[0] = Double.parseDouble(valuesStr[3]);
        vectorX[1] = Double.parseDouble(valuesStr[4]);
        vectorX[2] = Double.parseDouble(valuesStr[5]);
        vectorY[0] = Double.parseDouble(valuesStr[6]);
        vectorY[1] = Double.parseDouble(valuesStr[7]);
        vectorY[2] = Double.parseDouble(valuesStr[8]);
        voxelDimensions[2] = Double.parseDouble(valuesStr[9]) / 16.0;
        voxelDimensions[0] = Double.parseDouble(valuesStr[10]) / 32.0;
        voxelDimensions[1] = Double.parseDouble(valuesStr[11]) / 32.0;
    }
    
    public void readSpectroscopyExcel() throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(spectroscopyExcel);
        HSSFWorkbook wb = new HSSFWorkbook(fis);
        HSSFSheet sheet = wb.getSheetAt(0); 
        
        //getting header
        Row header = sheet.getRow(2);
        for (Cell cell: header) {
            if (cell.getColumnIndex() == 0) continue;
            else {
                String columnName = cell.getStringCellValue();
                if (columnName.toLowerCase().contains("concentration")) {
                    metabolites.add(columnName.substring(0, columnName.indexOf(":")));
                }
            }
        }
        
        spectroscopicValues = new float[sheet.getPhysicalNumberOfRows() - 2][metabolites.size() + 3];
        numberOfVoxels = sheet.getPhysicalNumberOfRows() - 2;
        
        //getting data
        for (int i = 0; i < numberOfVoxels; i++){
            Row row = sheet.getRow(i + 3);
            int indexCounter = 3;
            for (Cell cell: row){
                if (cell.getColumnIndex() == 0) {
                    String voxCoordString[] = cell.getStringCellValue().split("_");
                    spectroscopicValues[i][0] = Float.parseFloat(voxCoordString[0]);
                    spectroscopicValues[i][1] = Float.parseFloat(voxCoordString[1]);
                    spectroscopicValues[i][2] = Float.parseFloat(voxCoordString[2]);
                }                
                else if (header.getCell(cell.getColumnIndex()).getStringCellValue().toLowerCase().contains("concentration")) {
                    try {
                        if (cell.getStringCellValue().equalsIgnoreCase("NaN")) spectroscopicValues[i][indexCounter] = 0.0f;
                        else spectroscopicValues[i][indexCounter] = Float.parseFloat(cell.getStringCellValue());
                    }
                    catch (Exception ex) {
                        spectroscopicValues[i][indexCounter] = 0.0f;
                    }
                    indexCounter++;
                }
            }
            indexCounter = 3;
        }
    }
    
    //calculating cross product of vector x and y to find vector that is perpendicular to x and y vector
    private void calculateVectorZ(){
        vectorZ[0] = vectorX[1] * vectorY[2] - vectorX[2] * vectorY[1];
        vectorZ[1] = (vectorX[0] * vectorY[2] - vectorX[2] * vectorY[0]) * (-1.0);
        vectorZ[2] = vectorX[0] * vectorY[1] - vectorX[1] * vectorY[0];
    }
    
    public void preprocess(File patientDir) throws FileNotFoundException, IOException, DicomException {
        readSpectroscopyDICOM();
        readSpectroscopyExcel();
        calculateVectorZ();
    }
    

}
