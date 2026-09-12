/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package CODE;

/**
 *
 * @author tharu
 */
public class Session {
    private static int currentUserId = -1;
    private static String currentUsername = null;
    private static String currentRole = null; // "Cashier" or "Admin"

    public static void login(int userId, String username, String role) {
        currentUserId = userId;
        currentUsername = username;
        currentRole = role;
    }

    public static void logout() {
        currentUserId = -1;
        currentUsername = null;
        currentRole = null;
    }
    
    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static String getCurrentUsername() {
        return currentUsername;
    }

    public static boolean isAdmin() {
        return "Admin".equals(currentRole);
    }

    public static boolean isLoggedIn() {
        return currentUserId != -1;
    }
}
