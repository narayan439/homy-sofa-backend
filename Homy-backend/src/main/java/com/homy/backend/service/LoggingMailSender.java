package com.homy.backend.service;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class LoggingMailSender implements JavaMailSender {

    @Override
    public MimeMessage createMimeMessage() {
        return new MimeMessage(Session.getDefaultInstance(new Properties()));
    }

    @Override
    public MimeMessage createMimeMessage(java.io.InputStream contentStream) throws MailException {
        try {
            return new MimeMessage(Session.getDefaultInstance(new Properties()), contentStream);
        } catch (MessagingException e) {
            throw new MailException("Failed to create MimeMessage from stream") {
            };
        }
    }

    @Override
    public void send(MimeMessage mimeMessage) throws MailException {
        System.out.println("[LoggingMailSender] pretending to send MimeMessage");
    }

    @Override
    public void send(MimeMessage... mimeMessages) throws MailException {
        System.out.println("[LoggingMailSender] pretending to send " + mimeMessages.length + " MimeMessages");
    }

    @Override
    public void send(SimpleMailMessage simpleMessage) throws MailException {
        System.out.println("[LoggingMailSender] to=" + String.join(",", simpleMessage.getTo()) + " subject=" + simpleMessage.getSubject() + "\n" + simpleMessage.getText());
    }

    @Override
    public void send(SimpleMailMessage... simpleMessages) throws MailException {
        for (SimpleMailMessage m : simpleMessages) {
            send(m);
        }
    }
}
