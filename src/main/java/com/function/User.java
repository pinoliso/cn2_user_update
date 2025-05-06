package com.function;

public class User {
    public Long id;
    public String username;
    public String password;
    public String name;
    public String rol;

    public User(Long id, String username, String password, String name, String rol) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.name = name;
        this.rol = rol;
    }
    
    public User(String username, String password, String name, String rol) {
        this.id = null;
        this.username = username;
        this.password = password;
        this.name = name;
        this.rol = rol;
    }

    public User() {
        this.id = null;
        this.username = "";
        this.password = "";
        this.name = "";
        this.rol = "";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}

