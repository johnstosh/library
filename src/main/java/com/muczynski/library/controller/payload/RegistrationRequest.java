/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller.payload;

public class RegistrationRequest {
    private String username;
    private String password;
    private String authority;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }
}