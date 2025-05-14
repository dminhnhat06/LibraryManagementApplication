package model;

import Enumerations.BookItemStatus;

import java.util.UUID;

public class BookItem {
    private String bookItemId;
    private String bookId;
    private BookItemStatus status;
    public BookItem() {
    }
    public BookItem(String bookId) {
        this.bookItemId = UUID.randomUUID().toString();
        this.bookId = bookId;
        this.status = BookItemStatus.AVAILABLE;
    }
    public BookItem(String bookItemId, String bookId, BookItemStatus status) {
        this.bookItemId = bookItemId;
        this.bookId = bookId;
        this.status = status;
    }
    //Getters
    public String getBookItemId() {
        return bookItemId;
    }
    public String getBookId() {
        return bookId;
    }
    public BookItemStatus getStatus() {
        return status;
    }
    public void setStatus(BookItemStatus status) {
        this.status = status;
    }
    //Setters
    public void setBookItemId(String bookItemId) {
        this.bookItemId = bookItemId;
    }
    public void setBookId(String bookId) {
        this.bookId = bookId;
    }
    public void setBookItemStatus(BookItemStatus bookItemStatus) {
        this.status = bookItemStatus;
    }
}
