package com.banking.transfer.api;

import com.banking.transfer.application.exception.TransferConflictException;
import com.banking.transfer.application.exception.TransferNotFoundException;
import com.banking.transfer.domain.TransferRuleViolation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class TransferExceptionHandler {
    @ExceptionHandler(TransferConflictException.class)
    public void conflict(TransferConflictException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage()); }

    @ExceptionHandler(TransferNotFoundException.class)
    public void notFound(TransferNotFoundException ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage()); }

    @ExceptionHandler(TransferRuleViolation.class)
    public void invalid(TransferRuleViolation ex) { throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()); }
}
