package com.ecg.de.mobile.replyts.fsbo.fraud.broker;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ecg.replyts.core.api.model.mail.Mail;

import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;


@Component
public class RabbitMQClient implements MessageBrokerClient {
	
	private final static String EXCHANGE_NAME = "coma.reply.message";
	
	private static final Pattern IPADDRESS4_PATTERN = 
			Pattern.compile("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Gson gson = new Gson();
	private final ConnectionFactory connectionFactory;
	private final Address[] brokerHosts;
	private Channel channel;

	public RabbitMQClient( @Value("${rabbitMQ.hosts}") String rabbitMqHosts) {
		String[] asArray = rabbitMqHosts.split(",");
		brokerHosts = new Address[asArray.length];
		for (int i=0; i<asArray.length; i++) {
			brokerHosts[i] = new Address(asArray[i]);
		}
		this.connectionFactory = new ConnectionFactory();
	}

	@Override
	public void messageSend(MessageProcessingContext messageProcessingContext, long adId) {
		logger.info("messageSend {} ", adId);
		try {
			
			if(isSellerMail(messageProcessingContext)
                    && senderAndReciverEmailDiver(messageProcessingContext)) {
				MessageSentEvent messageSentEvent = new MessageSentEvent.Builder().adId(adId)
																				  .text(messageProcessingContext.getMessage().getPlainTextBody())
																				  .senderEmailAddress(messageProcessingContext.getOriginalFrom().getAddress())
																				  .senderRole("SELLER")
																				  .mailerInfo(messageProcessingContext.getMessage().getHeaders().get("X-Mailer"))
																				  .assertedSenderIpAddress(findSenderIp(messageProcessingContext.getMail()))
																				  .get();
				
				logger.debug("messageSentEvent {} ", messageSentEvent);
				Channel channel = getChannel();			
				channel.basicPublish(EXCHANGE_NAME, "", null , gson.toJson(messageSentEvent).getBytes());
			}
		} catch (Exception e) {
			disconnect();
			logger.warn("Could not send event to broker.", e);
		}
	}

	private static boolean senderAndReciverEmailDiver(MessageProcessingContext messageProcessingContext){
		return !messageProcessingContext.getOriginalFrom().getAddress().equalsIgnoreCase(messageProcessingContext.getOriginalTo().getAddress());
	}
	
	private static boolean isSellerMail(MessageProcessingContext messageProcessingContext){
		return "SELLER_TO_BUYER".equals(messageProcessingContext.getMessageDirection().name());
	}
	//Received
	//X-Originating-IP:[xxx.xxx.xxx.xxx]
	//X-AOL-IP
	private String findSenderIp(Mail mail) throws MessagingException {
		if(mail.containsHeader("Received")){
			List<String> received = mail.getDecodedHeaders().get("Received");

			
			String ip = findSenderIp(received.get(received.size()-1));
			if (ip.equals("127.0.0.1") && received.size() > 1) {
				ip = findSenderIp(received.get(received.size()-2));
			}
			
			return ip;
		}
		
		if(mail.containsHeader("X-Originating-IP")){
			return findIPPattern(mail.getDecodedHeaders().get("X-Originating-IP").get(0));
		}
		
		if(mail.containsHeader("X-AOL-IP")){
			return findIPPattern(mail.getDecodedHeaders().get("X-AOL-IP").get(0));
		}
		return null;
	}
	
	private String findIPPattern(String ipString){

		Matcher matcher = IPADDRESS4_PATTERN.matcher(ipString);
		        if (matcher.find()) {
		            return matcher.group();
		        }
		        else{
		            return null;
		        }
	}
	private String findSenderIp(String firstReceived) throws MessagingException {
		
		logger.info("Received: {}", firstReceived);
		
		// from xxx[xxx.xxx.xxx] by yyy[yyy.yyy.yyy]
		
		int endOfFrom = firstReceived.indexOf(" by ");
		if (endOfFrom > 0) {
			String from = firstReceived.substring(0, endOfFrom);
			
			int startPos = from.lastIndexOf("[");
			if (startPos >= 0) {
				int endPos = from.indexOf("]", startPos);
				if (endPos >= 0) {
					String ip = from.substring(startPos+1, endPos);
					
					return ip.trim();
				}
			}
		}
		
		return "unknown";
		
	}
	
	private synchronized Channel getChannel() throws IOException {
		if (channel == null) {
			Connection connection = connectionFactory.newConnection(brokerHosts);
		    channel = connection.createChannel();
		    channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
		}
		
		return channel;
	}
	
	private synchronized void disconnect() {
		if (channel != null) {
			try {
				channel.close(AMQP.REPLY_SUCCESS, "Ok");
				channel.getConnection().close(AMQP.REPLY_SUCCESS, "Ok");
			} catch (Exception e) {
				logger.warn("Could not close connection.", e);
			}
			channel = null;
		}
	}

}
