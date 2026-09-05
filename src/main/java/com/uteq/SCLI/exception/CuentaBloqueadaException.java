package com.uteq.SCLI.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.LOCKED) // 423
public class CuentaBloqueadaException extends RuntimeException {

    private final long minutosRestantes;

    public CuentaBloqueadaException(String message, long minutosRestantes) {
        super(message);
        this.minutosRestantes = minutosRestantes;
    }

    public long getMinutosRestantes() {
        return minutosRestantes;
    }
}