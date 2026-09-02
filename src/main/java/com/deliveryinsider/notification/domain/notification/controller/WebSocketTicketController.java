package com.deliveryinsider.notification.domain.notification.controller;

import com.deliveryinsider.notification.domain.notification.response.WebSocketTicketResponse;
import com.deliveryinsider.notification.domain.notification.service.WebSocketTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class WebSocketTicketController {

    private final WebSocketTicketService ticketService;

    @PostMapping("/ws-ticket")
    public WebSocketTicketResponse issueTicket(
        @RequestHeader("X-User-Id")
        Long userId
    ) {
        return WebSocketTicketResponse.from(
            ticketService.issue(userId)
        );
    }
}
