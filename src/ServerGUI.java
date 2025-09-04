import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class ServerGUI extends JFrame {
    private JPanel panel1;
    private JButton startButton;
    private JButton stopButton;
    private JTextArea textArea1;
    private JLabel setstatus;
    private JButton saveLogButton;
    private JLabel Port;
    private JLabel NumPort;
    private JTextArea ListClient;
    private JTextArea log_command;
    private JTextField inputcommand;
    private JButton submit_command;

    private final int port;
    private ServerSocket ss;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final ExecutorService clientPool = Executors.newCachedThreadPool();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicInteger numClients = new AtomicInteger(0);

    private static final int MAX_CLIENTS = 100;
    private static final Pattern VALID_FILENAME = Pattern.compile("[a-zA-Z0-9_.-]*");
    private static final int MAX_LOG_SIZE = 50000;

    public ServerGUI(int port) {
        this.port = port;
        setTitle("Server");
        setContentPane(panel1);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);

        NumPort.setText(String.valueOf(port));
        setStatus(false);

        textArea1.setEditable(false);
        ListClient.setEditable(false);
        log_command.setEditable(false);

        Font monoFont = new Font("Monospaced", Font.PLAIN, 12);
        textArea1.setFont(monoFont);
        ListClient.setFont(monoFont);
        log_command.setFont(monoFont);

        startButton.addActionListener(e -> {
            if (!isRunning.get()) {
                new Thread(() -> startServer(port)).start();
            } else {
                log("Server already running!");
            }
        });

        stopButton.addActionListener(e -> stopServer());
        saveLogButton.addActionListener(e -> saveLog());

        submit_command.addActionListener(e -> {
            String cmd = inputcommand.getText().trim();
            if(!cmd.isEmpty()) {
                inputcommand.setText("");
                command(cmd);
            }
        });

        inputcommand.addActionListener(e -> {
            String cmd = inputcommand.getText().trim();
            if(!cmd.isEmpty()) {
                inputcommand.setText("");
                command(cmd);
            }
        });
    }

    private void startServer(int port) {
        if(!isValid(port)) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Port tidak valid" +
                        "\nPlease enter a valid port!(1024 - 65535)", "Port Error", JOptionPane.ERROR_MESSAGE);
            });
            return;
        }
        try {
            ss = new ServerSocket(port);
            log("Server Started on port : " + port);
            setStatus(true);
            isRunning.set(true);

            while (!ss.isClosed() && isRunning.get()) {
                try {
                    Socket client = ss.accept();

                    if(clients.size() >= MAX_CLIENTS) {
                        log("Server reached maximum clients limit. " +
                                "Rejecting connection from: " + client.getInetAddress().getHostAddress());
                        try (PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
                            out.println("Server full. Please try again later.");
                        }
                        client.close();
                        continue;
                    }

                    log("Client connected : " + client.getRemoteSocketAddress());
                    ClientHandler handler = new ClientHandler(client, this);
                    clients.add(handler);
                    numClients.incrementAndGet();
                    updateClientlist();
                    clientPool.submit(handler);
                } catch (IOException e) {
                    if (isRunning.get() && !ss.isClosed()) {
                        log("Connection Error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log("Server Error : " + e.getMessage());
        } finally {
            isRunning.set(false);
            setStatus(false);
        }
    }

    private void stopServer() {
        isRunning.set(false);
        try {
            // Send disconnect signal to all clients
            for (ClientHandler ch : new ArrayList<>(clients)) {
                ch.closeConnection();
            }

            if (ss != null && !ss.isClosed()) {
                ss.close();
            }

            // Give threads time to finish
            clientPool.shutdown();
            if (!clientPool.isTerminated()) {
                clientPool.shutdownNow();
            }

            clients.clear();
            numClients.set(0);
            updateClientlist();
            log("Server Stopped");

        } catch (IOException e) {
            log("Error stopping server: " + e.getMessage());
        } finally {
            setStatus(false);
        }
    }

    public synchronized void log(String msg) {
        String timestamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
        String fullMsg = "[" + timestamp + "] " + msg;
        SwingUtilities.invokeLater(() -> {
            textArea1.append(fullMsg + "\n");

            // Prevent log from growing too large
            if (textArea1.getText().length() > MAX_LOG_SIZE) {
                String text = textArea1.getText();
                int cutPoint = text.indexOf('\n', text.length() - MAX_LOG_SIZE + 1000);
                if (cutPoint > 0) {
                    textArea1.setText(text.substring(cutPoint + 1));
                }
            }

            // Auto-scroll to bottom
            textArea1.setCaretPosition(textArea1.getDocument().getLength());
        });
    }

    private void setStatus(boolean status) {
        SwingUtilities.invokeLater(() -> {
            setstatus.setText(status ? "Active" : "Inactive");
            setstatus.setForeground(status ? Color.GREEN : Color.RED);

            // Enable/disable buttons based on server status
            startButton.setEnabled(!status);
            stopButton.setEnabled(status);
        });
    }

    private void saveLog() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Log");
        chooser.setSelectedFile(new File("server_log_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt"));

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(textArea1.getText());
                log("Log saved to: " + file.getAbsolutePath());
            } catch (IOException e) {
                log("Save Error: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Failed to save log: " + e.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void removeClient(ClientHandler handler) {
        if (clients.remove(handler)) {
            numClients.decrementAndGet();
            updateClientlist();
        }
    }

    private void updateClientlist() {
        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Jumlah Client: ").append(clients.size()).append("\n\n");

            int index = 1;
            for (ClientHandler ch : clients) {
                if (ch.isConnected()) {
                    sb.append(index).append(". ").append(ch.getClientInfo()).append("\n");
                    index++;
                }
            }

            ListClient.setText(sb.toString());
            ListClient.setCaretPosition(0);
        });
    }

    private void command(String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }

        log_command.append("> " + command + "\n");
        log_command.setCaretPosition(log_command.getDocument().getLength());

        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "@list" -> {
                updateClientlist();
                log("Client list updated");
            }
            case "@broadcast" -> {
                if (parts.length < 2) {
                    log("Usage: @broadcast <message>");
                    return;
                }
                String msg = parts[1].trim();
                if (msg.isEmpty()) {
                    log("Broadcast message cannot be empty");
                    return;
                }
                int sent = 0;
                for(ClientHandler ch : new ArrayList<>(clients)){
                    if (ch.sendMessage("[SERVER BROADCAST] " + msg)) {
                        sent++;
                    }
                }
                log("Broadcast sent to " + sent + " clients: " + msg);
            }
            case "@kick" -> {
                if (parts.length < 2) {
                    log("Usage: @kick <client_id or ip>");
                    return;
                }
                String target = parts[1].trim();
                boolean found = false;

                for(ClientHandler ch : new ArrayList<>(clients)){
                    String clientInfo = ch.getClientInfo();
                    if(clientInfo.contains(target) ||
                            String.valueOf(ch.getClientID()).equals(target)){
                        ch.sendMessage("[SERVER] You have been disconnected by administrator");
                        ch.closeConnection();
                        log("Kicked client: " + clientInfo);
                        found = true;
                        break;
                    }
                }
                if(!found){
                    log("Client not found: " + target);
                }
            }
            case "@shutdown" -> {
                log("Shutdown command received");
                stopServer();
            }
            case "@save" -> {
                if (parts.length < 2) {
                    log("Usage: @save <filename>");
                    return;
                }
                String filename = parts[1].trim();
                if (!VALID_FILENAME.matcher(filename).matches()) {
                    log("Invalid filename. Use only letters, numbers, dots, hyphens and underscores.");
                    return;
                }
                try(BufferedWriter writer = new BufferedWriter(new FileWriter(filename))){
                    writer.write(textArea1.getText());
                    log("Log saved to: " + new File(filename).getAbsolutePath());
                } catch (IOException e) {
                    log("Save Error: " + e.getMessage());
                }
            }
            case "@clear" -> {
                textArea1.setText("");
                log("Log cleared");
            }
            case "@status" -> {
                log("Server Status: " + (isRunning.get() ? "Running" : "Stopped"));
                log("Port: " + port);
                log("Connected clients: " + clients.size());
                log("Max clients: " + MAX_CLIENTS);
            }
            case "@help" -> {
                String help = """
                    Available commands:
                    @list - Show connected clients
                    @broadcast <msg> - Send message to all clients
                    @kick <client_id|ip> - Disconnect a client
                    @shutdown - Stop the server
                    @save <filename> - Save log to file
                    @clear - Clear log display
                    @status - Show server status
                    @help - Show this help""";
                log(help);
                log_command.append(help + "\n");
            }
            default -> {
                log("Unknown command: " + command + " (type @help for available commands)");
            }
        }
    }

    private boolean isValid(int port){
        return port >= 1024 && port <= 65535;
    }
}