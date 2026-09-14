package com.deliveryinsider.simulator.domain.baemin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SimulatorExceptionHandler {

    @ExceptionHandler(SimulatorOrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(SimulatorOrderNotFoundException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            exception.getMessage()
        );
        problemDetail.setTitle("Simulator order not found");
        return problemDetail;
    }
    @ExceptionHandler(
            SimulatorInvalidOrderStatusTransitionException.class
    )
    public ProblemDetail handleInvalidStatusTransition(
            SimulatorInvalidOrderStatusTransitionException exception
    ) {
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        exception.getMessage()
                );

        problemDetail.setTitle(
                "Invalid simulator order status transition"
        );

        return problemDetail;
    }

    @ExceptionHandler(SimulatorInvalidCancellationReasonException.class)
    public ProblemDetail handleInvalidCancellationReason(
            SimulatorInvalidCancellationReasonException exception
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "취소 사유를 입력해 주세요."
        );
        problemDetail.setTitle("Invalid cancellation reason");
        return problemDetail;
    }
}
