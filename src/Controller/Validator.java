/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controller;

import java.util.regex.Pattern;

/**
 *
 * @author dave
 */
public class Validator {
    public static final int USERNAME_MIN = 3;
    public static final int USERNAME_MAX = 20;
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 64;
    public static final int PRODUCT_NAME_MAX = 50;
    public static final int STOCK_MAX = 10000;
    public static final double PRICE_MAX = 1000000.0;
    
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{3,20}");
    private static final Pattern PRODUCT_NAME = Pattern.compile("[A-Za-z0-9 _-]{1,50}");
    
    private Validator() { }
    
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME.matcher(username).matches();
    }
    
    public static boolean isValidPassword(char[] password) {
        if (password == null || password.length < PASSWORD_MIN || password.length > PASSWORD_MAX) {
            return false;
        }
        
        boolean upper = false, lower = false, digit = false, symbol = false;
        
        for(char c : password) {
            if (c < 33 || c > 126) {
                return false;
            }
            
            if (c >= 'A' && c <= 'Z') {
                upper = true;
            } else if (c >= 'a' && c <= 'z') {
                lower = true;
            } else if (c >= '0' && c<= '9') {
                digit = true;
            } else {
                symbol = true;
            }
        }
        
        return upper && lower && digit && symbol;
    }
    
    public static boolean isValidRole(int role) {
        return role >= 1 && role <= 5;
    }
    
    public static boolean isValidRole(String text) {
        if (text == null || !text.matches("[0-9]")) {
            return false;
        }
        
        return isValidRole(Integer.parseInt(text));
    }
    
    public static boolean isValidProductName(String name) {
        return name != null && PRODUCT_NAME.matcher(name).matches();
    }
    
    public static boolean isValidStock(int stock) {
        return stock >= 0 && stock <= STOCK_MAX;
    }
    
    public static boolean isValidStock(String text) {
        if (text == null || !text.matches("[0-9]{1,7}(\\.[0-9]{1,2})?")) {
            return false;
        }
        
        return isValidStock(Integer.parseInt(text));
    }
    
    public static boolean isValidPrice(double price) {
        return !Double.isNaN(price) && !Double.isInfinite(price) && price > 0 && price <= PRICE_MAX;
    }
    
    public static boolean isValidPrice(String text) {
        if (text == null || !text.matches("[0-9]{1,7}(\\.[0-9]{1,2})?")) {
            return false;
        }
        
        return isValidPrice(Double.parseDouble(text));
    }
}
