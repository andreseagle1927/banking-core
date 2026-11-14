package com.banking.transfer.api;

import com.banking.transfer.application.TransferApplicationService;
import com.banking.transfer.application.TransferCommand;
import com.banking.transfer.domain.TransferRecord;
import com.banking.transfer.domain.TransferStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {
    private final TransferApplicationService transfers;
    public TransferController(TransferApplicationService transfers) { this.transfers = transfers; }

    @PostMapping
    public ResponseEntity<TransferResponse> create(@RequestHeader("Idempotency-Key") @NotBlank @Size(max=128) String idempotencyKey,
                                      @Valid @RequestBody CreateTransferRequest request,
                                      JwtAuthenticationToken principal) {
        UUID senderWalletId;
        try { senderWalletId = UUID.fromString(principal.getToken().getSubject()); }
        catch (IllegalArgumentException ex) { throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated subject is not a UUID wallet owner in this prototype"); }
        TransferRecord response = transfers.create(idempotencyKey, senderWalletId,
                new TransferCommand(request.recipientWalletId(), request.amount(), request.currency()));
        return ResponseEntity.status(switch (response.status()) {
            case REJECTED -> 422;
            case PENDING -> 202;
            case COMPLETED -> 200;
        }).body(TransferResponse.from(response));
    }

    @GetMapping("/{transferId}")
    public TransferResponse get(@PathVariable UUID transferId, JwtAuthenticationToken principal) {
        UUID walletId = UUID.fromString(principal.getToken().getSubject());
        return TransferResponse.from(transfers.get(transferId, walletId));
    }

    public record CreateTransferRequest(@NotNull UUID recipientWalletId, @NotNull @Positive @Digits(integer=16, fraction=2) BigDecimal amount,
                                        @NotBlank @Pattern(regexp="[A-Z]{3}") String currency) { }
    public record TransferResponse(UUID transferId, String status, UUID senderWalletId, UUID recipientWalletId,
                                   String amount, String currency, UUID ledgerTransactionId, String failureReason) {
        public static TransferResponse from(TransferRecord transfer) {
            return new TransferResponse(transfer.id(), transfer.status().name(), transfer.senderWalletId(),
                    transfer.recipientWalletId(), transfer.amount().toPlainString(), transfer.currency(),
                    transfer.ledgerTransactionId(), transfer.failureReason());
        }
    }
}
