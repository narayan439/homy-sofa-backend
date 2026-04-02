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

    @Value("${spring.mail.protocol:smtp}")
    private String mailProtocol;

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

        impl.setDefaultEncoding("UTF-8");

        Properties props = new Properties();
        // Common SMTP properties
        props.put("mail.transport.protocol", mailProtocol != null && !mailProtocol.isEmpty() ? mailProtocol : "smtp");
        props.put("mail.smtp.auth", String.valueOf(true));
        props.put("mail.smtp.starttls.enable", String.valueOf(true));
        // reasonable timeouts
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        // allow trusting all hosts if using self-signed certs (can be overridden)
        props.put("mail.smtp.ssl.trust", "*");

        impl.setJavaMailProperties(props);
        return impl;
    }
}
