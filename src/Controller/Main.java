package Controller;


import Model.History;
import Model.Logs;
import Model.Product;
import Model.User;
import View.Frame;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;



public class Main {
    
    public SQLite sqlite;
    
    public static void main(String[] args) {
        new Main().init();
    }
    
    public void init(){
        // Initialize a driver object
        sqlite = new SQLite();

        // Create a database
        sqlite.createNewDatabase();
        
        // Drop users table if needed
        sqlite.dropHistoryTable();
        sqlite.dropLogsTable();
        sqlite.dropProductTable();
        sqlite.dropUserTable();
        
        // Create users table if not exist
        sqlite.createHistoryTable();
        sqlite.createLogsTable();
        sqlite.createProductTable();
        sqlite.createUserTable();
        
        // Add sample history
        sqlite.addHistory("admin", "Antivirus", 1, "2019-04-03 14:30:00.000");
        sqlite.addHistory("manager", "Firewall", 1, "2019-04-03 14:30:01.000");
        sqlite.addHistory("staff", "Scanner", 1, "2019-04-03 14:30:02.000");
        
        // Add sample logs
        sqlite.addLogs("NOTICE", "admin", "User creation successful", new Timestamp(new Date().getTime()).toString());
        sqlite.addLogs("NOTICE", "manager", "User creation successful", new Timestamp(new Date().getTime()).toString());
        sqlite.addLogs("NOTICE", "admin", "User creation successful", new Timestamp(new Date().getTime()).toString());
        
        // Add sample product
        sqlite.addProduct("Antivirus", 5, 500.0);
        sqlite.addProduct("Firewall", 3, 1000.0);
        sqlite.addProduct("Scanner", 10, 100.0);

        // Add sample users
        sqlite.addUser("admin", PasswordUtil.hash("qwerty1234".toCharArray()) , 5);
        sqlite.addUser("manager", PasswordUtil.hash("qwerty1234".toCharArray()), 4);
        sqlite.addUser("staff", PasswordUtil.hash("qwerty1234".toCharArray()), 3);
        sqlite.addUser("client1", PasswordUtil.hash("qwerty1234".toCharArray()));
        sqlite.addUser("client2", PasswordUtil.hash("qwerty1234".toCharArray()));
        
        
        // Get users
        ArrayList<History> histories = sqlite.getHistory();
        for(int nCtr = 0; nCtr < histories.size(); nCtr++){
            System.out.println("===== History " + histories.get(nCtr).getId() + " =====");
            System.out.println(" Username: " + histories.get(nCtr).getUsername());
            System.out.println(" Name: " + histories.get(nCtr).getName());
            System.out.println(" Stock: " + histories.get(nCtr).getStock());
            System.out.println(" Timestamp: " + histories.get(nCtr).getTimestamp());
        }
        
        // Get users
        ArrayList<Logs> logs = sqlite.getLogs();
        for(int nCtr = 0; nCtr < logs.size(); nCtr++){
            System.out.println("===== Logs " + logs.get(nCtr).getId() + " =====");
            System.out.println(" Username: " + logs.get(nCtr).getEvent());
            System.out.println(" Password: " + logs.get(nCtr).getUsername());
            System.out.println(" Role: " + logs.get(nCtr).getDesc());
            System.out.println(" Timestamp: " + logs.get(nCtr).getTimestamp());
        }
        
        // Get users
        ArrayList<Product> products = sqlite.getProduct();
        for(int nCtr = 0; nCtr < products.size(); nCtr++){
            System.out.println("===== Product " + products.get(nCtr).getId() + " =====");
            System.out.println(" Name: " + products.get(nCtr).getName());
            System.out.println(" Stock: " + products.get(nCtr).getStock());
            System.out.println(" Price: " + products.get(nCtr).getPrice());
        }
        // Get users
        ArrayList<User> users = sqlite.getUsers();
        for(int nCtr = 0; nCtr < users.size(); nCtr++){
            System.out.println("===== User " + users.get(nCtr).getId() + " =====");
            System.out.println(" Username: " + users.get(nCtr).getUsername());
            System.out.println(" Password: " + users.get(nCtr).getPassword());
            System.out.println(" Role: " + users.get(nCtr).getRole());
            System.out.println(" Locked: " + users.get(nCtr).getLocked());
        }
        
        
        // Initialize User Interface
        final Frame frame = new Frame();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            javax.swing.SwingUtilities.invokeLater(() -> {
                frame.resetToLogin();
                ErrorHandler.show(frame);
            });
        });
        frame.init(this);
    }

    public void log(String event, String username, String desc) {
        try {
            String safeEvent = (event == null || event.trim().isEmpty()) ? "NOTICE" : event.trim();
            String safeUser = (username == null || username.trim().isEmpty()) ? "unknown" : username.trim();
            String safeDesc = (desc == null) ? "" : desc;
            if (safeUser.length() > 100) {
                safeUser = safeUser.substring(0, 100);
            }
            if (safeDesc.length() > 100) {
                safeDesc = safeDesc.substring(0, 100);
            }
            String timestamp = new Timestamp(new Date().getTime()).toString();
            sqlite.addLogs(safeEvent, safeUser, safeDesc, timestamp);
        } catch (Exception ex) {
            // Do not crash application if logging fails
        }
    }
    
}
