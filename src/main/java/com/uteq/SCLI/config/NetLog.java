package com.uteq.SCLI.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import java.net.InetAddress;

@Slf4j
@Configuration
public class NetLog {
  /*@PostConstruct
  void logNet() throws Exception {
    log.info("preferIPv6Addresses={}, preferIPv4Stack={}",
        System.getProperty("java.net.preferIPv6Addresses"),
        System.getProperty("java.net.preferIPv4Stack"));
    for (InetAddress a : InetAddress.getAllByName("smtp.gmail.com")) {
      log.info("smtp.gmail.com -> {}", a.getHostAddress());
    }
  }*/
}
