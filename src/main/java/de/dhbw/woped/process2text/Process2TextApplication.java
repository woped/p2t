package de.dhbw.woped.process2text;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "de.dhbw.woped.process2text")
public class Process2TextApplication {

  public static void main(String[] args) {
    SpringApplication.run(Process2TextApplication.class, args);
  }
}
