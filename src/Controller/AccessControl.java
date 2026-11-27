package Controller;

import Model.User;

public class AccessControl {

    public static boolean canAccess(User user, String panel) {
        try {
            if (user == null || panel == null || user.getLocked() == 1) {
                return false;
            }
            int role = user.getRole();
            if ("admin".equals(panel) || "logs".equals(panel)) {
                return role == 5;
            } else if ("manager".equals(panel)) {
                return role == 4;
            } else if ("staff".equals(panel)) {
                return role == 3;
            } else if ("client".equals(panel)) {
                return role == 2;
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    public static String getHomePanel(User user) {
        try {
            if (user == null || user.getLocked() == 1) {
                return null;
            }
            switch (user.getRole()) {
                case 5:
                    return "admin";
                case 4:
                    return "manager";
                case 3:
                    return "staff";
                case 2:
                    return "client";
                default:
                    return null;
            }
        } catch (Exception ex) {
            return null;
        }
    }
}
