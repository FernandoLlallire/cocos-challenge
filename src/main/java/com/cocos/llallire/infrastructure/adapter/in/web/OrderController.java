package com.cocos.llallire.infrastructure.adapter.in.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cocos.llallire.application.dto.OrderInformation;
import com.cocos.llallire.application.port.in.CancelOrderUseCase;
import com.cocos.llallire.application.port.in.PlaceOrderUseCase;

/**
 * REST adapter for orders.
 */
@RestController
@RequestMapping(path = ApiPaths.V1 + "/orders")
public class OrderController {
    private final PlaceOrderUseCase placeOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;

    /**
     * Creates the controller.
     *
     * @param placeOrderUseCase  order placement use case
     * @param cancelOrderUseCase order cancellation use case
     */
    public OrderController(PlaceOrderUseCase placeOrderUseCase, CancelOrderUseCase cancelOrderUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
    }

    /**
     * {@code POST /api/v1/orders}: sends an order to the market.
     *
     * <p>Responds 201 whenever the order is recorded, even if it ended REJECTED: the rejection is a business
     * outcome and is stored in the {@code orders} table. Format errors return 400 and are not
     * stored.</p>
     *
     * @param request order data
     * @return 201 with the recorded order and its status
     */
    @PostMapping
    public ResponseEntity<OrderInformation> placeOrder(@RequestBody PlaceOrderRequest request) {
        OrderInformation order = placeOrderUseCase.execute(request.toCommand());
        return ResponseEntity.created(URI.create(ApiPaths.V1 + "/orders/" + order.id())).body(order);
    }

    /**
     * {@code POST /api/v1/orders/{orderId}/cancel}: cancels an order in NEW status.
     *
     * @param orderId order to cancel
     * @return 200 with the cancelled order; 404 if it does not exist; 409 if it is not in NEW status
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderInformation> cancelOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(cancelOrderUseCase.execute(orderId));
    }
}
