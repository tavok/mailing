package com.mailing;

import javax.mail.Flags.Flag;
import javax.mail.Message;

public class Main {
    
    public static void main(String [] args) {
        int unreadMessages;
        MailService mail = new MailService();
        Message [] messages;
        
        try {
            // Connect to the mail host server
            mail.login("imaps", "imap.gmail.com", 993, "INBOX","username@gmail.com", "password");
            
            // Get unread message count in folder 
            unreadMessages = mail.getMessageCount(2);
            System.out.println("Unread messages: " + unreadMessages);
            
            // Get unread messages in folder
            messages = mail.getMessages(Flag.SEEN, false);
            
            // Print-screen unread messages & download attachments
            for (Message m : messages) {
                MailService.printMessage(m);
                MailService.downloadAttachments(m);
            }
            
            // Disconnect from mail host server
            mail.logout();
        
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}
