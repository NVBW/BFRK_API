package de.nvbw.bfrk.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;

public class ImageCoding {
	// Quelle: https://grokonez.com/java/java-advanced/java-8-encode-decode-an-image-base64
  public static void main(String[] args) {
    String imagePath = "C:\\Users\\sei\\temp\\test.base64jpg";
    String destinationfile = "C:\\Users\\sei\\temp\\test.jpg";
    convertHtmlembeddedjpgTojpg(imagePath, destinationfile);
    System.out.println("DONE!");
 
  }

  public static void test() {
	    String imagePath = "C:\\Users\\sei\\temp\\e7e144c2-4438-444a-ab92-d7df0a445d00.jpg_base64";
	    System.out.println("=================Encoder Image to Base 64!=================");
	    //String base64ImageString = encoder(imagePath);
	    String base64ImageString = getContent(imagePath);
	    System.out.println("Base64ImageString = " + base64ImageString);
	 
	    System.out.println("=================Decoder Base64ImageString to Image!=================");
	    decoder(base64ImageString, "C:\\Users\\sei\\temp\\e7e144c2-4438-444a-ab92-d7df0a445d00_bild.jpg");
	 
	    System.out.println("DONE!");
  }

  /**
   * Konvertierung klappt schon mal (Quelle: per wget heruntergeladenes Image, z.B. wget -O test.base64jpg "http://eyevis.kobra-nvs.de/Image/GetImageAsBase64Html?imageName=dddde15c-b58c-4f3e-8771-27b7048e9eb9.jpg&projectId=267"
   * Nach dieser Methode gibt es eine test.jpg
   * 
   * @param htmlfilename
   * @param destinationfilename
   */
  public static void convertHtmlembeddedjpgTojpg(String htmlfilename, String destinationfilename) {
	    String base64ImageString = getContent(htmlfilename);
	    String suchstring = "data:image/jpg;base64";
	    int findpos = base64ImageString.indexOf(suchstring);
	    if( findpos != -1) {
	    	base64ImageString = base64ImageString.substring(findpos + suchstring.length() + 1);
	    	//System.out.println("netto file start ===" + base64ImageString + "===");
	    	suchstring = "\"/>";
		    findpos = base64ImageString.indexOf(suchstring);
		    if( findpos != -1) {
		    	base64ImageString = base64ImageString.substring(0, findpos);
		    	//System.out.println("netto file kpl ===" + base64ImageString + "===");
		    }
	    }
	    decoder(base64ImageString, destinationfilename);
  }
  
  public static String getContent(String imagePath) {
	    String base64Image = "";
	    File file = new File(imagePath);
	    byte imageData[] = null;
	    try (FileInputStream imageInFile = new FileInputStream(file)) {
	      // Reading a Image file from file system
	      imageData = new byte[(int) file.length()];
	      imageInFile.read(imageData);
	      //System.out.println("String inhalt ==" + new String(imageData) + "===");
	    } catch (FileNotFoundException e) {
	        System.out.println("Image not found" + e);
	    } catch (IOException ioe) {
	        System.out.println("Exception while reading the Image " + ioe);
	    }
	    return new String(imageData);
  }

  
  public static void writeContent(String base64Image, String pathFile) {
    try (FileOutputStream imageOutFile = new FileOutputStream(pathFile)) {
      // Converting a Base64 String into Image byte array
      byte[] imageByteArray = base64Image.getBytes();
      imageOutFile.write(imageByteArray);
    } catch (FileNotFoundException e) {
      System.out.println("Image not found" + e);
    } catch (IOException ioe) {
      System.out.println("Exception while reading the Image " + ioe);
    }
  }

  public static String encoder(String imagePath) {
    String base64Image = "";
    File file = new File(imagePath);
    try (FileInputStream imageInFile = new FileInputStream(file)) {
      // Reading a Image file from file system
      byte imageData[] = new byte[(int) file.length()];
      imageInFile.read(imageData);
      base64Image = Base64.getEncoder().encodeToString(imageData);
    } catch (FileNotFoundException e) {
      System.out.println("Image not found" + e);
    } catch (IOException ioe) {
      System.out.println("Exception while reading the Image " + ioe);
    }
    return base64Image;
  }
 
  public static void decoder(String base64Image, String pathFile) {
    try (FileOutputStream imageOutFile = new FileOutputStream(pathFile)) {
      // Converting a Base64 String into Image byte array
      byte[] imageByteArray = Base64.getDecoder().decode(base64Image);
      imageOutFile.write(imageByteArray);
    } catch (FileNotFoundException e) {
      System.out.println("Image not found" + e);
    } catch (IOException ioe) {
      System.out.println("Exception while reading the Image " + ioe);
    }
  }
}