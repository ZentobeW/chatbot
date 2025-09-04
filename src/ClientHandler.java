import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler extends Thread {
    private static final AtomicInteger counter = new AtomicInteger(0);

    private final Socket socket;
    private final ServerGUI server;
    private final int clientID;
    private final AtomicBoolean isConnected = new AtomicBoolean(true);
    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(Socket socket, ServerGUI server) {
        this.socket = socket;
        this.server = server;
        this.clientID = counter.incrementAndGet();
        this.setName("ClientHandler-" + clientID);

        try{
            socket.setSoTimeout(30000);
        } catch(SocketException e){
            server.log("Warning: Socket timed out for client " + clientID);
        }
    }

    public void closeConnection() {
        isConnected.set(false);

        try {
            if(out != null && !socket.isClosed()) {
                out.println("__DISCONNECTED__");
                out.flush();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (!socket.isClosed()) {
                socket.close();
            }
            server.log("Client " + clientID + " disconnected");

        } catch (IOException e) {
            server.log("Close Error: " + e.getMessage());
        } finally {
            server.removeClient(this);
        }
    }

    public String getClientInfo(){
        if(socket != null && !socket.isClosed()){
            return "ID: " + clientID + "-" + socket.getRemoteSocketAddress();
        }
        return "ID: " + clientID + "-[Disconnected]";
    }

    public int getClientID() {
        return clientID;
    }

    public boolean sendMessage(String message) {
        if(!isConnected.get() || out == null || socket.isClosed())
            return false;

        try{
            out.println(message);
            out.flush();

            if(out.checkError()){
                server.log("Error sending message to client : " + clientID + "- connection maybe broken");
                closeConnection();
                return false;
            }
            return true;
        } catch (Exception e){
            server.log("Failed to send message to client : " + clientID + ": " + e.getMessage());
            closeConnection();
            return false;
        }

    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            server.log("Client " + clientID + " connected from : " + socket.getRemoteSocketAddress());
            sendMessage("Selamat datang di Chatbot Server! Anda adalah client #" + clientID);
            sendMessage("Ketik 'help' untuk melihat perintah yang tersedia atau 'selamat tinggal' untuk keluar.");

            String line;
            while (isConnected.get() && (line = in.readLine()) != null) {
                line = line.trim();
                if(line.isEmpty()){
                    continue;
                }
                server.log("Client " + clientID + " says: " + line);

                String response = generateResponse(line);

                // Add artificial delay
                if(!line.equalsIgnoreCase("selamat tinggal")){
                    try{
                        Thread.sleep(500 + (int) (Math.random() * 500));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                if(!sendMessage(response)){
                    break;
                }

                if (line.equalsIgnoreCase("selamat tinggal")) {
                    break;
                }
            }

        } catch (IOException e) {
            if(isConnected.get()){
                server.log("Client " + clientID + " connection error: " + e.getMessage());
            }
        } catch(Exception e){
            server.log("Unexpected error with client " + clientID + ": " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private String generateResponse(String input) {
        if (input == null) return "Pesan tidak valid.";

        String lowerInput = input.toLowerCase().trim();

        return switch (lowerInput) {
            case "halo", "hai", "hello" -> "Halo juga! Senang bertemu dengan Anda.";
            case "apa kabar", "apa kabar?" -> "Kabar baik! Bagaimana dengan Anda?";
            case "siapa namamu", "siapa namamu?" -> "Saya adalah ChatBot Server! Saya siap membantu Anda.";
            case "selamat tinggal", "bye", "goodbye" -> "Sampai jumpa! Terima kasih telah menggunakan layanan kami.";
            case "help", "bantuan" -> generateHelpMessage();
            case "waktu", "jam berapa", "waktu sekarang" -> "Maaf, saya belum bisa memberikan informasi waktu saat ini.";
            case "siapa saya", "siapa saya?" -> "Anda adalah client #" + clientID + " yang terhubung dari " +
                    socket.getRemoteSocketAddress();
            case "terima kasih", "thanks", "thank you" -> "Sama-sama! Senang bisa membantu Anda.";
            case "test", "testing" -> "Test berhasil! Koneksi Anda berfungsi dengan baik.";
            // Easter eggs
            case "ping" -> "pong!";
            case "knock knock" -> "Who's there?";
            case "marco" -> "polo!";

            default -> {
                // Try to provide more helpful responses for common patterns
                if (lowerInput.contains("nama")) {
                    yield "Nama saya ChatBot Server. Apa nama Anda?";
                } else if (lowerInput.contains("umur") || lowerInput.contains("usia")) {
                    yield "Saya adalah program komputer, jadi saya tidak memiliki usia seperti manusia.";
                } else if (lowerInput.contains("bagaimana")) {
                    yield "Bisa Anda jelaskan lebih spesifik apa yang ingin Anda ketahui?";
                } else if (lowerInput.contains("dimana")) {
                    yield "Saya berada di server ini, melayani client seperti Anda!";
                } else if (lowerInput.contains("kapan")) {
                    yield "Maaf, saya tidak memiliki informasi waktu yang spesifik.";
                } else if (lowerInput.length() > 100) {
                    yield "Pesan Anda terlalu panjang. Coba gunakan kalimat yang lebih pendek.";
                } else {
                    yield "Maaf, saya tidak mengerti '" + input + "'. Ketik 'help' untuk melihat perintah yang tersedia.";
                }
            }
        };
    }

    private String generateHelpMessage() {
        return """
               Perintah yang tersedia:
               • halo/hai - Menyapa bot
               • apa kabar - Menanyakan kabar
               • siapa namamu - Menanyakan nama bot
               • help/bantuan - Menampilkan pesan ini
               • siapa saya - Informasi tentang koneksi Anda
               • test - Mengetes koneksi
               • terima kasih - Mengucapkan terima kasih
               • selamat tinggal - Keluar dari chat
               
               Anda juga bisa mencoba perintah lainnya!""";
    }

    public boolean isConnected(){
        return isConnected.get() && !socket.isClosed();
    }

    public String getConnectioninfo(){
        if(socket != null && !socket.isClosed()){
            return String.format("Client %d connected from %s", clientID, socket.getRemoteSocketAddress());
        }
        return String.format("Client %d [Disconnected]", clientID);
    }
}