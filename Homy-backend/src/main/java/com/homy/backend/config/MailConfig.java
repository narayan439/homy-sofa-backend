package com.homy.backend.config;

import com.homy.backend.service.LoggingMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:25}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUser;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Bean
    public JavaMailSender javaMailSender() {
        if (mailHost == null || mailHost.isEmpty() || mailHost.contains("example.com")) {
            return new LoggingMailSender();
        }

        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(mailHost);
        impl.setPort(mailPort);
        if (mailUser != null && !mailUser.isEmpty()) impl.setUsername(mailUser);
        if (mailPassword != null && !mailPassword.isEmpty()) impl.setPassword(mailPassword);

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        impl.setJavaMailProperties(props);
        return impl;
    }
}
