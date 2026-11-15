package com.example.chocolateShop.exceptions;

public class InvalidImageUploadException extends RuntimeException {
    public InvalidImageUploadException(String message) {
        super(message);
    }

    public InvalidImageUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
