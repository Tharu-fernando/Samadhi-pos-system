/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package CODE;

/**
 *
 * @author tharu
 */
public class HashGenerator {
    public static void main(String[] args) {
        String hash = CODE.PasswordUtil.hash("abc123".toCharArray());
        System.out.println(hash);
    }
}
