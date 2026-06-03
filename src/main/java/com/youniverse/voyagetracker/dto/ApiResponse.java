package com.youniverse.voyagetracker.dto;

public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    private ApiResponse() {
    }

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        return response;
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        ApiResponse<T> response = ok(data);
        response.message = message;
        return response;
    }

    public static <T> ApiResponse<T> fail(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
