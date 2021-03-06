import java.util.*;
import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import java.lang.StringBuilder;
import java.io.BufferedWriter;
import java.io.*;
import java.lang.Math;
import ij.io.DirectoryChooser;

public class ImageTagging_ implements PlugInFilter
{
  protected ImagePlus image;

  public void run (ImageProcessor ip)
  {
  	  String imagePath = image.getOriginalFileInfo().directory;

      // Level of levelBrightness 128
      final Double levelBrightness = ((double)0xff)/2;

      // Convert current image to gray scale
      ImagePlus grayImage = new ImagePlus("grey", ip);

      new ImageConverter(grayImage).convertToGray8();

      ImageProcessor grayImageProcessor = grayImage.getProcessor() ;

      // Get main colors
      Map<String, Integer> map = rgbToHsv(ip);

      List<String> mainColors = getMainColors(map);
      String colors = " ";
      for(int i = 0; i < mainColors.size() - 1; i++){
        colors += mainColors.get(i) + ", ";
      }
      colors += mainColors.get(mainColors.size() - 1);
      IJ.showMessage("Main colors : " + colors);

      // Get the average of gray scale
      Double averageCurrent = AverageNdg(grayImageProcessor);

      try {
        StringBuilder output = new StringBuilder();
        // Writes an output file
        BufferedWriter printWriter = new BufferedWriter(new FileWriter( imagePath + image.getShortTitle()+".txt"));

        // If the result of the average is greater than 255, then the picture is light else dark
        if(averageCurrent > levelBrightness)
            printWriter.write("light,");
        else
            printWriter.write("dark,");

        printWriter.write(colors);
        printWriter.close();


        IJ.showMessage("ImageTagging finished sucessfully :) And a .txt file was saved in the directory.");
      }
      catch (FileNotFoundException e)
      {
          IJ.showMessage("Error ! File not found " + e.getMessage());
      }
      catch (IOException e) {
          IJ.showMessage("Error ! Input/Output " + e.getMessage());
      }
  }

  // Average of gray scale
  public double AverageNdg(ImageProcessor ip)
  {
      byte [] pixels = ( byte []) ip.getPixels ();
      int width = ip.getWidth() ;
      int height = ip.getHeight() ;
      int ndg;
      int size = width * height;
      int sum = 0;

      for (int y=0; y< height ; y++){
        for (int x=0; x< width ; x++) {
          ndg = pixels [ y*width + x ] & 0xff ;
          sum += ndg;
        }
      }

      if(size != 0){
        double average = sum / size;
        return average;
      } else{
        return -1.0;
      }
  }

  public int setup(String arg, ImagePlus imp)
  {
      if (arg.equals("about")) {
          IJ.showMessage("Traitement de l'image");
          return DONE;
      }

      image = imp;
      return DOES_ALL;
  }

  public Map<String, Integer> rgbToHsv(ImageProcessor ip)
  {
      int width = ip.getWidth() ;
      int height = ip.getHeight() ;
      int [] rgb = new int[3];
      double R, G, B;
      double H = 0.0, S, V;
      double tmp, delta;
      double min = 0.0, max = 0.0;
      int [] counterColors = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
      boolean isGoodV = false;
      boolean isGoodS = false;
      Map<String, Integer> map = new HashMap<String, Integer>();

      for (int y=0; y< height ; y++){
        for (int x=0; x< width ; x++) {
          ip.getPixel(x, y, rgb);
          R = rgb[0]/255.0;
          G = rgb[1]/255.0;
          B = rgb[2]/255.0;

          // Get min [0, 1]
          tmp = Math.min(R, G);
          min = Math.min(tmp, B);
          // Get max [0, 1]
          tmp = Math.max(R, G);
          max = Math.max(tmp, B);

          // Get V [0, 1]
          V = max;
          // Get delta
          delta = max - min;

          if(max != 0.0){
            S = delta / max;
          }else{ // R = G = B = 0
            S = 0.0;
            H = -1.0; // Undefined H
          }

          // Get H [0, 360]
          if(R == max){
            H = (G - B) / delta;
          }else if(G == max){
            H = 2 + (B - R) / delta;
          }else{
            H = 4 + (R - G) / delta;
          }

          H *= 60;
          if(H < 0){
            H += 360;
          }

          V *= 255; // V [0 ,255]
          S *= 255; // S [0, 255]
          H /= 2; // H [0, 180]

          if(V >= 46 && V <= 255) {
            isGoodV = true;
          }

          if(S >= 43 && S <= 255) {
            isGoodS = true;
          }

          // Get color range
          if(isGoodV && isGoodS){
            if(H >= 11 && H <= 25){ // Orange
              counterColors[0]++;
            }
            else if(H >= 26 && H <= 34){ // Yellow
              counterColors[1]++;
            }
            else if(H >= 35 && H <= 77){ // Green
              counterColors[2]++;
            }
            else if(H >= 78 && H <= 124){ // Blue
              counterColors[3]++;
            }
            else if(H >= 125 && H <= 155){ // Purple
              counterColors[4]++;
            }
            else if((H >= 156 && H <= 179) || (H >= 0 && H <= 10)){ // Red
              counterColors[5]++;
            }
          }

          if(S >= 0 && S <= 255 && V >= 0 && V <= 45){ // Black
            counterColors[6]++;
          }else if(S >= 0 && S <= 43 && V >= 46 && V <= 220){ // Grey
            counterColors[7]++;
          }else if(S >= 0 && S <= 30 && V >= 221 && V <= 255){ // White
            counterColors[8]++;
          }

          // Put colors into map
          map.put("orange", counterColors[0]);
          map.put("yellow", counterColors[1]);
          map.put("green", counterColors[2]);
          map.put("blue", counterColors[3]);
          map.put("purple", counterColors[4]);
          map.put("red", counterColors[5]);
          map.put("black", counterColors[6]);
          map.put("grey", counterColors[7]);
          map.put("white", counterColors[8]);
        }
      }
      return map;
  }

  public List<String> getMainColors(Map<String, Integer> map){
      int counterSum = 0;
      List<String> mainColors = new ArrayList<String>();
      double percentage;
      // All the values
      for (int value : map.values()) {
        counterSum += value; // Get the amount of counters
      }
      // All the keys
      for (String key : map.keySet()) {
        percentage = (double)map.get(key) / (double)counterSum;
        if(percentage >= 0.1){ // If a color account for 10%
          mainColors.add(key); // Add this color into list
        }
      }
      return mainColors;
    }
}
