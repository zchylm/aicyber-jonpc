package com.aicyber.backend.cart;

import com.aicyber.backend.cart.dto.CartPreparationRequest;
import com.aicyber.backend.cart.dto.CartPreparationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
public class CartCheckoutController {
    private final CartCheckoutService service;

    public CartCheckoutController(CartCheckoutService service) {
        this.service = service;
    }

    @PostMapping("/prepare")
    public CartPreparationResponse prepare(Authentication authentication, @RequestBody CartPreparationRequest request) {
        try {
            return service.prepare(UUID.fromString(authentication.getName()), request);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }
}
