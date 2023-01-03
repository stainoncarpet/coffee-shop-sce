package Utilities;

import spark.utils.IOUtils;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * File class is responsible for file uploads
 */
public class File {
    /**
     * This method uploads an image file
     * @param req instance http request object
     * @param outputPath the path to the uploaded file
     * @return void
     */
    public static void uploadFile(spark.Request req, String outputPath) {
        try {
            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("C:/tmp"));
            Part filePart = req.raw().getPart("image-file");
            InputStream inputStream = filePart.getInputStream();
            var ext = "." + filePart.getSubmittedFileName().split("\\.")[1];
            OutputStream outputStream = new FileOutputStream(outputPath + ext);
            IOUtils.copy(inputStream, outputStream);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method remove an image file
     * @param path for which an image is removed
     * @return void
     */
    public static void removeFile(String path) {
        java.io.File f= new java.io.File(path);
        if(f.delete()) {
            System.out.println(f.getName() + " deleted");
        } else {
            System.out.println(f.getName() + " wasn't deleted");
        }
    }
}
