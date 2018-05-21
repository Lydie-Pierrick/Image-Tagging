import java.util.*;
import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import java.lang.StringBuilder;
import java.io.BufferedWriter;
import java.io.*;

public class ImageTagging_ implements PlugInFilter
{
  protected ImagePlus image;

  public void run (ImageProcessor ip)
  {
    String pathOuput = "C:/Users/Bebo/Documents/Bureau";
    final Double levelBrightness = ((double)0xff)/2;

    // Convertir vers échelle de gris
    ImagePlus tempImg = new ImagePlus("ImageConvert", ip);
    new ImageConverter(tempImg).convertToGray8();

    ImageProcessor ipTemp = tempImg.getProcessor() ;

    Double averageCurrent = AverageNdg(ipTemp); // Get la moyenne de gris
    IJ.showMessage("Moyenne niveau de gris : " + averageCurrent);

    if(averageCurrent == -1.0)
        IJ.showMessage("Erreur : impossible d'avoir la moyenne de gris !");
    else
    {
      try {
        StringBuilder output = new StringBuilder();
        BufferedWriter printWriter = new BufferedWriter(new FileWriter(pathOuput + "/testPierrick.txt"));

        if(averageCurrent > levelBrightness)
            printWriter.write("clair");
        else
            printWriter.write("foncé");

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
}
