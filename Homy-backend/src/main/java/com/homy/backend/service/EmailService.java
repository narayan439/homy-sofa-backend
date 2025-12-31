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
            logger.error("Failed to build booking confirmation email for {}: {}", booking.getEmail(), ex.getMessage(), ex);
        } catch (Exception ex) {
            // Catch MailException and other runtime exceptions from JavaMailSender.send
            logger.error("Failed to send booking confirmation email to {}: {}", booking.getEmail(), ex.getMessage(), ex);
        }
    }

    /**
     * Build HTML email template for booking confirmation
     */
    private String buildConfirmationEmailHtml(Booking booking) {
        String bookingDate = booking.getDate() != null ? booking.getDate() : "Not specified";
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
     * Send booking status change email
     */
    public void sendStatusChangeEmail(Booking booking, String oldStatus, String newStatus) {
        if (booking.getEmail() == null || booking.getEmail().isEmpty()) {
            logger.warn("Booking has no email, skipping status change email for booking id={}", booking.getId());
            return;
        }

        try {
            logger.info("Preparing status change email for {} <{}> (status: {} -> {})", 
                booking.getName(), booking.getEmail(), oldStatus, newStatus);
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(booking.getEmail());
            helper.setSubject(getStatusChangeEmailSubject(newStatus));
            helper.setText(buildStatusChangeEmailContent(booking, oldStatus, newStatus), true);
            
            mailSender.send(mimeMessage);
            logger.info("Status change email sent successfully to {} for booking id={}", 
                booking.getEmail(), booking.getId());
        } catch (MessagingException e) {
            logger.error("Failed to send status change email for booking id={}", booking.getId(), e);
            throw new RuntimeException("Failed to send status change email", e);
        }
    }

    /**
     * Get email subject based on new status
     */
    private String getStatusChangeEmailSubject(String status) {
        switch (status.toUpperCase()) {
            case "APPROVED":
                return "Your Booking is Approved - " + appName;
            case "COMPLETED":
                return "Your Booking is Completed - " + appName;
            case "CANCELLED":
                return "Your Booking has been Cancelled - " + appName;
            default:
                return "Booking Status Update - " + appName;
        }
    }

    /**
     * Build HTML content for status change email
     */
    private String buildStatusChangeEmailContent(Booking booking, String oldStatus, String newStatus) {
        String statusMessage = getStatusMessage(newStatus);
        String headerColor = getStatusColor(newStatus);
        String icon = getStatusIcon(newStatus);
        String serviceName = booking.getService() != null ? booking.getService() : "Your Service";
        
        return String.format(
            "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <style>\n" +
            "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }\n" +
            "        .container { max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; }\n" +
            "        .header { background-color: %s; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }\n" +
            "        .content { background-color: white; padding: 30px; border: 1px solid #ddd; border-radius: 0 0 5px 5px; }\n" +
            "        .status-box { background-color: %s; padding: 15px; margin: 20px 0; border-radius: 5px; text-align: center; border-left: 4px solid %s; }\n" +
            "        .status-icon { font-size: 32px; margin-bottom: 10px; }\n" +
            "        .status-text { font-size: 20px; font-weight: bold; color: %s; }\n" +
            "        .details { margin: 20px 0; }\n" +
            "        .detail-row { margin: 10px 0; padding: 10px; background-color: #f5f5f5; border-left: 4px solid %s; }\n" +
            "        .detail-label { font-weight: bold; color: %s; }\n" +
            "        .reference-box { background-color: #e8f5e9; padding: 15px; margin: 20px 0; border-radius: 5px; text-align: center; }\n" +
            "        .reference-code { font-size: 18px; font-weight: bold; color: #2E7D32; font-family: monospace; }\n" +
            "        .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; text-align: center; font-size: 12px; color: #666; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"container\">\n" +
            "        <div class=\"header\">\n" +
            "            <h1>%s Booking Status Update</h1>\n" +
            "        </div>\n" +
            "        <div class=\"content\">\n" +
            "            <p>Dear <strong>%s</strong>,</p>\n" +
            "\n" +
            "            <div class=\"status-box\">\n" +
            "                <div class=\"status-icon\">%s</div>\n" +
            "                <div class=\"status-text\">%s</div>\n" +
            "            </div>\n" +
            "\n" +
            "            <p>Your booking has been <strong>%s</strong>.</p>\n" +
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
            "                    <span class=\"detail-label\">Current Status:</span> <strong>%s</strong>\n" +
            "                </div>\n" +
            "                <div class=\"detail-row\">\n" +
            "                    <span class=\"detail-label\">Booking Date:</span> %s\n" +
            "                </div>\n" +
            "            </div>\n" +
            "\n" +
            "            <p>If you have any questions regarding this status change, please feel free to contact us.</p>\n" +
            "\n" +
            "            <div class=\"footer\">\n" +
            "                <p>© %d %s. All rights reserved.</p>\n" +
            "                <p>Update sent on: %s</p>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</body>\n" +
            "</html>",
            headerColor,
            getStatusBgColor(newStatus),
            headerColor,
            headerColor,
            headerColor,
            headerColor,
            icon,
            icon,
            statusMessage,
            booking.getName(),
            icon,
            statusMessage,
            newStatus.toLowerCase(),
            booking.getReference() != null ? booking.getReference() : "ID-" + booking.getId(),
            serviceName,
            newStatus,
            booking.getDate() != null ? booking.getDate() : "Not specified",
            LocalDateTime.now().getYear(),
            appName,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss"))
        );
    }

    /**
     * Get status message based on status
     */
    private String getStatusMessage(String status) {
        switch (status.toUpperCase()) {
            case "APPROVED":
                return "Your booking has been approved!";
            case "COMPLETED":
                return "Your booking is complete!";
            case "CANCELLED":
                return "Your booking has been cancelled.";
            default:
                return "Your booking status has been updated.";
        }
    }

    /**
     * Get status color based on status
     */
    private String getStatusColor(String status) {
        switch (status.toUpperCase()) {
            case "APPROVED":
                return "#4CAF50";  // Green
            case "COMPLETED":
                return "#2196F3";  // Blue
            case "CANCELLED":
                return "#f44336";  // Red
            default:
                return "#FF9800";  // Orange
        }
    }

    /**
     * Get status background color
     */
    private String getStatusBgColor(String status) {
        switch (status.toUpperCase()) {
            case "APPROVED":
                return "#E8F5E9";  // Light green
            case "COMPLETED":
                return "#E3F2FD";  // Light blue
            case "CANCELLED":
                return "#FFEBEE";  // Light red
            default:
                return "#FFF3E0";  // Light orange
        }
    }

    /**
     * Get status icon
     */
    private String getStatusIcon(String status) {
        switch (status.toUpperCase()) {
            case "APPROVED":
                return "✓";  // Checkmark
            case "COMPLETED":
                return "✓✓";  // Double checkmark
            case "CANCELLED":
                return "✗";  // X mark
            default:
                return "⚡";  // Lightning
        }
    }
}
