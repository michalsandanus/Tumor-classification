/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controllers;

import com.pixelmed.dicom.DicomException;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import models.Patient;

/**
 *
 * @author Michal Sandanus
 */
public class Coregistration {
    private Patient patient;
    private File workingDir;
    float[][][][] finalArray;
    int[][][] arrayIndexes;

    public Coregistration(Patient patient, File workingDir) {
        this.patient = patient;
        this.workingDir = workingDir;
    }

    public Patient getPatient() {
        return patient;
    }
    
    public void coregistrate() throws IOException, FileNotFoundException, ParserConfigurationException, TransformerException, DicomException {
        preprocess();
        createFinalArray();
        calculateSpectroscopyMinMax();
        createSpectroscopicImages();
    }
    
    public void preprocess() throws FileNotFoundException, IOException, ParserConfigurationException, TransformerException, DicomException{
        patient.getPersonalData();
        
        File patientDir = new File(workingDir.getAbsolutePath() + "/" + patient.getName().replace(' ','_') + "_" + patient.getId());
        if (! patientDir.exists()) patientDir.mkdir();
        
        File mriImagesDirectory = new File(patientDir.getAbsolutePath() + "/MRI_Images");
        if (! mriImagesDirectory.exists()) mriImagesDirectory.mkdir();
        
        File coregArrayDirectory = new File(patientDir.getAbsolutePath() + "/Arrays");
        if (! coregArrayDirectory.exists()) coregArrayDirectory.mkdir();
        
        Files.copy(Paths.get(patient.getSegmentationMask().getAbsolutePath()), Paths.get(patientDir.getAbsolutePath() + "/Arrays/Mask.nii"));
        
        patient.getMri().preprocess(new File(workingDir.getAbsolutePath() + "/" + patient.getName().replace(' ','_') + "_" + patient.getId()));
        
        File metabolitesDirectory = new File(patientDir.getAbsolutePath() + "/Metabolites_Images");
        if (! metabolitesDirectory.exists()) metabolitesDirectory.mkdir();
        
        patient.getSpectroscopy().preprocess(new File(workingDir.getAbsolutePath() + "/" + patient.getName().replace(' ','_') + "_" + patient.getId()));
        
        for (String metabolite: patient.getSpectroscopy().getMetabolites()){
            File metaboliteDirectory = new File(patientDir.getAbsolutePath() + "/Metabolites_Images/" + metabolite.replace(' ', '_'));
            if (! metaboliteDirectory.exists()) metaboliteDirectory.mkdir();
        }
        
        patient.generatePatientXml(patientDir);
    }
    
