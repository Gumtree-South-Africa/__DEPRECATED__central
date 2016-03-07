package com.ecg.de.mobile.replyts.sms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Map.Entry;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;



public class ContactMessageSmsService {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private static Map<String, String> ABBREVIATIONS = new HashMap<String, String>();
    static {
        ABBREVIATIONS.put(quote("autohaus"), "AH");
        ABBREVIATIONS.put(quote("fahrzeug"), "Fzg.");
        ABBREVIATIONS.put(quote("händler"), "Hdl.");
        ABBREVIATIONS.put(quote("mercedes-benz"), "MB");
        ABBREVIATIONS.put(quote("niederlassung"), "Ndl.");
        ABBREVIATIONS.put(quote("volkswagen"), "VW");
    }
    
    /*
     * The order is important for not encoding already encoded stuff.
     */
    private static List<Encoding> ENCODINGS = new ArrayList<Encoding>();
    static {
		ENCODINGS.add(new Encoding(quote("&"), "&amp;"));
    	ENCODINGS.add(new Encoding(quote("<"), "&lt;"));
    	ENCODINGS.add(new Encoding(quote(">"), "&gt;"));
    	ENCODINGS.add(new Encoding(quote("\u20AC"), "&#x20ac;"));
		
    }
    
    
    private static String quote(String word){
    	return "(?i)\\Q" +word+"\\E";
    }
    
    
	private final static int SMS_MAX_LENGTH = 160;
	private final InternetAddress providerEmail;
	private final InternetAddress senderEmail;
	private final EmailSender emailSender = new EmailSender();
	/**
	 * {userName}/{password}
	 */
	private final String subject = "mobile/&libor$";
	
	public ContactMessageSmsService() {
		try {
			this.providerEmail = new InternetAddress("mobile@sms-news.org");
			this.senderEmail = new InternetAddress("noreply@team.mobile.de");
			
		} catch (AddressException e){
			throw new RuntimeException(e);
		}
	}

	public boolean send(long adId,  ContactMessage contactMessage) throws IOException, MessagingException {
		String message = createMessage(contactMessage);
		long costcenter = contactMessage.getCustomerNumber();
		if (costcenter<=0){
			costcenter = 9999;
		}
		StringBuilder content = new StringBuilder();
        	content
        		.append("<an_nr>")
        		.append(contactMessage.getSmsPhoneNumber().replace("+", "00"))
        		.append("</an_nr>\n")

        		.append("<von_nr>")
        		.append("mobile.de")
        		.append("</von_nr>\n")
                
        		.append("<kst>")
        		.append(costcenter)
        		.append("</kst>\n")

        		.append("<text>")
        		.append(message)
        		.append("</text>\n");
        	
		String smsEmailText = content.toString().trim();
		emailSender.sendEmail(subject, providerEmail, senderEmail, null, smsEmailText, "text/plain;charset=ISO-8859-1");
		logger.debug("Sent <<"+smsEmailText+">>");
		return true;
	}
	

	private String createMessage(ContactMessage contactMessage) throws IOException {

		Locale sellerLocale = parse(contactMessage.getSellerLocale());
		
		StringBuilder sms = new StringBuilder();
		sms.append(ResourceBundle.getBundle("MYDATA", sellerLocale).getString("I18N.MYDATA.Sender")).append(": ");
		sms.append(cutBuyerDisplayName(contactMessage.getDisplayName()));
		sms.append(" - ");
		if (StringUtils.hasText(contactMessage.getBuyerPhoneNumber().getDisplayNumber())) {
			sms.append(contactMessage.getBuyerPhoneNumber().getDisplayNumber());
		} else {
			sms.append(contactMessage.getBuyerMailAddress());
		}
		sms.append(" - ");
		sms.append(getVehicleDescription(contactMessage, sellerLocale)).append("; ");
		if (StringUtils.hasText(contactMessage.getInternalNumber())) {
			sms.append(contactMessage.getInternalNumber());
		} else {
			String displayPrice = getDisplayPrice(contactMessage, sellerLocale);
			if (displayPrice != null){
				sms.append(displayPrice);
			}
		}
		sms.append("; ");
		
		
		
		
		String strSms = sms.toString();
		/*
		 * last character is a "
		 */
		if (strSms.length()>SMS_MAX_LENGTH-1){
			strSms = strSms.substring(0, SMS_MAX_LENGTH-5)+"...";
		}
		
		for (Encoding encoding : ENCODINGS) {
			strSms = strSms.replaceAll(encoding.getKey(), encoding.getValue());
		}
		
		
		return strSms;
	}
	
	
	private String cutBuyerDisplayName(String displayName){
		if (displayName == null) {
			return "";
		}
		
		for (Entry<String, String> entry : ABBREVIATIONS.entrySet()) {
			displayName = displayName.replaceAll(entry.getKey(), entry.getValue());
		}
		
		
		if (displayName.length()<=20){
			return displayName;
		}
		
		return displayName.substring(0, 20);
	}
	

	private String getVehicleDescription(ContactMessage contactMessage, Locale locale){		
		
		String asString = contactMessage.getMakeName();
		if (asString.length()>10){
			asString = asString.substring(0, 10);
		}
		
		String modelDescription = contactMessage.getModelDescription();
		if (modelDescription != null && modelDescription.length()>30){
			modelDescription = modelDescription.substring(0, 27)+"...";
		}
		
		if (modelDescription != null){
			asString = asString + " " + modelDescription;
		}
		
		return asString;		
	}
	
	
	// ad.getPriceInMainUnit()<=0 || ad.getCurrency()
	private String getDisplayPrice(ContactMessage contactMessage, Locale locale){
		if (contactMessage.getPriceInMainUnit()<=0 || contactMessage.getCurrency() == null){
			return null;
		}
		
		Currency currency = Currency.getInstance(contactMessage.getCurrency());
		
		return contactMessage.getPriceInMainUnit()+" "+currency.getSymbol(locale);
	}
	
	static class Encoding {
		
		private final String key;
		
		private final String value;
		
		public Encoding(String key, String value) {
			this.key = key;
			this.value = value;
		}
		
		public String getKey() {
			return key;
		}
		
		public String getValue() {
			return value;
		}
		
	}
	
	private static Locale parse(String string){
		String[] parts = string.split("_");
		if (parts.length<2){
			return new Locale(parts[0]);
		}
		
		return new Locale(parts[0], parts[1]);
	}
}