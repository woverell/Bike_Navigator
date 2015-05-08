package com.example.williamoverell.bikenavigator;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.CvType;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by williamoverell on 12/5/14.
 */
public class LanePreprocessor {

    Mat hierarchy; // used for finding contours

    Mat dilationKernel;
    Mat erosionKernel;
    Mat line;

    List<MatOfPoint> contours;

    private int huelow;
    private int huehigh;
    private int satlow;
    private int sathigh;
    private int vallow;
    private int valhigh;

    private String instructionString;

    boolean performOpenSwitch;

    public int getHuelow() {
        return huelow;
    }

    public void setHuelow(int huelow) {
        this.huelow = huelow;
    }

    public int getHuehigh() {
        return huehigh;
    }

    public void setHuehigh(int huehigh) {
        this.huehigh = huehigh;
    }

    public int getSatlow() {
        return satlow;
    }

    public void setSatlow(int satlow) {
        this.satlow = satlow;
    }

    public int getSathigh() {
        return sathigh;
    }

    public void setSathigh(int sathigh) {
        this.sathigh = sathigh;
    }

    public int getVallow() {
        return vallow;
    }

    public void setVallow(int vallow) {
        this.vallow = vallow;
    }

    public int getValhigh() {
        return valhigh;
    }

    public void setValhigh(int valhigh) {
        this.valhigh = valhigh;
    }

    public LanePreprocessor()
    {
        // erosion/dilation defaults
        dilationKernel = Mat.ones(2,2,CvType.CV_8U);
        erosionKernel = Mat.ones(2,2,CvType.CV_8U);
        performOpenSwitch = false;

        // Color threshold defaults
        huelow = 0;
        huehigh = 180;
        satlow = 0;
        sathigh = 4;
        vallow = 200;
        valhigh = 255;

        hierarchy = new Mat();

        line = new Mat();

    }

    /**
     * This function finds lane line in the frame provided and draws the found line
     * over the rgbaFrame and stores this in lineFrame, it also puts the found blobs
     * in blobFrame so that we can see the binary image of the blobs when adjusting
     * the processing.
     * @param rgbaFrame
     * @param lineFrame
     * @param blobFrame
     */
    public void findLaneLine(Mat rgbaFrame, Mat lineFrame, Mat blobFrame)
    {
        // Segment the Image
        contours = new ArrayList<MatOfPoint>();
        findBlobs(blobFrame);

        if(!contours.isEmpty()) {

            processBlobs(rgbaFrame);
        }

    }

    /**
     * This function will perform a color threshold of the rgbaFrame
     * and prepare the image for blob detection.  If performOpenSwitch
     * is true then perform an open on the processedFrame.
     * @param rgbaFrame
     * @param blobFrame
     */
    public void processImage(Mat rgbaFrame, Mat blobFrame)
    {
        // Perform color thresholding
        Imgproc.cvtColor(rgbaFrame, blobFrame,Imgproc.COLOR_RGB2HSV);
        // Hue Range: 0-180(all hues) Saturation Range: 0-12, Value Range: 220-255
        Core.inRange(blobFrame, new Scalar(huelow, satlow, vallow), new Scalar(huehigh,sathigh,valhigh), blobFrame);

        if(this.performOpenSwitch)
        {
            // Perform dilation and erosion
            Imgproc.dilate(blobFrame, blobFrame, dilationKernel); // dilate the image
            Imgproc.erode(blobFrame, blobFrame, erosionKernel); // erode the image
        }
    }

    /**
     * This function finds the connected regions in a processed image
     * and stores them in the contours variable.
     * @param inputFrame
     */
    public void findBlobs(Mat inputFrame)
    {
        // Segment the Image
        Imgproc.findContours(inputFrame, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
    }

    public void processBlobs(Mat rgbaFrame)
    {
        Iterator<MatOfPoint> each = contours.iterator(); // this will iterate through each contour
        MatOfPoint biggestCont = null; // this will point to the largest contour
        double largestHeight = 0; // this is a counter for the largest height bounding rectangle
        Rect boundingrec = new Rect();

        // Loop through all the blobs/contours to find the largest height bounding box
        while(each.hasNext())
        {


            MatOfPoint wrapper = each.next(); // get the next blob

            boundingrec = Imgproc.boundingRect(wrapper); // Find the bounding rectangle of the blob

            // If this blob is the current largest then update the largestHeight and biggestCont
            if(boundingrec.height > largestHeight)
            {
                largestHeight = boundingrec.height;
                biggestCont = wrapper;
            }
        }

        // Now that we have found the largest height contour draw the fitline through it
        // First find the fitline
        Imgproc.fitLine(biggestCont, line, Imgproc.CV_DIST_L2, 0, 0.01, 0.01);
        // Now Draw the Line on the rgbaFrame and clip to boundingrec
        drawLine(rgbaFrame, boundingrec);
    }

    /**
     * This function draws the current found line on the toDrawOn Mat and clips
     * the line to the size of boundingRect.
     * @param toDrawOn
     * @param boundingRect
     */
    public void drawLine(Mat toDrawOn, Rect boundingRect)
    {
        // Extract the line
        double[] vx = line.get(0,0);
        double[] vy = line.get(1,0);
        double[] x = line.get(2,0);
        double[] y = line.get(3,0);

        Point pt1 = new Point(400*vx[0]+x[0],400*vy[0]+y[0]);
        Point pt2 = new Point(x[0]-400*vx[0],y[0]-400*vy[0]);

        // Clip the line to the bounding rectangle it is in
        Core.clipLine(boundingRect, pt1, pt2);
        // Draw the line
        Core.line(toDrawOn, pt1, pt2, new Scalar(255,0,0,255), 5);

        setInstructions(vx[0], vy[0]);
    }

    public void setInstructions(double vx, double vy)
    {
        if (vx >= -0.1 && vx <= 0.1) {
            instructionString = "Go Straight";
        }else if (vy/vx < -0.1){
            instructionString = "Go Left";
        }else if (vy/vx > 0.1){
            instructionString = "Go Right";
        }
    }

    public String getInstruction(){
        return instructionString;
    }

    public void findLines(Mat inputFrame, Mat returnFrame)
    {

    }

    public int getErosionSize() {
        return erosionKernel.rows();
    }

    public int getDilationSize() {
        return dilationKernel.rows();
    }

    public boolean isPerformOpenSwitch() {
        return performOpenSwitch;
    }

    public void setDilationSize(int dilationSize) {
        this.dilationKernel = Mat.ones(dilationSize,dilationSize,CvType.CV_8U);
    }

    public void setErosionSize(int erosionSize) {
        this.erosionKernel = Mat.ones(erosionSize,erosionSize,CvType.CV_8U);
    }

    public void setPerformOpenSwitch(boolean performOpenSwitch) {
        this.performOpenSwitch = performOpenSwitch;
    }
}
