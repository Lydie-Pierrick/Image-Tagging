import java.util.*;
import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import java.lang.StringBuilder;
import java.io.BufferedWriter;
import java.io.*;
import ij.io.DirectoryChooser;

public class ImageTagging_ implements PlugInFilter
{
  protected ImagePlus image;

  public void run (ImageProcessor ip)
  {
    DirectoryChooser pickDir = new DirectoryChooser("Pick a directory");
	  String imagePath = pickDir.getDirectory();

    // Level of levelBrightness 255/2
    final Double levelBrightness = ((double)0xff)/2;

    // Convert current image to gray scale
    ImagePlus grayImage = new ImagePlus(null, ip);
    new ImageConverter(grayImage).convertToGray8();

    ImageProcessor grayImageProcessor = grayImage.getProcessor() ;

    // Get the average of gray scale
    Double averageCurrent = AverageNdg(grayImageProcessor);
    IJ.showMessage("Average of gray : " + averageCurrent);

    if(averageCurrent == -1.0)
        IJ.showMessage("Error : Impossible to get the average of gray");
    else
    {
      try {
        StringBuilder output = new StringBuilder();
        // Writes an output file
        BufferedWriter printWriter = new BufferedWriter(new FileWriter( imagePath + "/outputTagging.txt"));

        // If the result of the average is greater than 255, then the picture is brigth else dark
        if(averageCurrent > levelBrightness)
            printWriter.write("Brigth");
        else
            printWriter.write("Dark");

        printWriter.close();

        IJ.showMessage("ImageTagging sucessfully finished :)");
      }
      catch (FileNotFoundException e)
      {
          IJ.showMessage("Error ! File not found " + e.getMessage());
      }
      catch (IOException e) {
          IJ.showMessage("Error ! Input/Output " + e.getMessage());
      }
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
