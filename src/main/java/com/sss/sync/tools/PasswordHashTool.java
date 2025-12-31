package com.sss.sync.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashTool {
  public static void main(String[] args) {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    String adminHash = encoder.encode("admin123");
    String userHash = encoder.encode("user123");

    System.out.println("admin123 bcrypt = " + adminHash);
    System.out.println("user123  bcrypt = " + userHash);
  }
}