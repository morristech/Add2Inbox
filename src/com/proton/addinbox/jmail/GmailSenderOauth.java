package com.proton.addinbox.jmail;

import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import android.util.Log;

import com.sun.mail.smtp.SMTPTransport;
import com.sun.mail.util.BASE64EncoderStream;

public class GmailSenderOauth {
	private Session session;

	/** Note: This does NOT work on Android 2.3. This implementation is limited to 4.0+**/
	
	public SMTPTransport connectToSmtp(String host, int port, String userEmail,
			String oauthToken, boolean debug) throws Exception {
		try {
			Properties props = new Properties();
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.starttls.required", "true");
			props.put("mail.smtp.sasl.enable", "false");
			session = Session.getInstance(props);
			session.setDebug(debug);

			final URLName unusedUrlName = null;
			SMTPTransport transport = new SMTPTransport(session, unusedUrlName);

			// If the password is non-null, SMTP tries to do AUTH LOGIN.
			final String emptyPassword = null;
			transport.connect(host, port, userEmail, emptyPassword);

			byte[] response = String.format("user=%s\1auth=Bearer %s\1\1",
					userEmail, oauthToken).getBytes();
			response = BASE64EncoderStream.encode(response);

			transport.issueCommand("AUTH XOAUTH2 " + new String(response), 235);

			return transport;
		} catch (Exception ex) {
			return null;
		}
	}

	/*********@deprecated: Not currently working. Still investigating.************/
	public synchronized void sendMailWithAttachment(String subject,
			String body, String user, String oauthToken, String recipients,
			Object data) {
		try {
			Byte[] attachmentData = (Byte[]) data;
			Multipart mp = new MimeMultipart();

			SMTPTransport smtpTransport = connectToSmtp("smtp.gmail.com", 587,
					user, oauthToken, true);
			MimeMessage message = new MimeMessage(session);
			message.setSender(new InternetAddress(user));
			message.setSubject(subject);

			MimeBodyPart htmlPart = new MimeBodyPart();
			htmlPart.setContent(body.getBytes(), "text/html");
			mp.addBodyPart(htmlPart);

			MimeBodyPart attachment = new MimeBodyPart();
			attachment.setFileName("attachment.jpg");
			attachment.setContent(attachmentData, "application/x-any");
			mp.addBodyPart(attachment);

			message.setContent(mp);

			message.setRecipient(Message.RecipientType.TO, new InternetAddress(
					recipients));
			smtpTransport.sendMessage(message, message.getAllRecipients());

		} catch (Exception e) {
		}
	}

	public synchronized void sendMail(String subject, String body, String user,
			String oauthToken, String recipients) {
		try {
			SMTPTransport smtpTransport = connectToSmtp("smtp.gmail.com", 587,
					user, oauthToken, true);

			MimeMessage message = new MimeMessage(session);
			DataHandler handler = new DataHandler(new ByteArrayDataSource(
					body.getBytes(), "text/plain"));

			message.setSender(new InternetAddress(user));
			message.setSubject(subject);
			message.setDataHandler(handler);

			/*
			 * if (recipients.indexOf(',') > 0)
			 * message.setRecipients(Message.RecipientType
			 * .TO,InternetAddress.parse(recipients)); //Prefab code, commented
			 * out for irrelevance .
			 */

			message.setRecipient(Message.RecipientType.TO, new InternetAddress(
					recipients));
			
			smtpTransport.sendMessage(
					message, 
					message.getAllRecipients());
		} catch (Exception e) {
			e.printStackTrace(); 
		}
	}

}
