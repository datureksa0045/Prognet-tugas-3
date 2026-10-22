import java.io.*;
import java.net.*;

public class DownloadFile {
    public static void main(String[] args) {
        InputStream in = null;
        FileOutputStream fout = null;

        try {
            // URL file yang akan diunduh
            String fileUrl = "http://dl.vafamusic.com/Full%20Album/bts/320/BTS-Boy-With-Luv%20%28VafaMusic%29.mp3";
            URL remoteFile = new URL(fileUrl);
            URLConnection fileStream = remoteFile.openConnection();

            // Membuka input dan output stream
            fout = new FileOutputStream("BTS1.mp3");
            in = fileStream.getInputStream();

            // Menyimpan file byte demi byte
            int data;
            while ((data = in.read()) != -1) {
                fout.write(data);
            }

            System.out.println("File telah berhasil didownload");

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
                if (fout != null) {
                    fout.flush();
                    fout.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}