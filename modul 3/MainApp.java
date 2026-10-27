import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class MainApp extends JFrame {

    private JTextField dlUrl;
    private JComboBox<String> dlType;
    private JButton dlBtn;
    private JProgressBar dlProgress;
    private JLabel dlStatus;

    private JTextField rwUrl;
    private JButton rwBtn;
    private JTextArea rwOutput;
    private JLabel rwStatus;

    private static final String[] FILE_TYPES = {
        "Otomatis (dari URL)", "PDF (.pdf)", "MP3 (.mp3)",
        "MP4 (.mp4)", "JPG (.jpg)", "PNG (.png)",
        "ZIP (.zip)", "EXE (.exe)", "Lainnya..."
    };
    private static final String[] EXTENSIONS = {
        "", ".pdf", ".mp3", ".mp4", ".jpg", ".png", ".zip", ".exe", ""
    };

    public MainApp() {
        setTitle("Aplikasi Jaringan - Download & Read Website");
        setSize(680, 480);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("  Download File  ", buildDownloadPanel());
        tabs.addTab("  Read Website  ", buildReadPanel());
        add(tabs);
    }

    private JPanel buildDownloadPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(16, 16, 16, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        p.add(new JLabel("URL File:"), g);
        dlUrl = new JTextField(35);
        g.gridx = 1; g.weightx = 1;
        p.add(dlUrl, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        p.add(new JLabel("Jenis File:"), g);
        dlType = new JComboBox<>(FILE_TYPES);
        g.gridx = 1; g.weightx = 1;
        p.add(dlType, g);

        dlBtn = new JButton("Download");
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2; g.weightx = 1;
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

    private void startDownload() {
        String url = dlUrl.getText().trim();
        if (url.isEmpty()) { showWarn("URL tidak boleh kosong!"); return; }
        int idx = dlType.getSelectedIndex();
        String ext = EXTENSIONS[idx];
        if (idx == 0) {
            String path = url.replaceAll("[?#].*", "");
            ext = path.contains(".") ? path.substring(path.lastIndexOf('.')) : "";
        } else if (idx == FILE_TYPES.length - 1) {
            ext = JOptionPane.showInputDialog(this, "Masukkan ekstensi file:", ".txt");
            if (ext == null) return;
            if (!ext.startsWith(".")) ext = "." + ext;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Pilih lokasi simpan");
        fc.setSelectedFile(new File(guessFilename(url, ext)));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        String savePath = fc.getSelectedFile().getAbsolutePath();

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
                SwingUtilities.invokeLater(() -> {
                    dlProgress.setValue(100);
                    dlProgress.setIndeterminate(false);
                    dlStatus.setText("Status: Berhasil diunduh");
                    dlBtn.setEnabled(true);
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

    private JPanel buildReadPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel top = new JPanel(new BorderLayout(6, 0));
        rwUrl = new JTextField();
        rwBtn = new JButton("Baca Website");
        top.add(new JLabel("URL: "), BorderLayout.WEST);
        top.add(rwUrl, BorderLayout.CENTER);
        top.add(rwBtn, BorderLayout.EAST);

        rwOutput = new JTextArea();
        rwOutput.setEditable(false);
        rwOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        rwOutput.setLineWrap(true);
        rwOutput.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(rwOutput);
        scroll.setBorder(BorderFactory.createTitledBorder("Hasil"));

        rwStatus = new JLabel("Status: Siap");

        p.add(top, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        p.add(rwStatus, BorderLayout.SOUTH);

        rwBtn.addActionListener(e -> startRead());
        rwUrl.addActionListener(e -> startRead());
        return p;
    }

    private void startRead() {
        String url = rwUrl.getText().trim();
        if (url.isEmpty()) { showWarn("URL tidak boleh kosong!"); return; }
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://" + url;
        final String finalUrl = url;
        rwBtn.setEnabled(false);
        rwOutput.setText("");
        rwStatus.setText("Status: Membaca...");

        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            try {
                URL remote = new URL(finalUrl);
                URLConnection conn = remote.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(15_000);
                String enc = conn.getContentEncoding();
                if (enc == null) enc = StandardCharsets.UTF_8.name();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), enc))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line).append('\n');
                }
                String content = sb.toString();
                SwingUtilities.invokeLater(() -> {
                    rwOutput.setText(content);
                    rwOutput.setCaretPosition(0);
                    rwStatus.setText("Status: Berhasil dibaca (" + content.length() + " karakter)");
                    rwBtn.setEnabled(true);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    rwOutput.setText("Gagal membaca website.\n\nAlasan:\n" + friendlyError(ex));
                    rwStatus.setText("Status: Gagal");
                    rwBtn.setEnabled(true);
                });
            }
        }).start();
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
