package com.banking.notification.domain;

import java.time.Instant;
import java.util.UUID;

public record Notification(UUID id, UUID transferId, String type, String message, Instant createdAt) { }
