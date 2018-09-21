package com.mailing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.search.FlagTerm;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

public class MailService {

    private Session session;
    private Store store;
    private Folder folder;

    public MailService() {

    }

    /**
     * to check if there is an active connection to the store
     * @return 
     */
    public boolean isLoggedIn() {
        return store.isConnected();
    }
    
    /**
     * to login to the mail host server
     * @param protocol
     * @param host
     * @param port
     * @param file
     * @param username
     * @param password
     * @throws java.lang.Exception
     */
    public void login(String protocol, String host, int port, String file, String username, String password) throws Exception {
	URLName url = new URLName(protocol, host, port, file, username, password);

	if (session == null) {
            Properties props = null;
            try {
		props = System.getProperties();
            } catch (SecurityException sex) {
                props = new Properties();
            }
            session = Session.getInstance(props, null);
	}
        
	store = session.getStore(url);
	store.connect();
	folder = store.getFolder(url);
	folder.open(Folder.READ_WRITE);
    }
        
    /**
     * to logout from the mail host server
     * @throws javax.mail.MessagingException
     */
    public void logout() throws MessagingException {
	folder.close(false);
	store.close();
	store = null;
	session = null;
    }

    /**
     * to get the message count
     * @param type [0 = All; 1 = New Messages; 2 = Unread Messages; 3 = Deleted Messages]
     * @return 
     * @throws java.lang.Exception 
     */
    public int getMessageCount(int type) throws Exception {
        int messageCount = 0;
        switch (type) {
            case 0:
                messageCount = folder.getMessageCount();
                break;
            case 1:
                // not supported in gmail (RECENT flag)
                messageCount = folder.getNewMessageCount();
                break;
            case 2:
                messageCount = folder.getUnreadMessageCount();
                break;
            case 3:
                messageCount = folder.getDeletedMessageCount();
                break;
            default:
                messageCount = -1;
        }        
        return messageCount;
    }
    
    /**
     * to get the message list
     * @param flag
     * @param set
     * @return
     * @throws MessagingException 
     */
    public Message[] getMessages(Flag flag, boolean set) throws MessagingException {
        Message messages[] = folder.search(new FlagTerm(new Flags(flag), set));
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.ENVELOPE);
        fp.add(FetchProfile.Item.CONTENT_INFO);
        folder.fetch(messages, fp);
        return messages;
    }

    /**
     * to print-screen a message
     * @param message
     * @throws java.lang.Exception
     */
    public static void printMessage(Message message) throws Exception {
        Address [] from, to;
        int messageId;
        String subject, receivedStringDate;
        Date receivedDate;
        DateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
            
        // getmessageId = message.getMessageNumber(); data
        messageId = message.getMessageNumber();
        from = message.getFrom();
        to = message.getRecipients(Message.RecipientType.TO);
        subject = message.getSubject();
        receivedDate = message.getReceivedDate();
        receivedStringDate = sdf.format(receivedDate);
        
        // print data
        System.out.println("####################");
        System.out.println("ID: " + messageId);
        System.out.println("DATE: " + receivedStringDate);
            
        if (from != null)
            for (Address a : from)
                System.out.println("FROM: " + a.toString());
        
        if (to != null)
            for (Address a : to)
                    System.out.println("TO: " + a.toString());
            
        System.out.println("SUBJECT: " + subject);
        
        // print attachment list
        if (message.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) message.getContent();
            for (int j=0; j < multipart.getCount(); j++) {
                BodyPart bodyPart = multipart.getBodyPart(j);
                String disp = bodyPart.getDisposition();
                if (disp == null || disp.equalsIgnoreCase(Part.ATTACHMENT)) {
                    continue;
                }
                System.out.println("File #" + j + ":" + bodyPart.getFileName());
            }
        }
    }
    
    /**
     * to download message's attachments
     * @param message
     * @throws Exception 
     */
    public static void downloadAttachments(Message message) throws Exception {
        int messageId = message.getMessageNumber();
        String folder = String.valueOf(messageId);
        String dir = "../" + folder + "/"; // hardcoded dir
        
        // create folder 
        boolean success = (new File(dir)).mkdir();
        if (success)
            // create files
            if (message.isMimeType("multipart/*")) {
                Multipart multipart = (Multipart) message.getContent();
                for (int j=0; j < multipart.getCount(); j++) {
                    BodyPart bodyPart = multipart.getBodyPart(j);
                    String disp = bodyPart.getDisposition();
                    if (disp == null || disp.equalsIgnoreCase(Part.ATTACHMENT)) {
                        continue;
                    }
                    InputStream is = bodyPart.getInputStream();
                    File f = new File(dir + bodyPart.getFileName());
                    try (FileOutputStream fos = new FileOutputStream(f)) {
                        byte[] buf = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buf)) != -1)
                            fos.write(buf, 0, bytesRead);
                    }
                }
            }
    }
}