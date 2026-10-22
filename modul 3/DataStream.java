import java.io.*;

public class DataStream {
    public static void main(String[] args) {
        File file = new File("contohdatastream.txt");

        // 1. Cek atau buat file baru
        if (file.exists()) {
            System.out.println("File sudah ada: " + file.getName());
        } else {
            try {
                if (file.createNewFile()) {
                    System.out.println("File berhasil dibuat");
                    System.out.println("Lokasi file: " + file.getPath());
                } else {
                    System.out.println("Tidak bisa membuat file");
                }
            } catch (Exception e) {
                System.out.println("Error membuat file: " + e.toString());
            }
        }

        // 2. Menulis dan membaca data primitif
        try (
            DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
            DataInputStream in = new DataInputStream(new FileInputStream(file))
        ) {
            // Menulis data primitif
            out.writeInt(70);
            out.writeDouble(456.67);
            out.writeFloat(123.45F);

            // Membaca data primitif (harus sesuai urutan penulisan)
            int var1 = in.readInt();
            double var2 = in.readDouble();
            float var3 = in.readFloat();

            System.out.println("Nilai integer : " + var1);
            System.out.println("Nilai double  : " + var2);
            System.out.println("Nilai float   : " + var3);

        } catch (Exception e) {
            System.out.println("Error I/O: " + e.toString());
        }
    }
}