package com.restaurantos.orderservice.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException() {
        super("Order does not exist");
    }

}
