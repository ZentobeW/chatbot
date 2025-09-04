import javax.swing.*;

public class mainClient {
    public static void main(String[] args) {
        String input = JOptionPane.showInputDialog("Masukkan jumlah client:");
        int jumlahClient = Integer.parseInt(input);

        for (int i = 0; i < jumlahClient; i++) {
            new Thread(() -> new ClientGUI("localhost")).start();
        }
    }
}

