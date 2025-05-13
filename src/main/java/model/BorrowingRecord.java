package model;

import Enumerations.Constants;

import java.time.LocalDate;
import java.util.UUID;

public class BorrowingRecord {
    private String borrowingRecordId;
    private BookItem bookItem;
    private User user;
    private String bookItemId;
    private String userId;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate = null;

    public BorrowingRecord() {
    }

    public BorrowingRecord(BookItem bookItem, User user) {
        this.borrowingRecordId = UUID.randomUUID().toString();
        this.bookItem = bookItem;
        this.user = user;
        this.bookItemId = bookItem.getBookItemId();
        this.userId = user.getId();
        this.borrowDate = LocalDate.now();
        this.dueDate = LocalDate.now().plusDays(Constants.getMaxLendingDays());
        this.returnDate = null;
    }

    public BorrowingRecord(String borrowingRecordId, String bookItemId, String userId, LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate) {
        this.borrowingRecordId = borrowingRecordId;
        this.bookItemId = bookItemId;
        this.userId = userId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
    }

    // Getters
    public String getBorrowingRecordId() {
        return borrowingRecordId;
    }

    public BookItem getBookItem() {
        return bookItem;
    }

    public User getUser() {
        return user;
    }

    public String getBookItemId() {
        return bookItemId;
    }

    public String getUserId() {
        return userId;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    // Setters
    public void setBorrowingRecordId(String borrowingRecordId) {
        this.borrowingRecordId = borrowingRecordId;
    }

    public void setBookItem(BookItem bookItem) {
        this.bookItem = bookItem;
        if (bookItem != null) this.bookItemId = bookItem.getBookItemId();
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) this.userId = user.getId();
    }

    public void setBookItemId(String bookItemId) {
        this.bookItemId = bookItemId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setBorrowDate(LocalDate borrowDate) {
        this.borrowDate = borrowDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }
}
