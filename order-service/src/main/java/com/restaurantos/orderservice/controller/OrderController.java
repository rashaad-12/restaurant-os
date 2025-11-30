package com.restaurantos.orderservice.controller;

import com.restaurantos.coresecurity.annotation.RestaurantCodes;
import com.restaurantos.orderservice.dto.OrderDTO;
import com.restaurantos.orderservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/order-api/v1/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody List<OrderDTO> request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<OrderDTO>> getAllOrder(@RestaurantCodes Set<String> restaurantCodes) {
        return ResponseEntity.ok(orderService.getAllOrder(restaurantCodes));
    }

    @PutMapping
    public ResponseEntity<String> updateOrder(@RequestBody List<OrderDTO> request) {
        return ResponseEntity.ok(orderService.updateOrder(request));
    }

    @PatchMapping("/accept")
    public ResponseEntity<String> acceptOrder(@RequestBody List<OrderDTO> request) {
        return ResponseEntity.ok(orderService.acceptOrder(request));
    }

    @PatchMapping("/prepare")
    public ResponseEntity<String> prepareOrder(@RequestBody List<OrderDTO> request) {
        return ResponseEntity.ok(orderService.prepareOrder(request));
    }

    @PatchMapping("/complete")
    public ResponseEntity<String> completeOrder(@RequestBody List<OrderDTO> request) {
        return ResponseEntity.ok(orderService.completeOrder(request));
    }

    @PatchMapping("/reject")
    public ResponseEntity<String> rejectOrder(@RequestBody List<OrderDTO> request) {
        return ResponseEntity.ok(orderService.rejectOrder(request));
    }

    @PatchMapping("/cancel")
    public ResponseEntity<String> cancelOrder(@RequestBody List<OrderDTO> request) {
        return ResponseEntity.ok(orderService.cancelOrder(request));
    }

    @DeleteMapping
    public ResponseEntity<String> deleteOrder(@RequestBody List<OrderDTO> request) {
        return ResponseEntity.ok(orderService.deleteOrder(request));
    }

}