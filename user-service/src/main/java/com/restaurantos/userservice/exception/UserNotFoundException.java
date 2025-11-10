package com.restaurantos.userservice.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("User does not exist or has been removed by the admin");
    }

}
