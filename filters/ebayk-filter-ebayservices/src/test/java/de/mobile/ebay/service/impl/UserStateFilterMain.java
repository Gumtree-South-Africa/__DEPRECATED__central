package de.mobile.ebay.service.impl;

import de.mobile.ebay.service.ServiceException;
import org.apache.http.impl.client.DefaultHttpClient;

public class UserStateFilterMain {
    // test class for accessing the userStateFilter service
    // to run this in the IntelliJ, include the following dependency in the pom
    /*
            <groupId>de.mobile.common</groupId>
            <artifactId>mobile-soap-simple</artifactId>
            <version>1.1</version>
     */

    public static void main(String[] args) throws ServiceException {
        UserServiceImpl svc = new UserServiceImpl(new DefaultHttpClient(), new Config() {
            public String appName() {
                return null;
            }

            public String iafToken() {
                return ""; // e.g. from properties
            }

            public String endpointUrl() {
                return "https://svcs.ebay.com/ws/spf";
            }

            public String proxyUrl() {
                return "";
            }
        });

        System.out.println(svc.getMemberBadgeData("USER_EMAIL_HERE"));
    }

}
