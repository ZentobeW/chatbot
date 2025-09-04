import javax.swing.*;

public class mainServer {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try{
                int port = Integer.parseInt(JOptionPane.showInputDialog("Enter port:"));
                new ServerGUI(port);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Port berupa angka");
            }
        });
    }
}
