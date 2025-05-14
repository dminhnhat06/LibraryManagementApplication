package model;

import java.util.ArrayList;
import java.util.List;

public class Book {
    private String id;
    private String title;
    private List<String> authors;
    private String publisher;
    private String publishedDate;
    private String printType;
    private int quantity;
    private int borrowedQuantity;
    private boolean isAvailable;
    private String imagePath;
    private List<BookItem> bookItems = new ArrayList<>();
    public Book() {
        this.authors = new ArrayList<>();
        this.bookItems = new ArrayList<>();
    }
    public Book(String id, String title, List<String> authors, String publisher, String publishedDate, String printType, int quantity, String imagePath) {}
    public Book(String id, String title, List<String> authors, String publisher, String publishedDate, String printType, int quantity, int borrowedQuantity, String imagePath) {
        this.id = id;
        this.title = title;
        this.authors = authors;
        this.publisher = publisher;
        this.publishedDate = publishedDate;
        this.printType = printType;
        this.quantity = quantity;
        this.isAvailable = true;
        this.imagePath = imagePath;
        this.borrowedQuantity = borrowedQuantity;
        for (int i = 0; i < quantity; i++) {
            bookItems.add(new BookItem(id));
        }
    }
    //Getters
    public String getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }
    public List<String> getAuthors() {
        return authors;
    }
    public String getPublisher() {
        return publisher;
    }
    public String getPublishedDate() {
        return publishedDate;
    }
    public String getPrintType() {
        return printType;
    }
    public int getQuantity() {
        return quantity;
    }
    public int getBorrowedQuantity() {
        return borrowedQuantity;
    }
    public String getImagePath() {
        return imagePath;
    }
    public boolean isAvailable() {
        return isAvailable;
    }
    public List<BookItem> getBookItems() {
        return bookItems;
    }
    //Setters
    public void setId(String id) {
        this.id = id;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }
    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }
    public void setPrintType(String printType) {
        this.printType = printType;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    public void setBorrowedQuantity(int borrowedQuantity) {
        this.borrowedQuantity = borrowedQuantity;
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    public void setAvailable(boolean available) {
        isAvailable = available;
    }
    public void setBookItems(List<BookItem> bookItems) { this.bookItems = bookItems;}
    public void addBookItem(BookItem item) {
        if (this.bookItems == null) {
            this.bookItems = new ArrayList<>();
        }
        this.bookItems.add(item);
        item.setBookId(this.id); // Đảm bảo bookId trong BookItem là chính xác
    }
}
