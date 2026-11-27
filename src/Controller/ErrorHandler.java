package Controller;

import java.awt.Component;
import javax.swing.JOptionPane;

public class ErrorHandler {

    public static void show(Component parent) {
        JOptionPane.showMessageDialog(parent, "Something went wrong. Please try again.",
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}
