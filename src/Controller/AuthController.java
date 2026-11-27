/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controller;

import Model.User;
import java.util.Arrays;

/**
 *
 * @author dave
 */
public class AuthController {
    public static class LoginResult {
        public final User user;
        public final String lastAttempt;
        public final String lastStatus;
        
        LoginResult(User user, String lastAttempt, String lastStatus) {
            this.user = user;
            this.lastAttempt = lastAttempt;
            this.lastStatus = lastStatus;
        }
    }
    
    private final Main main;
    
    public AuthController(Main main) {
        this.main = main;
    }
    
    public LoginResult login(String username, char[] password) {
        try {
            System.out.println("DEBUG user=[" + username + "] pwlen=" + password.length);
            
            if (!Validator.isValidUsername(username) || password.length == 0 || password.length > Validator.PASSWORD_MAX) {
                main.log("VALIDATION_FAILURE", "unknown", "Login input rejected");
                return null;
            }
            
            User user = main.sqlite.getUser(username);
            System.out.println("DEBUG found=" + (user != null));
            
            if (user == null) {
                main.log("LOGIN_FAILED", username, "Unknown user");
                return null;
            }
            
            if (user.getRole() == 1 || user.getLocked() == 1) {
                System.out.println("DEBUG account disabled, role=" + user.getRole());
                main.log("LOGIN_FAILED", username, "Attempt on disabled account");
                return null;
            }
            
            String now = new java.sql.Timestamp(System.currentTimeMillis()).toString();
            
            boolean ok = PasswordUtil.verify(password, user.getPassword());
            System.out.println("DEBUG verify=" + ok);
            
            if (!PasswordUtil.verify(password, user.getPassword())) {
                boolean disabled = main.sqlite.recordFailure(username, now);
                main.log("LOGIN_FAILED", username, "Wrong password");
                
                if (disabled) {
                    main.log("ACCOUNT_LOCKED", username, "Disabled after 5 failed attempts");
                }
                
                return null;
            }
            
            String[] last = main.sqlite.getLastAttempt(username);
            main.sqlite.recordSuccess(username, now);
            main.log("LOGIN_SUCCESS", username, "Successful login");
            return new LoginResult(user, last[0], last[1]);
        } catch (Exception e) {
            System.out.println("DEBUG exception: " + e);
            return null;
        }
    }
    
    public boolean register(String username, char[] password, char[] confirm) {
        try {
            if (!Validator.isValidUsername(username) || !Validator.isValidPassword(password) || !Arrays.equals(password, confirm) || main.sqlite.getUser(username) != null) {
                main.log("VALIDATION_FAILURE", "unknown", "Registration rejected");
                return false;
            }
            main.sqlite.addUser(username, PasswordUtil.hash(password));
            main.log("NOTICE", username, "User registered");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
