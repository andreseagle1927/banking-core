package com.banking.transfer;
import com.banking.platform.security.ResourceServerSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication @Import(ResourceServerSecurity.class) @EnableScheduling public class TransferServiceApplication { public static void main(String[] args) { SpringApplication.run(TransferServiceApplication.class, args); } }
