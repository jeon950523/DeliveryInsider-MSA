package com.deliveryinsider.simulator.domain.control.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SimulatorControlExceptionHandler {

    @ExceptionHandler(SimulatorEventNotFoundException.class)
    public ProblemDetail handleEventNotFound(
            SimulatorEventNotFoundException exception
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );

        problemDetail.setTitle("Simulator event not found");

        return problemDetail;
    }

    @ExceptionHandler(SimulatorPlatformNotImplementedException.class)
    public ProblemDetail handlePlatformNotImplemented(
            SimulatorPlatformNotImplementedException exception
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_IMPLEMENTED,
                exception.getMessage()
        );

        problemDetail.setTitle(
                "Simulator provider is not implemented"
        );

        return problemDetail;
    }
}
