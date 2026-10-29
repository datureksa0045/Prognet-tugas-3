import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainApp extends JFrame {

    private JTextField dlUrl;
    private JComboBox<String> dlType;
    private JButton dlBtn;
    private JProgressBar dlProgress;
    private JLabel dlStatus;
    private DefaultTableModel historyModel;
    private JTable historyTable;

    private static final String[] FILE_TYPES = {
        "Otomatis (dari URL)", "PDF (.pdf)", "MP3 (.mp3)",
        "MP4 (.mp4)", "JPG (.jpg)", "PNG (.png)",
        "ZIP (.zip)", "EXE (.exe)", "Lainnya..."
    };
    private static final String[] EXTENSIONS = {
        "", ".pdf", ".mp3", ".mp4", ".jpg", ".png", ".zip", ".exe", ""
    };

    public MainApp() {
        setTitle("Aplikasi Download File");
        setSize(750, 520);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildDownloadPanel(), BorderLayout.NORTH);
        add(buildHistoryPanel(), BorderLayout.CENTER);
    }

    private JPanel buildDownloadPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Download File"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        p.add(new JLabel("URL File:"), g);
        dlUrl = new JTextField(35);
        g.gridx = 1; g.weightx = 1; g.gridwidth = 2;
        p.add(dlUrl, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0; g.gridwidth = 1;
        p.add(new JLabel("Jenis File:"), g);
        dlType = new JComboBox<>(FILE_TYPES);
        g.gridx = 1; g.weightx = 1; g.gridwidth = 2;
        p.add(dlType, g);

        dlBtn = new JButton("Download");
        g.gridx = 0; g.gridy = 2; g.gridwidth = 3;
        p.add(dlBtn, g);

        dlProgress = new JProgressBar(0, 100);
        dlProgress.setStringPainted(true);
        g.gridy = 3;
        p.add(dlProgress, g);

        dlStatus = new JLabel("Status: Siap", SwingConstants.CENTER);
        g.gridy = 4;
        p.add(dlStatus, g);

        dlBtn.addActionListener(e -> startDownload());
        return p;
    }

    private JPanel buildHistoryPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Riwayat Download"));

        String[] cols = {"No", "Nama File", "Jenis", "Ukuran", "Lokasi", "Waktu"};
        historyModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        historyTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        historyTable.getColumnModel().getColumn(0).setMaxWidth(40);
        historyTable.getColumnModel().getColumn(2).setMaxWidth(80);
        historyTable.getColumnModel().getColumn(3).setMaxWidth(100);

        JScrollPane scroll = new JScrollPane(historyTable);
        p.add(scroll, BorderLayout.CENTER);

        JButton btnClear = new JButton("Hapus Riwayat");
        btnClear.addActionListener(e -> historyModel.setRowCount(0));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnClear);
        p.add(bottom, BorderLayout.SOUTH);

        return p;
    }

    private void startDownload() {
        String url = dlUrl.getText().trim();
        if (url.isEmpty()) { showWarn("URL tidak boleh kosong!"); return; }

        int idx = dlType.getSelectedIndex();
        String ext = EXTENSIONS[idx];
        String jenisFile = FILE_TYPES[idx];

        if (idx == 0) {
            String path = url.replaceAll("[?#].*", "");
            int dot = path.lastIndexOf('.');
            int slash = path.lastIndexOf('/');
            ext = (dot > slash && dot < path.length() - 1) ? path.substring(dot) : "";
            jenisFile = ext.isEmpty() ? "Unknown" : ext.substring(1).toUpperCase();
        } else if (idx == FILE_TYPES.length - 1) {
            ext = JOptionPane.showInputDialog(this, "Masukkan ekstensi file:", ".txt");
            if (ext == null) return;
            if (!ext.startsWith(".")) ext = "." + ext;
            jenisFile = ext.substring(1).toUpperCase();
        } else {
            jenisFile = ext.substring(1).toUpperCase();
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Pilih lokasi simpan");
        fc.setSelectedFile(new File(guessFilename(url, ext)));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        String savePath = fc.getSelectedFile().getAbsolutePath();
        String fileName = fc.getSelectedFile().getName();
        final String fJenis = jenisFile;

        dlBtn.setEnabled(false);
        dlProgress.setValue(0);
        dlProgress.setIndeterminate(true);
        dlStatus.setText("Status: Mengunduh...");

        new Thread(() -> {
            try {
                URL remote = new URL(url);
                URLConnection conn = remote.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(30_000);
                int total = conn.getContentLength();
                if (total > 0) SwingUtilities.invokeLater(() -> dlProgress.setIndeterminate(false));

                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(savePath)) {
                    byte[] buf = new byte[8192];
                    int read, done = 0;
                    while ((read = in.read(buf)) != -1) {
                        out.write(buf, 0, read);
                        done += read;
                        if (total > 0) {
                            int pct = (int)(done * 100.0 / total);
                            SwingUtilities.invokeLater(() -> dlProgress.setValue(pct));
                        }
                    }
                }

                File downloaded = new File(savePath);
                String size = formatSize(downloaded.length());
                String waktu = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());

                SwingUtilities.invokeLater(() -> {
                    dlProgress.setValue(100);
                    dlProgress.setIndeterminate(false);
                    dlStatus.setText("Status: Berhasil diunduh");
                    dlBtn.setEnabled(true);
                    int no = historyModel.getRowCount() + 1;
                    historyModel.addRow(new Object[]{no, fileName, fJenis, size, savePath, waktu});
                    JOptionPane.showMessageDialog(this, "File berhasil disimpan ke:\n" + savePath, "Sukses", JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    dlProgress.setIndeterminate(false);
                    dlProgress.setValue(0);
                    dlStatus.setText("Status: Gagal");
                    dlBtn.setEnabled(true);
                    JOptionPane.showMessageDialog(this, "Download gagal!\n\nAlasan: " + friendlyError(ex), "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String friendlyError(Exception ex) {
        if (ex instanceof MalformedURLException) return "URL tidak valid - " + ex.getMessage();
        if (ex instanceof UnknownHostException) return "Host tidak ditemukan - cek koneksi atau domain: " + ex.getMessage();
        if (ex instanceof SocketTimeoutException) return "Koneksi timeout - server tidak merespons.";
        if (ex instanceof ConnectException) return "Koneksi ditolak server - " + ex.getMessage();
        if (ex instanceof FileNotFoundException) return "File tidak ditemukan (404): " + ex.getMessage();
        if (ex instanceof IOException) return "I/O Error - " + ex.getMessage();
        return ex.getClass().getSimpleName() + ": " + ex.getMessage();
    }

    private String guessFilename(String url, String ext) {
        try {
            String path = new URL(url).getPath();
            String name = path.substring(path.lastIndexOf('/') + 1);
            if (name.isEmpty()) name = "download";
            if (!ext.isEmpty() && !name.endsWith(ext)) name += ext;
            return name;
        } catch (Exception e) { return "download" + ext; }
    }

    private void showWarn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Peringatan", JOptionPane.WARNING_MESSAGE);
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new MainApp().setVisible(true));
    }
}
