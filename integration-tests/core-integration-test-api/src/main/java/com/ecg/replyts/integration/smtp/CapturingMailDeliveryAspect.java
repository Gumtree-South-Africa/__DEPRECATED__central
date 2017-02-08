package com.ecg.replyts.integration.smtp;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import com.ecg.replyts.core.api.model.mail.Mail;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CapturingMailDeliveryAspect {
    private Mail lastSentMail = null;
    
    @After("execution(* com.ecg..MailDeliveryService.deliverMail(com.ecg..Mail))")
    public void capture(JoinPoint joinPoint) {
        lastSentMail = (Mail) joinPoint.getArgs()[0];
    }
    
    public Mail getLastSentMail() {
        return lastSentMail;
    }
}
