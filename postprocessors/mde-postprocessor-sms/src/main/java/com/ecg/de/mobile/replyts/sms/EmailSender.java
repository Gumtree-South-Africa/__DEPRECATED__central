package com.ecg.de.mobile.replyts.sms;


import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mobile.util.Pair;

public class EmailSender {
	private final static Logger LOGGER = LoggerFactory.getLogger(EmailSender.class);


    private final Session defaultMailSession;

    private final static String DEFAULT_HOST = "localhost";
	
	public EmailSender(){

        this.defaultMailSession = Session.getInstance(createSessionProperties());

	}
	
	public void sendEmail(String subject, InternetAddress addressTo, InternetAddress addressFrom, InternetAddress addressReplyTo, String content, String contentType) throws AddressException, MessagingException {
	    sendEmail(subject, addressTo, addressFrom, addressReplyTo, content, contentType, false, null);
	}
	
	public void sendEmail(String subject, InternetAddress addressTo, InternetAddress addressFrom, InternetAddress addressReplyTo, String content, String contentType, boolean replyTS, List<Pair<String, String>> additionalHeaders) throws AddressException, MessagingException {

		Message msg = new MimeMessage(defaultMailSession);

		msg.setFrom(addressFrom);
		msg.setRecipient(Message.RecipientType.TO, addressTo);
	
		if (addressReplyTo != null){
			msg.setReplyTo(new InternetAddress[]{addressReplyTo});
		}
	
		msg.setSubject(subject);
		
		if(null != additionalHeaders){
		    for (Pair<String, String> pair : additionalHeaders) {

		        msg.addHeader(pair.getFirst(), pair.getSecond());
            }
		   
		}
		
		msg.setContent(content, contentType);
		Transport.send(msg);
		
		LOGGER.info("Send contact message to "+addressTo.getAddress()+".");
	}

    private Properties createSessionProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", false);
        props.put("mail.smtp.host", DEFAULT_HOST);
        return props;
    }

}

