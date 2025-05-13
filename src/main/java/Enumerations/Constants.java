package Enumerations;

public class Constants {
    //Constants
    private static int MAX_BOOKS_ISSUED_TO_A_USER = 5;
    private static int MAX_LENDING_DAYS = 10;
    //Getters
    public static int getMaxLendingDays() {
        return MAX_LENDING_DAYS;
    }
    public static int getMaxBooksIssuedToAUser() {
        return MAX_BOOKS_ISSUED_TO_A_USER;
    }
    //Setters
    public static void setMaxLendingDays(int maxLendingDays) {
        MAX_LENDING_DAYS = maxLendingDays;
    }
    public static void setMaxBooksIssuedToAUser(int maxBooksIssuedToAUser) {
        MAX_BOOKS_ISSUED_TO_A_USER = maxBooksIssuedToAUser;
    }
}