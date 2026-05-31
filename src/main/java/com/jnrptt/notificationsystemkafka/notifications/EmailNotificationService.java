package com.jnrptt.notificationsystemkafka.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    public void sendWelcomeEmail(String to, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Bienvenido");
        message.setText("Hola " + name + ", tu cuenta ha sido creada correctamente");
        mailSender.send(message);
    }

    public void sendBudgetExceededEmail(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Aviso de gasto excedido");
        message.setText("Hola, has excedido tu gasto mensual. Por favor, revisa tu plan de gastos.");
        mailSender.send(message);
    }
}
