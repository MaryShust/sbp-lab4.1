package com.example.sbp.controller;

import com.example.sbp.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exchange-rate")
@RequiredArgsConstructor
@Tag(name = "ExchangeRate", description = "Курс валют")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/rate")
    @Operation(
            summary = "Запросить курс валюты",
            description = "Осуществляет получение текущего курса конкретных валют."
    )
    public ResponseEntity<?> getRate(
            @Parameter(description = "Валюта с которой переводим", example = "USD")
            @RequestParam(defaultValue = "USD") String base,
            @Parameter(description = "Валюта в которую переводим", example = "USD")
            @RequestParam String target
    ) {
        return ResponseEntity.ok(exchangeRateService.getRate(base, target));
    }
}
