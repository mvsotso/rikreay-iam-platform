package com.iam.platform.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/unavailable")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Mono<Map<String, Object>> serviceUnavailable() {
        return Mono.just(Map.of(
                "success", false,
                "message", "Service is temporarily unavailable. Please try again later.",
                "errorCode", "SERVICE_UNAVAILABLE",
                "timestamp", Instant.now().toString()
        ));
    }
}
