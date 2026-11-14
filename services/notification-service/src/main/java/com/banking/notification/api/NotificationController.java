package com.banking.notification.api;
import com.banking.notification.application.NotificationApplicationService;
import com.banking.notification.domain.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
@RestController @RequestMapping("/api/v1/notifications") public class NotificationController {
    private final NotificationApplicationService notifications;
    public NotificationController(NotificationApplicationService notifications) { this.notifications = notifications; }
    @GetMapping("/me") public List<Notification> me(JwtAuthenticationToken principal) {
        return notifications.forWallet(UUID.fromString(principal.getToken().getSubject()));
    }
}
