package com.homy.backend.service;

import com.homy.backend.model.Booking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendBookingConfirmation(Booking booking) {
        if (booking.getEmail() == null) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(booking.getEmail());
            msg.setSubject("Booking Confirmation - " + booking.getService());
            String body = String.format("Hello %s,\n\nYour booking for %s on %s has been received. Booking id: %s\n\nThanks,\nHomy Sofa Service",
                    booking.getName(), booking.getService(), booking.getDate(), booking.getId());
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception ex) {
            // swallow and log in real app
            System.err.println("Failed to send email: " + ex.getMessage());
        }
    }
}
