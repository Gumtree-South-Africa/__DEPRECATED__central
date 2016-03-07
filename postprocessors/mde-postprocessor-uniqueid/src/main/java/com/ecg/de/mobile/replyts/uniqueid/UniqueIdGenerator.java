package com.ecg.de.mobile.replyts.uniqueid;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UniqueIdGenerator {

    private String pepper;

    public UniqueIdGenerator(String pepper) {
        this.pepper = pepper;
    }


    public String generateUniqueBuyerId(String emailAddress) {
        String generatedUniqueId = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(pepper.getBytes());
            byte[] bytes = md.digest(emailAddress.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            generatedUniqueId = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return generatedUniqueId;

    }

}
