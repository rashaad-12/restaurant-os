package com.restaurantos.identityservice.restaurant.exception;

public class RestaurantNotFoundException extends RuntimeException {

    public RestaurantNotFoundException() {
        super("Restaurant does not exist or has been removed by the admin");
    }

}