    private double[] multiplyArrayByConstant(double[] arr, double constant){
        double[] result = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = arr[i] * constant;
        }
        return result;
    }
    
    private double[] sumTwoArrays(double[] arr1, double[] arr2){
        double[] result = new double[arr1.length];
        for (int i = 0; i < arr1.length; i++) {
            result[i] = arr1[i] + arr2[i];
        }
        return result;
    }
    
    private double calculateDotProduct(double[] vector1, double[] vector2) {
        double dotProduct = 0.0;
        for (int i = 0; i < vector1.length; i++) dotProduct += vector1[i] * vector2[i];
        
        return dotProduct;
    }
    
    private double[] getCenterVoxelCoordinates(int index){
        double[] centerCoord = new double[3];
        double[] origin = patient.getSpectroscopy().getPosition();
        
        double[] x_shift = multiplyArrayByConstant(patient.getSpectroscopy().getVectorX(), (patient.getSpectroscopy().getVoxelDimensions()[1])*(patient.getSpectroscopy().getSpectroscopicValues()[index][1] - 1));
        double[] y_shift = multiplyArrayByConstant(patient.getSpectroscopy().getVectorY(), (patient.getSpectroscopy().getVoxelDimensions()[0])*(patient.getSpectroscopy().getSpectroscopicValues()[index][0] - 1));
        double[] z_shift = multiplyArrayByConstant(patient.getSpectroscopy().getVectorZ(), (patient.getSpectroscopy().getVoxelDimensions()[2])*(patient.getSpectroscopy().getSpectroscopicValues()[index][2] - 1));
        centerCoord = sumTwoArrays(sumTwoArrays(origin, x_shift), sumTwoArrays(y_shift, z_shift));
        
        return centerCoord;
    }
    
    private double getVectorLength(double[] vector){
        return Math.sqrt(Math.pow(vector[0], 2) + Math.pow(vector[1], 2) + Math.pow(vector[2], 2));
    }
    
    private double[][] getVoxelCubeCoordinatesPoints(double[] centerCoord){
        double[][] cubeCoordinates = new double[12][3];
        
        double[] vectorX = patient.getSpectroscopy().getVectorX();
        double[] vectorY = patient.getSpectroscopy().getVectorY();
        double[] vectorZ = patient.getSpectroscopy().getVectorZ();

        double x_shift = patient.getSpectroscopy().getVoxelDimensions()[1] / 2.0;
        double y_shift = patient.getSpectroscopy().getVoxelDimensions()[0] / 2.0;
        double z_shift = patient.getSpectroscopy().getVoxelDimensions()[2] / 2.0;
        
        //point 1
        cubeCoordinates[0] = sumTwoArrays(sumTwoArrays(centerCoord, multiplyArrayByConstant(vectorX, -1.0 * x_shift)), 
                                            sumTwoArrays(multiplyArrayByConstant(vectorY, -1.0 * y_shift), multiplyArrayByConstant(vectorZ, -1.0 * z_shift)));
        
        //point2
        cubeCoordinates[1] = sumTwoArrays(sumTwoArrays(centerCoord, multiplyArrayByConstant(vectorX, x_shift)), 
                                            sumTwoArrays(multiplyArrayByConstant(vectorY, -1.0 * y_shift), multiplyArrayByConstant(vectorZ, -1.0 * z_shift)));
        
        //point3
        cubeCoordinates[2] = sumTwoArrays(sumTwoArrays(centerCoord, multiplyArrayByConstant(vectorX, x_shift)), 
                                            sumTwoArrays(multiplyArrayByConstant(vectorY, y_shift), multiplyArrayByConstant(vectorZ, -1.0 * z_shift)));
        
        //point4
        cubeCoordinates[3] = sumTwoArrays(sumTwoArrays(centerCoord, multiplyArrayByConstant(vectorX, -1.0 * x_shift)), 
                                            sumTwoArrays(multiplyArrayByConstant(vectorY, y_shift), multiplyArrayByConstant(vectorZ, -1.0 * z_shift)));
        
        //point5
        cubeCoordinates[4] = sumTwoArrays(sumTwoArrays(centerCoord, multiplyArrayByConstant(vectorX, -1.0 * x_shift)), 
                                            sumTwoArrays(multiplyArrayByConstant(vectorY, -1.0 * y_shift), multiplyArrayByConstant(vectorZ, z_shift)));
        
        //point6
        cubeCoordinates[5] = sumTwoArrays(sumTwoArrays(centerCoord, multiplyArrayByConstant(vectorX, x_shift)), 
                                            sumTwoArrays(multiplyArrayByConstant(vectorY, -1.0 * y_shift), multiplyArrayByConstant(vectorZ, z_shift)));
        
        //point7
        cubeCoordinates[6] = sumTwoArrays(sumTwoArrays(centerCoord, multiplyArrayByConstant(vectorX, x_shift)), 
                                            sumTwoArrays(multiplyArrayByConstant(vectorY, y_shift), multiplyArrayByConstant(vectorZ, z_shift)));
        
        //point8
        cubeCoordinates[7] = sumTwoArrays(sumTwoArrays(centerCoord, multiplyArrayByConstant(vectorX, -1.0 * x_shift)), 
                                            sumTwoArrays(multiplyArrayByConstant(vectorY, y_shift), multiplyArrayByConstant(vectorZ, z_shift)));
        
        //local unity vectors
        double[] dir1 = sumTwoArrays(cubeCoordinates[4], multiplyArrayByConstant(cubeCoordinates[0], -1.0));
        double size1 = getVectorLength(dir1);
        cubeCoordinates[8] = multiplyArrayByConstant(dir1, (1.0 / size1));
        
        double[] dir2 = sumTwoArrays(cubeCoordinates[1], multiplyArrayByConstant(cubeCoordinates[0], -1.0));
        double size2 = getVectorLength(dir2);
        cubeCoordinates[9] = multiplyArrayByConstant(dir2, (1.0 / size2));
        
        double[] dir3 = sumTwoArrays(cubeCoordinates[3], multiplyArrayByConstant(cubeCoordinates[0], -1.0));
        double size3 = getVectorLength(dir3);
        cubeCoordinates[10] = multiplyArrayByConstant(dir3, (1.0 / size3));
        
        //local unity vector size
        cubeCoordinates[11][0] = size1;
        cubeCoordinates[11][1] = size2;
        cubeCoordinates[11][2] = size3;
        
        return cubeCoordinates;
    }
    
    private double[][][] getSpectroscopicCoordinatesArray(){
        double[][][] spectroscopicCoordinates = new double[patient.getSpectroscopy().getNumberOfVoxels()][13][3];
        
        for (int i = 0; i < patient.getSpectroscopy().getNumberOfVoxels(); i++) {
            
            double[] centerCoord = getCenterVoxelCoordinates(i);
            
            //coordinates of voxel in magnet coordinate system
            spectroscopicCoordinates[i][0][0] = centerCoord[0];
            spectroscopicCoordinates[i][0][1] = centerCoord[1];
            spectroscopicCoordinates[i][0][2] = centerCoord[2];
            
            double[][] cubeCoordinates = getVoxelCubeCoordinatesPoints(centerCoord);
            
            for (int j = 0; j < 12; j++) spectroscopicCoordinates[i][j+1] = cubeCoordinates[j];
        }
        
        return spectroscopicCoordinates;
    }
    
    private double[] findMinMaxSpectroscopyCoordinates(double[][][] spectroscopicCoordinates) {
        double[] minMaxCoordinates = new double[6];
        
        double min_x = Double.MAX_VALUE;
        double min_y = Double.MAX_VALUE;
        double min_z = Double.MAX_VALUE;
        
        double max_x = Double.MIN_VALUE;
        double max_y = Double.MIN_VALUE;
        double max_z = Double.MIN_VALUE;
        
        for (int i = 0; i < spectroscopicCoordinates.length; i++) {
            for (int j = 1; j < 9; j++) {
                if (min_x > spectroscopicCoordinates[i][j][0]) min_x = spectroscopicCoordinates[i][j][0];
                if (min_y > spectroscopicCoordinates[i][j][1]) min_y = spectroscopicCoordinates[i][j][1];
                if (min_z > spectroscopicCoordinates[i][j][2]) min_z = spectroscopicCoordinates[i][j][2];
                
                if (max_x < spectroscopicCoordinates[i][j][0]) max_x = spectroscopicCoordinates[i][j][0];
                if (max_y < spectroscopicCoordinates[i][j][1]) max_y = spectroscopicCoordinates[i][j][1];
                if (max_z < spectroscopicCoordinates[i][j][2]) max_z = spectroscopicCoordinates[i][j][2];
            }
        }
        
        minMaxCoordinates[0] = min_x;
        minMaxCoordinates[1] = min_y;
        minMaxCoordinates[2] = min_z;
        
        minMaxCoordinates[3] = max_x;
        minMaxCoordinates[4] = max_y;
        minMaxCoordinates[5] = max_z;
        
        return minMaxCoordinates;
    }
    
    private double[] getMriPointCoordinates(int x, int y, int z) {
        double[] vectorX = patient.getMri().getVectorX();
        double[] vectorY = patient.getMri().getVectorY();
        double[] vectorZ = patient.getMri().getVectorZ();
        
        double[] position = patient.getMri().getPosition();
        
        double pixelSpacingX = patient.getMri().getPixelSpacingX();
        double pixelSpacingY = patient.getMri().getPixelSpacingY();    
        double sliceThickness = patient.getMri().getSliceThickness();
        
        return sumTwoArrays(sumTwoArrays(position, multiplyArrayByConstant(vectorX, (double)x * pixelSpacingX)), 
                sumTwoArrays(multiplyArrayByConstant(vectorY, (double)y * pixelSpacingY), multiplyArrayByConstant(vectorZ, (double)z * sliceThickness)));
    }
    
    private boolean insideTest(double[] mriPointCoord, double[][] spectroscopicCoordinates) {
        double[] dirVector = sumTwoArrays(mriPointCoord, multiplyArrayByConstant(spectroscopicCoordinates[0], -1.0));
        
        if (Math.abs(calculateDotProduct(dirVector, spectroscopicCoordinates[9])) * 2.0 > spectroscopicCoordinates[12][0] ||
                Math.abs(calculateDotProduct(dirVector, spectroscopicCoordinates[10])) * 2.0 > spectroscopicCoordinates[12][1] ||
                Math.abs(calculateDotProduct(dirVector, spectroscopicCoordinates[11])) * 2.0 > spectroscopicCoordinates[12][2]) return false;
        
        return true;
    }
    
    private int getSpectroscopyVoxelIndex(double[][][] spectroscopicCoordinates, double[] mriPointCoordinates) {
        for (int i = 0; i < spectroscopicCoordinates.length; i++) {
            if (insideTest(mriPointCoordinates, spectroscopicCoordinates[i]))
                return i;
        }
        return -1;
    }
    
    private int[][] coregistrateFrame(int frame, double[] minMaxCoordinates, double[][][] spectroscopicCoordinates) {
        int[][] arrayIndexesFrame = new int[patient.getMri().getColumnsCount()][patient.getMri().getRowsCount()];
        
        for (int x = 0; x < patient.getMri().getColumnsCount(); x++) {
            for (int y = 0; y < patient.getMri().getRowsCount(); y++){
                double[] mriPointCoord = getMriPointCoordinates(x, y, frame);
                if (mriPointCoord[0] < minMaxCoordinates[0] || mriPointCoord[0] > minMaxCoordinates[3] || 
                        mriPointCoord[1] < minMaxCoordinates[1] || mriPointCoord[1] > minMaxCoordinates[4] ||
                        mriPointCoord[2] < minMaxCoordinates[2] || mriPointCoord[2] > minMaxCoordinates[5]) arrayIndexesFrame[x][y] = -1;
                else {
                    arrayIndexesFrame[x][y] = getSpectroscopyVoxelIndex(spectroscopicCoordinates, mriPointCoord);
                }
            }
        }
        
        return arrayIndexesFrame;
    }
    
    public void createFinalArray() {   
        finalArray = new float[patient.getSpectroscopy().getMetabolites().size() + 1]
                                            [patient.getMri().getFramesCount()]
                                            [patient.getMri().getColumnsCount()]
                                            [patient.getMri().getRowsCount()];
        
        arrayIndexes = new int[patient.getMri().getFramesCount()]
                                        [patient.getMri().getColumnsCount()]
                                        [patient.getMri().getRowsCount()];
        
        double[][][] spectroscopicCoordinates = getSpectroscopicCoordinatesArray();
        double[] minMaxCoordinates = findMinMaxSpectroscopyCoordinates(spectroscopicCoordinates);
        float[][] spectroscopicValues = patient.getSpectroscopy().getSpectroscopicValues();
        
        System.out.println("Coregistration started");
        
        for (int frame = 0; frame < patient.getMri().getFramesCount(); frame++) {
            arrayIndexes[frame] = coregistrateFrame(frame, minMaxCoordinates, spectroscopicCoordinates);
        }
        
        finalArray[0] = patient.getMri().getMriArray();
        for (int frame = 0; frame < patient.getMri().getFramesCount(); frame++) {
            for (int row = 0; row < patient.getMri().getRowsCount(); row++) {
                for (int column = 0; column < patient.getMri().getColumnsCount(); column++){
                    if (arrayIndexes[frame][column][row] != -1) {
                        for (int metabolite = 1; metabolite <= patient.getSpectroscopy().getMetabolites().size(); metabolite++) {
                            finalArray[metabolite][frame][column][row] = spectroscopicValues[arrayIndexes[frame][column][row]][metabolite + 2];
                        }
                    }
                }
            }
        }
        
        System.out.println("Coregistration finished");
        
        try{
            FileWriter myWriter = new FileWriter(workingDir.getAbsolutePath() + "/" + patient.getName().replace(' ','_') + "_" + patient.getId() + "/Arrays/CoregArray.txt");
            
            for (int metabolite = 0; metabolite <= patient.getSpectroscopy().getMetabolites().size(); metabolite++) {
                System.out.println("Writing metabolite " + String.valueOf(metabolite));
                for (int frame = 0; frame < patient.getMri().getFramesCount(); frame++) {
                    for (int column = 0; column < patient.getMri().getColumnsCount(); column++) {
                        for (int row = 0; row < patient.getMri().getRowsCount(); row++) {
                            myWriter.write(String.valueOf(finalArray[metabolite][frame][column][row]) + "\n");
                        }
                    }
                }
            }
            
            myWriter.close();
        }
        catch (Exception ex) {
            System.out.println("Saving array error");
        }
        
        try {
            FileWriter myWriter = new FileWriter(workingDir.getAbsolutePath() + "/" + patient.getName().replace(' ','_') + "_" + patient.getId() + "/Arrays/ArrayIndexes.txt");
            
            for (int frame = 0; frame < patient.getMri().getFramesCount(); frame++) {
                for (int column = 0; column < patient.getMri().getColumnsCount(); column++) {
                    for (int row = 0; row < patient.getMri().getRowsCount(); row++) {
                        myWriter.write(String.valueOf(arrayIndexes[frame][column][row]) + "\n");
                    }
                }
            }
            
            myWriter.close();
        }
        catch (Exception ex) {
            System.out.println("Saving array error");
        }
        
        try {
            FileWriter myWriter = new FileWriter(workingDir.getAbsolutePath() + "/" + patient.getName().replace(' ','_') + "_" + patient.getId() + "/Arrays/Dimensions.txt");
            
            myWriter.write(String.valueOf(patient.getSpectroscopy().getMetabolites().size() + 1) + "\n");
            myWriter.write(String.valueOf(patient.getMri().getFramesCount()) + "\n");
            myWriter.write(String.valueOf(patient.getMri().getColumnsCount()) + "\n");
            myWriter.write(String.valueOf(patient.getMri().getRowsCount()));
            
            myWriter.close();
        }
        catch (Exception ex) {
            System.out.println("Saving array error");
        }
    }
    
    private void calculateSpectroscopyMinMax() {
        patient.getSpectroscopy().setMinValues(new float[patient.getSpectroscopy().getMetabolites().size()][patient.getMri().getFramesCount()]);
        patient.getSpectroscopy().setMaxValues(new float[patient.getSpectroscopy().getMetabolites().size()][patient.getMri().getFramesCount()]);
        
        for (int metabolite = 1; metabolite <= patient.getSpectroscopy().getMetabolites().size(); metabolite++) {           
            for (int frame = 0; frame < patient.getMri().getFramesCount(); frame++) {
                
                float min = Float.MAX_VALUE;
                float max = Float.MIN_VALUE;
                
                for (int row = 0; row < patient.getMri().getRowsCount(); row++) {
                    for (int column = 0; column < patient.getMri().getColumnsCount(); column++){
                        if (arrayIndexes[frame][column][row] != -1) {
                            if (finalArray[metabolite][frame][column][row] < min) min = finalArray[metabolite][frame][column][row];
                            if (finalArray[metabolite][frame][column][row] > max) max = finalArray[metabolite][frame][column][row];
                        }
                    }
                }
                
                patient.getSpectroscopy().getMinValues()[metabolite - 1][frame] = min;
                patient.getSpectroscopy().getMaxValues()[metabolite - 1][frame] = max;
            }
        }
    }
    
    private int getColor(double v, double vmin, double vmax) {
        double r = 1.0;
        double g = 1.0;
        double b = 1.0;
        double dv;

        if (v < vmin)
           v = vmin;
        if (v > vmax)
           v = vmax;
        dv = vmax - vmin;

        if (v < (vmin + 0.25 * dv)) {
           r = 0;
           g = 4 * (v - vmin) / dv;
        } else if (v < (vmin + 0.5 * dv)) {
           r = 0;
           b = 1 + 4 * (vmin + 0.25 * dv - v) / dv;
        } else if (v < (vmin + 0.75 * dv)) {
           r = 4 * (v - vmin - 0.5 * dv) / dv;
           b = 0;
        } else {
           g = 1 + 4 * (vmin + 0.75 * dv - v) / dv;
           b = 0;
        }
        
        int red = (int)(r * 255.0);
        int green = (int)(g * 255.0);
        int blue = (int)(b * 255.0);

        int value = 60 << 24 | red << 16 | green << 8 | blue;
        
        return(value);
    }
    
    private boolean compareFloatToValue(float fVal, int iVal){
        float threshold = 0.0001f;
        return ((fVal >= ((float)iVal) - threshold) && (fVal <= ((float)iVal) - threshold));
    } 
    
    public void createSpectroscopicImages() throws IOException{
        final int ARRAY_SIZE = 100;
        final int MAX_COLOR = 240;
        final int MIN_COLOR = 0;
        
        double jump = (MAX_COLOR - MIN_COLOR) / (ARRAY_SIZE*1.0);
        Color[] colors = new Color[ARRAY_SIZE];
        for (int i = 0; i < colors.length; i++) {
            int rgb = Color.HSBtoRGB((float) (MIN_COLOR + (jump*i)), 1.0f, 1.0f);
            Color color = new Color(rgb);
            colors[i] = color;
        }
        
        for (int metabolite = 0; metabolite < patient.getSpectroscopy().getMetabolites().size(); metabolite++) {
            for (int frame = 0; frame < patient.getMri().getFramesCount(); frame++) {
                BufferedImage spectroscopyImage = new BufferedImage(patient.getMri().getColumnsCount(), patient.getMri().getRowsCount(), BufferedImage.TYPE_INT_ARGB);
                for (int column = 0; column < patient.getMri().getColumnsCount(); column++) {
                    for (int row = 0; row < patient.getMri().getRowsCount(); row++) {
                        int value;
                        if (arrayIndexes[frame][column][row] == -1) value = 0;
                        else {                            
                            if (compareFloatToValue(finalArray[metabolite + 1][frame][column][row], 0)) value = 0;
                            else value = getColor(finalArray[metabolite + 1][frame][column][row], 
                                    patient.getSpectroscopy().getMinValues()[metabolite][frame], 
                                    patient.getSpectroscopy().getMaxValues()[metabolite][frame]);
                        } 
                        spectroscopyImage.setRGB(column, row, value);
                    }
                }
                
                File outputfile = new File(workingDir.getAbsolutePath() + "/" + patient.getName().replace(' ','_') + "_" + patient.getId() + "/Metabolites_Images/" + 
                    patient.getSpectroscopy().getMetabolites().get(metabolite).replace(' ', '_') + "/frame_" + frame + ".png");
            
                if (! outputfile.exists()) outputfile.createNewFile();
                ImageIO.write(spectroscopyImage, "png", outputfile);
            }
        }
    }
}
