import java.util.*;
import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import java.lang.StringBuilder;
import java.io.BufferedWriter;
import java.io.*;
import java.lang.Math;

public class ImageTagging_ implements PlugInFilter
{
  protected ImagePlus image;

  public void run (ImageProcessor ip)
  {
    Double averageCurrent = AverageNdg(ip); // Get la moyenne de gris
    IJ.showMessage("Moyenne niveau de gris : " + averageCurrent);

    if(averageCurrent == -1.0)
        IJ.showMessage("Erreur : impossible d'avoir la moyenne de gris !");
    else
    {
      try {
        StringBuilder output = new StringBuilder();
        BufferedWriter printWriter = new BufferedWriter(new FileWriter(pathOuput + "/testPierrick.txt"));

        if(averageCurrent > levelBrightness)
            printWriter.write("Bright");
        else
            printWriter.write("Dark");

        printWriter.close();

        IJ.showMessage("Lecture de l'image termine ! :)");

      }
      catch (FileNotFoundException e)
      {
          IJ.showMessage("Erreur ! Fichier non trouve " + e.getMessage());
      }
      catch (IOException e) {
          IJ.showMessage("Erreur ! Entree/Sortie " + e.getMessage());
      }
    }

    Map<String, Integer> map = rgbToHsv(ip);
    // IJ.showMessage("Results： \n" + "Orange " + map.get("Orange") + "\nYellow " + map.get("Yellow") + "\nGreen " + map.get("Green") +
    // "\nBlue " + map.get("Blue") + "\nPurple " + map.get("Purple") + "\nRed " + map.get("Red") + "\nGrey " + map.get("Grey") + "\nBlack "
    // + map.get("Black") + "\nWhite " + map.get("White"));

    List<String> mainColors = getMainColors(map);
    String colors = " ";
    for(String color : mainColors){
      colors += color + "\n";
    }
    IJ.showMessage("Main colors : " + colors);
  }

  // Retourne la moyenne des NdG d’une image en NdG
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
    }else{
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
    return DOES_8G | DOES_16 | DOES_32 | DOES_RGB;
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
        map.put("Orange", counterColors[0]);
        map.put("Yellow", counterColors[1]);
        map.put("Green", counterColors[2]);
        map.put("Blue", counterColors[3]);
        map.put("Purple", counterColors[4]);
        map.put("Red", counterColors[5]);
        map.put("Black", counterColors[6]);
        map.put("Grey", counterColors[7]);
        map.put("White", counterColors[8]);
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
      if(percentage >= 0.1){ // If a color account for 50%
        mainColors.add(key); // Add this color into list
      }
    }
    return mainColors;
  }
}
