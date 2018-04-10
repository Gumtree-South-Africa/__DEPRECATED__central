package com.ecg.comaas.mde.postprocessor.uniqueid;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class UniqueIdGenerator {

    private String pepper;

    UniqueIdGenerator(String pepper) {
        this.pepper = pepper;
    }

    String generateUniqueBuyerId(String emailAddress) {
        String generatedUniqueId = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(pepper.getBytes());
            byte[] bytes = md.digest(emailAddress.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            generatedUniqueId = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return generatedUniqueId;
    }
}
