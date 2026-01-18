package com.bhadabazaar.BhadaBazaar.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WhatsAppService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        if (accountSid != null && !accountSid.startsWith("ACxxx")) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio initialized");
        } else {
             log.warn("Twilio credentials not configured correctly. WhatsApp notifications will be simulated.");
        }
    }

    public void sendBookingNotification(String toPhoneNumber, String messageBody) {
        if (accountSid == null || accountSid.startsWith("ACxxx")) {
            log.info("SIMULATION: Sending WhatsApp to {}: {}", toPhoneNumber, messageBody);
            return;
        }

        try {
            // Ensure phone number has "whatsapp:" prefix
            String to = toPhoneNumber.startsWith("whatsapp:") ? toPhoneNumber : "whatsapp:" + toPhoneNumber;
            // From number should also have "whatsapp:" prefix (usually configured in properties)
            String from = fromNumber.startsWith("whatsapp:") ? fromNumber : "whatsapp:" + fromNumber;

            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(from),
                    messageBody
            ).create();

            log.info("WhatsApp sent. SID: {}", message.getSid());
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message", e);
        }
    }
}
