package com.homy.backend.service;

import com.homy.backend.model.Booking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.homy.backend.repository.ServiceRepository;
import com.homy.backend.model.ServiceEntity;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private ServiceRepository serviceRepository;

    @Value("${app.email.from:noreply@homysofa.com}")
    private String fromEmail;

    @Value("${app.app.name:Homy Sofa}")
    private String appName;

    /**
     * Send booking confirmation email with HTML template
     */
    public void sendBookingConfirmation(Booking booking) {
        if (booking.getEmail() == null || booking.getEmail().isEmpty()) {
            logger.warn("Booking has no email, skipping confirmation for booking id={}", booking.getId());
            return;
        }

        try {
            logger.info("Preparing booking confirmation email for {} <{}>", booking.getName(), booking.getEmail());
            // Log JavaMailSender implementation and connection properties (no passwords)
            if (mailSender != null) {
                logger.info("JavaMailSender implementation: {}", mailSender.getClass().getName());
                if (mailSender instanceof JavaMailSenderImpl) {
                    JavaMailSenderImpl impl = (JavaMailSenderImpl) mailSender;
                    try {
                        logger.info("SMTP host={} port={} username={} protocol={}", impl.getHost(), impl.getPort(), impl.getUsername(), impl.getProtocol());
                    } catch (Exception e) {
                        logger.debug("Failed to read JavaMailSenderImpl properties", e);
                    }
                }
            } else {
                logger.warn("mailSender is null");
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(booking.getEmail());
            helper.setFrom(fromEmail);
            helper.setSubject("Booking Confirmation - " + appName);

            String htmlBody = buildConfirmationEmailHtml(booking);
            helper.setText(htmlBody, true); // true = HTML content

            mailSender.send(message);
            logger.info("Booking confirmation email sent to: {}", booking.getEmail());
        } catch (MessagingException ex) {
            logger.error("Failed to send booking confirmation email to {}: {}", booking.getEmail(), ex.getMessage(), ex);
        }
    }

    /**
     * Build HTML email template for booking confirmation
     */
    private String buildConfirmationEmailHtml(Booking booking) {
        String bookingDate = formatDate(booking.getDate());
        String confirmationDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm a"));
        // Resolve service display name: if booking.service looks like an ID, try to fetch the real name
        String serviceDisplay = booking.getService() != null ? booking.getService() : "Not specified";
        try {
            if (serviceDisplay.startsWith("svc-") || serviceDisplay.matches("^[a-zA-Z0-9\\-]{6,}")) {
                ServiceEntity svc = serviceRepository.findById(serviceDisplay).orElse(null);
                if (svc != null && svc.getName() != null && !svc.getName().isBlank()) {
                    serviceDisplay = svc.getName();
                }
            }
        } catch (Exception e) {
            // be defensive: if lookup fails, keep original value
        }

        return String.format(
            "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <style>\n" +
            "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }\n" +
            "        .container { max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; }\n" +
            "        .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }\n" +
            "        .content { background-color: white; padding: 30px; border: 1px solid #ddd; border-radius: 0 0 5px 5px; }\n" +
            "        .details { margin: 20px 0; }\n" +
            "        .detail-row { margin: 10px 0; padding: 10px; background-color: #f5f5f5; border-left: 4px solid #4CAF50; }\n" +
            "        .detail-label { font-weight: bold; color: #4CAF50; }\n" +
            "        .reference-box { background-color: #e8f5e9; padding: 15px; margin: 20px 0; border-radius: 5px; text-align: center; }\n" +
            "        .reference-code { font-size: 24px; font-weight: bold; color: #2E7D32; font-family: monospace; }\n" +
            "        .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; text-align: center; font-size: 12px; color: #666; }\n" +
            "        .button { display: inline-block; padding: 10px 20px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px; margin-top: 20px; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"container\">\n" +
            "        <div class=\"header\">\n" +
            "            <h1>✓ Booking Confirmed</h1>\n" +
            "        </div>\n" +
            "        <div class=\"content\">\n" +
            "            <p>Dear <strong>%s</strong>,</p>\n" +
            "            <p>Thank you for booking with %s! Your booking has been successfully received and confirmed.</p>\n" +
            "\n" +
            "            <div class=\"reference-box\">\n" +
            "                <p style=\"margin: 0 0 10px 0; color: #666;\">Your Booking Reference:</p>\n" +
            "                <div class=\"reference-code\">%s</div>\n" +
            "            </div>\n" +
            "\n" +
            "            <h3>Booking Details:</h3>\n" +
            "            <div class=\"details\">\n" +
            "                <div class=\"detail-row\">\n" +
            "                    <span class=\"detail-label\">Service:</span> %s\n" +
            "                </div>\n" +
            "                <div class=\"detail-row\">\n" +
            "                    <span class=\"detail-label\">Booking Date:</span> %s\n" +
            "                </div>\n" +
            "                <div class=\"detail-row\">\n" +
            "                    <span class=\"detail-label\">Phone:</span> %s\n" +
            "                </div>\n" +
            "                <div class=\"detail-row\">\n" +
            "                    <span class=\"detail-label\">Email:</span> %s\n" +
            "                </div>\n" +
            (booking.getPrice() != null ? String.format(
            "                <div class=\"detail-row\">\n" +
            "                    <span class=\"detail-label\">Price:</span> ₹ %.2f\n" +
            "                </div>\n", booking.getPrice()) : "") +
            "                <!-- Status removed from email as per request -->\n" +
            "            </div>\n" +
            "\n" +
            "            <p><strong>What happens next?</strong></p>\n" +
            "            <ul>\n" +
            "                <li>Our team will review your booking and confirm availability.</li>\n" +
            "                <li>You will receive a confirmation call or email within 24 hours.</li>\n" +
            "                <li>Keep your booking reference number for your records.</li>\n" +
            "            </ul>\n" +
            "\n" +
            "            <p>If you have any questions, please don't hesitate to contact us.</p>\n" +
            "\n" +
            "            <div class=\"footer\">\n" +
            "                <p>© %d %s. All rights reserved.</p>\n" +
            "                <p>Booking confirmed on: %s</p>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</body>\n" +
            "</html>",
            booking.getName(),
            appName,
            booking.getReference() != null ? booking.getReference() : "ID-" + booking.getId(),
            serviceDisplay,
            bookingDate,
            booking.getPhone() != null ? booking.getPhone() : "Not provided",
            booking.getEmail(),
            LocalDateTime.now().getYear(),
            appName,
            confirmationDate
        );
    }

    /**
     * Format date string for display
     */
    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return "Not specified";
        }
        try {
            return dateStr;
        } catch (Exception e) {
            return dateStr;
        }
    }
}
