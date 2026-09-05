// src/main/java/com/uteq/SCLI/controller/NetDiagController.java
package com.uteq.SCLI.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RestController
public class NetDiagController {

  record Hop(String host, int port, boolean ok, String ip, String error) {}

  @GetMapping("/__diag/smtp")
  public List<Hop> diag() {
    List<Hop> r = new ArrayList<>();
    int timeout = (int) Duration.ofSeconds(5).toMillis();
    try {
      for (InetAddress ia : InetAddress.getAllByName("smtp.gmail.com")) {
        // pruebo 587 y 465 a cada IP resuelta (v4/v6)
        r.add(test(ia, 587, timeout));
        r.add(test(ia, 465, timeout));
      }
    } catch (Exception e) {
      r.add(new Hop("smtp.gmail.com", -1, false, "", "DNS: " + e.toString()));
    }
    return r;
  }

  private Hop test(InetAddress ia, int port, int timeoutMs) {
    String host = ia.getHostAddress();
    try (Socket s = new Socket()) {
      s.connect(new java.net.InetSocketAddress(ia, port), timeoutMs);
      return new Hop("smtp.gmail.com", port, true, host, null);
    } catch (Exception ex) {
      return new Hop("smtp.gmail.com", port, false, host, ex.getClass().getSimpleName() + ": " + ex.getMessage());
    }
  }
}
