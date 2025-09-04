import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class ClientGUI extends JFrame {
    private JPanel panel1;
    private JTextArea textArea1;
    private JTextField messagetosend;
    private JButton sendButton;
    private JButton stopButton;
    private JButton restartButton;
    private JButton connectButton;
    private JTextField portInput;
    private JLabel statusLabel;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String host;
    private int port;
    private boolean connected = false;

    public ClientGUI(String host) {
        this.host = host;
        setContentPane(panel1);
        setTitle("Client ChatBot");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);

        textArea1.setEditable(false);
        setStatus(false);

        connectButton.addActionListener(e -> connectToServer());
        sendButton.addActionListener(e -> sendMessage());
        stopButton.addActionListener(e -> disconnect());
        restartButton.addActionListener(e -> restartConnection());

        // Add Enter key listener for message input
        messagetosend.addActionListener(e -> sendMessage());
    }

    private void connectToServer() {
        try {
            port = Integer.parseInt(portInput.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port tidak valid", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        textArea1.append("Connecting to server...\n");

        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            setStatus(true);
            textArea1.append("Connected to server!\n");
            new Thread(this::listenFromServer).start();
        } catch (IOException e) {
            setStatus(false);
            JOptionPane.showMessageDialog(this, "Failed to connect: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void listenFromServer() {
        try {
            String response;
            while ((response = in.readLine()) != null && connected) {
                if(response.equals("__DISCONNECTED__")){
                    SwingUtilities.invokeLater(() -> textArea1.append("Disconnected!\n"));
                    disconnect();
                    break;
                }
                String finalResponse = response;
                SwingUtilities.invokeLater(() -> textArea1.append("Bot: " + finalResponse + "\n"));
                if (response.equalsIgnoreCase("Sampai jumpa!")) {
                    disconnect();
                    break;
                }
            }
        } catch (IOException e) {
            if (connected) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Connection lost: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE));
                setStatus(false);
            }
        }
    }

    private void sendMessage() {
        if (!connected) {
            JOptionPane.showMessageDialog(this, "Not connected to server!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String message = messagetosend.getText().trim();
        if (!message.isEmpty()) {
            textArea1.append("Anda: " + message + "\n");
            out.println(message);
            messagetosend.setText("");
        }
    }

    private void disconnect() {
        connected = false;
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
                textArea1.append("Disconnected from server.\n");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saat memutuskan koneksi: " + e.getMessage());
        }
        setStatus(false);
    }

    private void restartConnection() {
        disconnect();
        // Add small delay to ensure cleanup is complete
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        connectToServer();
    }

    private void setStatus(boolean isConnected) {
        this.connected = isConnected;
        statusLabel.setText(isConnected ? "Connected" : "Not Connected");
        statusLabel.setForeground(isConnected ? Color.GREEN : Color.RED);

        // Enable/disable buttons based on connection status
        connectButton.setEnabled(!isConnected);
        sendButton.setEnabled(isConnected);
        stopButton.setEnabled(isConnected);
        restartButton.setEnabled(true);
        messagetosend.setEnabled(isConnected);
    }
}