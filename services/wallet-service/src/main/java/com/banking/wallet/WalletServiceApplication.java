package com.banking.wallet;
import com.banking.platform.security.ResourceServerSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
@SpringBootApplication @Import(ResourceServerSecurity.class) public class WalletServiceApplication { public static void main(String[] args) { SpringApplication.run(WalletServiceApplication.class, args); } }
