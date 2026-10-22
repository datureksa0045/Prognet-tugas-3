import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class DownloadUI extends JFrame {

    private JTextField txtUrl;
    private JTextField txtSavePath;
    private JButton btnBrowse;
    private JButton btnDownload;
    private JProgressBar progressBar;
    private JLabel lblStatus;
    private File selectedFile;

    public DownloadUI() {
        setTitle("Aplikasi Downloader Multi-Format (MP3, JPG, PDF, dll)");
        setSize(550, 280);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Label & Field URL
        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("URL File:"), gbc);

        txtUrl = new JTextField("", 30);
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(txtUrl, gbc);

        // Label, Field Path Simpan, & Tombol Browse
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 1;
        add(new JLabel("Lokasi Simpan:"), gbc);

        txtSavePath = new JTextField("", 20);
        gbc.gridx = 1; gbc.gridy = 1;
        add(txtSavePath, gbc);

        btnBrowse = new JButton("Pilih Folder...");
        gbc.gridx = 2; gbc.gridy = 1;
        add(btnBrowse, gbc);

        // Tombol Download
        btnDownload = new JButton("Download File");
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 3;
        add(btnDownload, gbc);

        // Progress Bar
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 3;
        add(progressBar, gbc);

        // Label Status
        lblStatus = new JLabel("Status: Siap", SwingConstants.CENTER);
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 3;
        add(lblStatus, gbc);

        // Event Handler Tombol Browse (Pilih Lokasi & Nama File)
        btnBrowse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Tentukan Lokasi & Nama File Simpan");

                // Ekstrak nama file default dari URL jika ada
                String urlText = txtUrl.getText().trim();
                if (!urlText.isEmpty()) {
                    String defaultName = urlText.substring(urlText.lastIndexOf('/') + 1);
                    if (!defaultName.isEmpty()) {
                        fileChooser.setSelectedFile(new File(defaultName));
                    }
                }

                int userSelection = fileChooser.showSaveDialog(DownloadUI.this);
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    selectedFile = fileChooser.getSelectedFile();
                    txtSavePath.setText(selectedFile.getAbsolutePath());
                }
            }
        });

        // Event Handler Tombol Download
        btnDownload.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fileUrl = txtUrl.getText().trim();
                String savePath = txtSavePath.getText().trim();

                if (fileUrl.isEmpty() || savePath.isEmpty()) {
                    JOptionPane.showMessageDialog(DownloadUI.this, 
                        "URL dan Lokasi Penyimpanan tidak boleh kosong!", 
                        "Peringatan", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                prosesDownloadThread(fileUrl, savePath);
            }
        });
    }

    private void prosesDownloadThread(String fileUrl, String savePath) {
        btnDownload.setEnabled(false);
        btnBrowse.setEnabled(false);
        lblStatus.setText("Status: Mengunduh...");
        progressBar.setValue(0);
        progressBar.setIndeterminate(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(fileUrl);
                    URLConnection connection = url.openConnection();
                    
                    // Mengatur User-Agent agar tidak diblokir oleh beberapa server media (seperti gambar/MP3)
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                    
                    int fileSize = connection.getContentLength(); // Mengambil ukuran total file

                    InputStream in = connection.getInputStream();
                    FileOutputStream out = new FileOutputStream(savePath);

                    byte[] buffer = new byte[4096]; // Buffer 4 KB untuk pengunduhan lebih cepat
                    int bytesRead;
                    int totalBytesRead = 0;

                    if (fileSize > 0) {
                        SwingUtilities.invokeLater(() -> progressBar.setIndeterminate(false));
                    }

                    // Pembacaan stream biner (support MP3, JPG, PNG, PDF, dll)
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;

                        if (fileSize > 0) {
                            int percent = (int) (((double) totalBytesRead / fileSize) * 100);
                            SwingUtilities.invokeLater(() -> progressBar.setValue(percent));
                        }
                    }

                    out.close();
                    in.close();

                    SwingUtilities.invokeLater(() -> {
                        lblStatus.setText("Status: Selesai diunduh!");
                        progressBar.setValue(100);
                        progressBar.setIndeterminate(false);
                        btnDownload.setEnabled(true);
                        btnBrowse.setEnabled(true);
                        JOptionPane.showMessageDialog(DownloadUI.this, 
                            "File berhasil disimpan ke:\n" + savePath, 
                            "Sukses", JOptionPane.INFORMATION_MESSAGE);
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        lblStatus.setText("Status: Gagal mengunduh.");
                        progressBar.setIndeterminate(false);
                        btnDownload.setEnabled(true);
                        btnBrowse.setEnabled(true);
                        JOptionPane.showMessageDialog(DownloadUI.this, 
                            "Gagal mendownload: " + ex.getMessage(), 
                            "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new DownloadUI().setVisible(true);
        });
    }
}