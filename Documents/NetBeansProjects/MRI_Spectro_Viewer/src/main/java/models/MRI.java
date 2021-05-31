/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models;

import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.DicomException;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.dcm4che3.imageio.plugins.dcm.*;
import org.w3c.dom.Document;

/**
 *
 * @author Michal Sandanus
 */
public class MRI {

    private File mriDir;
    private float[][][] mriArray;
    
    private int framesCount;
    private int rowsCount;
    private int columnsCount;
    
    private int minPixelValue;
    private int maxPixelValue;
    
    private double[] vectorX = new double[3];
    private double[] vectorY = new double[3];
    private double[] vectorZ = new double[3];
    
    private double[] position = new double[3];
    
    private double pixelSpacingX;
    private double pixelSpacingY;    
    private double sliceThickness;

    public File getMriDir() {
        return mriDir;
    }

    public float[][][] getMriArray() {
        return mriArray;
    }

    public int getRowsCount() {
        return rowsCount;
    }

    public int getColumnsCount() {
        return columnsCount;
    }

    public int getMinPixelValue() {
        return minPixelValue;
    }

    public int getMaxPixelValue() {
        return maxPixelValue;
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

    public double[] getPosition() {
        return position;
    }

    public double getPixelSpacingX() {
        return pixelSpacingX;
    }

    public double getPixelSpacingY() {
        return pixelSpacingY;
    }

    public double getSliceThickness() {
        return sliceThickness;
    }

    public void setMriDir(File mriDir) {
        this.mriDir = mriDir;
    }

    public int getFramesCount() {
        return framesCount;
    }
    
    /**
     * Get and sets number of MRI frames
     */
    public void setFramesCount() {
        framesCount = 0;
    
        for (final File fileDicom : mriDir.listFiles()) {
            if (fileDicom.isDirectory()) continue;
            else framesCount++;
        }
    }
    
    /**
     * Get and sets number of rows and columns in MRI frame
     * @throws IOException
     */
    public void setRowsColumnsCount() throws IOException{
        for (final File fileDicom : mriDir.listFiles()){
            if (fileDicom.isDirectory()) continue; 
            else {
                Raster raster = null ;
      
                //Open the DICOM file and get its pixel data
                Iterator iter = ImageIO.getImageReadersByFormatName("DICOM");
                ImageReader reader = (ImageReader) iter.next();
                DicomImageReadParam param = (DicomImageReadParam) reader.getDefaultReadParam();
                ImageInputStream iis = ImageIO.createImageInputStream(fileDicom);
                reader.setInput(iis, false);

                //Returns a new Raster (rectangular array of pixels) containing the raw pixel data from the image stream
                raster = reader.readRaster(0, param);
                if (raster == null)
                       System.out.println("Error: couldn't read Dicom image!");

                rowsCount = raster.getHeight();
                columnsCount = raster.getWidth();
                iis.close();
            }
        }  
    }
    
    /**
     * Creates 3D java array with dimensions [frames][columns][rows] from MRI images
     * @throws IOException
     */
    public void createMriArray() throws IOException{
        mriArray = new float[framesCount][columnsCount][rowsCount];
        int actualFrame = 0;
        
        for (final File fileDicom : mriDir.listFiles()){
            if (fileDicom.isDirectory()) continue; 
            else {
                Raster raster = null ;

                //Open the DICOM file and get its pixel data
                Iterator iter = ImageIO.getImageReadersByFormatName("DICOM");
                ImageReader reader = (ImageReader) iter.next();
                DicomImageReadParam param = (DicomImageReadParam) reader.getDefaultReadParam();
                ImageInputStream iis = ImageIO.createImageInputStream(fileDicom);
                reader.setInput(iis, false);
                //Returns a new Raster (rectangular array of pixels) containing the raw pixel data from the image stream
                raster = reader.readRaster(0, param);
                if (raster == null)
                       System.out.println("Error: couldn't read Dicom image!");

                float[][] frameArray = new float[columnsCount][rowsCount];
                for (int x = 0; x < columnsCount; x++) {
                    for (int y = 0; y < rowsCount; y++) {
                        frameArray[x][y] = (float)raster.getSample(x, y, 0);
                    }
                }

                mriArray[actualFrame] = frameArray;
                actualFrame++;

                iis.close();
            }
        }
    }
    
    /**
     * Saves created 3D java array for MRI to directory patientDir + "/Arrays/mri.arr"
     * @param patientDir
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public void saveMriArray(File patientDir) throws FileNotFoundException, IOException {
        FileOutputStream f = new FileOutputStream(patientDir + "/Arrays/mri.arr");
        ObjectOutput s = new ObjectOutputStream(f);
        s.writeObject(mriArray);
    }
    
    /**
     * Gets and sets MRI array minimal and maximal value
     */
    public void getArrayMinMax(){
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (int z = 0; z < framesCount; z++) {
            for (int x = 0; x < columnsCount; x++) {
                for (int y = 0; y < rowsCount; y++) {
                    if (mriArray[z][x][y] < min) min = mriArray[z][x][y];
                    if (mriArray[z][x][y] > max) max = mriArray[z][x][y];
                }
            }
        }
        
        minPixelValue = (int)min;
        maxPixelValue = (int)max;
    }
    
    /**
     * Converts java 2D arrays of MRI to grayscaled png images and saves them to patientDir + "/MRI_Images/"
     * @param patientDir
     * @throws IOException 
     */
    public void convertToImages(File patientDir) throws IOException {
        for (int z = 0; z < framesCount; z++) {
            BufferedImage mriImage = new BufferedImage(columnsCount, rowsCount, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < columnsCount; x++) {
                for (int y = 0; y < rowsCount; y++) {
                    int convertedValue = (int)(((int)mriArray[z][x][y] + (Math.abs(minPixelValue))) * 255 / (maxPixelValue - minPixelValue));
                    int value = convertedValue << 16 | convertedValue << 8 | convertedValue;
                    mriImage.setRGB(x, y, value);
                }
            }
            
            File outputfile = new File(patientDir.getAbsolutePath() + "/MRI_Images/frame_" + z + ".png");
            
            if (! outputfile.exists()) outputfile.createNewFile();
            ImageIO.write(mriImage, "png", outputfile);
        }
    }
    
    /**
     * Gets MRI constants from the first frame: vectorX, vectorY, position vector, pixel spacing and spacing between slices
     * @throws IOException
     * @throws DicomException 
     */
    public void getMriConstants() throws IOException, DicomException {
        for (final File fileDicom : mriDir.listFiles()) {
            if (fileDicom.isDirectory()) continue;
            else {
                AttributeList list = new AttributeList();
                list.read(fileDicom);
                
                AttributeTag tagInstanceNumber = new AttributeTag("(0x0020,0x0013)");
                String instanceNumber = list.get(tagInstanceNumber).getStringValues()[0];
                if (Integer.parseInt(instanceNumber) != 1) continue;

                //vectors
                AttributeTag tagVectors = new AttributeTag("(0x0020,0x0037)");  
                String[] vectorsString = list.get(tagVectors).getStringValues();

                vectorX[0] = Double.parseDouble(vectorsString[0]);
                vectorX[1] = Double.parseDouble(vectorsString[1]);
                vectorX[2] = Double.parseDouble(vectorsString[2]);
                vectorY[0] = Double.parseDouble(vectorsString[3]);
                vectorY[1] = Double.parseDouble(vectorsString[4]);
                vectorY[2] = Double.parseDouble(vectorsString[5]);

                //position
                AttributeTag tagPosition = new AttributeTag("(0x0020,0x0032)"); 
                String positionString[] = list.get(tagPosition).getStringValues();

                position[0] = Double.parseDouble(positionString[0]);
                position[1] = Double.parseDouble(positionString[1]);
                position[2] = Double.parseDouble(positionString[2]);

                //pixel spacing
                AttributeTag tagPixelSpacing = new AttributeTag("(0x0028,0x0030)"); 
                String pixelSpacingString[] = list.get(tagPixelSpacing).getStringValues();

                pixelSpacingX = Double.parseDouble(pixelSpacingString[0]);
                pixelSpacingY = Double.parseDouble(pixelSpacingString[1]);

                //spacing between slices
                AttributeTag tagSpacingBetweenSlices = new AttributeTag("(0x0018,0x0088)"); 
                String spacingBetweenSlices[] = list.get(tagSpacingBetweenSlices).getStringValues();

                sliceThickness = Double.parseDouble(spacingBetweenSlices[0]);

                calculateVectorZ();

                break;
            }
        }
                
    }
    
    /**
     * Calculates vector Z that is perpendicular to vector X and Y 
     */
    private void calculateVectorZ(){
        vectorZ[0] = vectorX[1] * vectorY[2] - vectorX[2] * vectorY[1];
        vectorZ[1] = (vectorX[0] * vectorY[2] - vectorX[2] * vectorY[0]) * (-1.0);
        vectorZ[2] = vectorX[0] * vectorY[1] - vectorX[1] * vectorY[0];
    }
    
    /**
     * Preprocess MRI images, saves 3D java array, and creates PNG images
     * @param patientDir
     * @throws IOException
     * @throws DicomException 
     */
    public void preprocess(File patientDir) throws IOException, DicomException{
        setFramesCount();
        setRowsColumnsCount();
        createMriArray();
        //saveMriArray(patientDir);
        getArrayMinMax();
        convertToImages(patientDir);
        getMriConstants();
    }

}
