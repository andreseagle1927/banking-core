package com.banking.customer.api;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/v1/customers") public class CustomerController {
    @GetMapping("/me") public Map<String,String> me() { return Map.of("status", "skeleton", "service", "customer-service", "note", "Customer profile endpoints are scaffolded; persistence is not implemented yet."); }
}
