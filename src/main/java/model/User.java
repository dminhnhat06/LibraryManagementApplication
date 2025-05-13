package model;

import Enumerations.AccountStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class User {
    private String id;
    private String name;
    private String username;
    private String password;
    private String email;
    private String phone;
    private AccountStatus status;
    private boolean isAdmin;
    private List<Book> borrowedBooks = new ArrayList<>();
    public User() {
    }
    public User(String id, String name, String username, String password, String email, String phone, AccountStatus status, boolean isAdmin) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.isAdmin = isAdmin;
    }
    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public String getEmail() {
        return email;
    }
    public String getPhone() {
        return phone;
    }
    public AccountStatus getStatus() {
        return status;
    }
    public boolean isAdmin() {
        return isAdmin;
    }
    public List<Book> getBorrowedBooks() {
        return borrowedBooks;
    }
    //Setters
    public void setId(String id) {
        this.id = id;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public void setStatus(AccountStatus status) {
        this.status = status;
    }
    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
    public void setBorrowedBooks(List<Book> borrowedBooks) {
        this.borrowedBooks = borrowedBooks;
    }
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", isAdmin=" + isAdmin +
                '}';
    }
}
