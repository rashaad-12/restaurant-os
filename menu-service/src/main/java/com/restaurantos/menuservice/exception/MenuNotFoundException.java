package com.restaurantos.menuservice.exception;

public class MenuNotFoundException extends RuntimeException {

    public MenuNotFoundException() {
        super("Menu does not exist or has been removed by the admin");
    }

}
