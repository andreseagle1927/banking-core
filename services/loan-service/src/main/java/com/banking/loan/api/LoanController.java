package com.banking.loan.api;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/v1/loans") public class LoanController {
    @GetMapping public Map<String,String> index() { return Map.of("status", "skeleton", "service", "loan-service", "note", "Loan origination and repayment schedule are not implemented yet."); }
}
