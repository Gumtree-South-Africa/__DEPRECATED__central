package com.ecg.messagecenter.bt.pushmessage;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

public class PushMessageTest {

  /**
   * @param args
   * @throws IOException 
   * @throws ClientProtocolException 
   */
  public static void main(String[] args) throws ClientProtocolException, IOException {
    DefaultHttpClient httpclient = new DefaultHttpClient();
    DefaultHttpClient httpclient2 = new DefaultHttpClient();
    HttpPost httpPost = new HttpPost("http://app.gumtree.sg/kpi/v1/notifications/notify-to-user");
    System.out.println("Requesting : " + httpPost.getURI());

    try {
      //Initial request without credentials returns "HTTP/1.1 401 Unauthorized"
      HttpResponse response = httpclient.execute(httpPost);
      System.out.println(response.getStatusLine());
      
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
              
              //Get current current "WWW-Authenticate" header from response
              // WWW-Authenticate:Digest realm="My Test Realm", qop="auth", 
              //nonce="cdcf6cbe6ee17ae0790ed399935997e8", opaque="ae40d7c8ca6a35af15460d352be5e71c"
                Header authHeader = response.getFirstHeader(AUTH.WWW_AUTH);
                System.out.println("authHeader = " + authHeader);
                
                DigestScheme digestScheme = new DigestScheme();
                
                //Parse realm, nonce sent by server. 
                digestScheme.processChallenge(authHeader);
                
                UsernamePasswordCredentials creds = new UsernamePasswordCredentials("kpi_api1", "12asDeA$");
                httpPost.addHeader(digestScheme.authenticate(creds, httpPost));
                String body = "<notification:notify-to-user xmlns:notification=\"http://www.ebayclassifiedsgroup.com/schema/notification/v1\">" +
                	"<notification:from-email>dummy@ebay.com</notification:from-email>" +
                	"<notification:to-email>kemo@ebay.com</notification:to-email>" + 
                	"<notification:message>I love Gumtree!</notification:message>"+ 
                	"<notification:meta>" +
                	"<notification:meta-key-value key=\"action\">MESSAGE</notification:meta-key-value>"+ 
                	"<notification:meta-key-value key=\"conversation-id\">xxxxxx</notification:meta-key-value>"+ 
                	"<notification:meta-key-value key=\"ad-id\">12345</notification:meta-key-value>"+ 
                	"</notification:meta>"+ 
                	"</notification:notify-to-user>";
                
                httpPost.setEntity(new StringEntity(body, ContentType.create("application/json", "UTF-8")));

                
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String responseBody = httpclient2.execute(httpPost, responseHandler);
                responseHandler.handleResponse(response);
          System.out.println("responseBody : " + responseBody);
            }
            
    } catch (MalformedChallengeException e) {
      e.printStackTrace();
    } catch (AuthenticationException e) {
      e.printStackTrace();
    } finally {
       httpclient.getConnectionManager().shutdown();
       httpclient2.getConnectionManager().shutdown();
    }

  }

}