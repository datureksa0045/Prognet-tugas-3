import java.io.*;
import java.net.*;

public class ReadWebsite {
    public static void main(String[] args) {
        String nextLine;
        URL url = null;
        URLConnection urlConn = null;
        InputStreamReader inStream = null;
        BufferedReader buff = null;

        try {
            // Inisialisasi URL & Koneksi
            url = new URL("https://www.google.com");
            urlConn = url.openConnection();
            inStream = new InputStreamReader(urlConn.getInputStream(), "UTF-8");
            buff = new BufferedReader(inStream);

            // Membaca dan mencetak tiap baris dari index.html
            while ((nextLine = buff.readLine()) != null) {
                System.out.println(nextLine);
            }

        } catch (MalformedURLException e) {
            System.out.println("Cek kembali URL: " + e.toString());
        } catch (IOException e1) {
            System.out.println("Tidak dapat membaca dari Internet: " + e1.toString());
        } finally {
            if (buff != null) {
                try {
                    buff.close();
                    if (inStream != null) inStream.close();
                } catch (IOException e1) {
                    System.out.println("Tidak dapat menutup stream: " + e1.getMessage());
                }
            }
        }
    }
}