package com.ecg.replyts.core.webapi.ssl;

import com.ecg.replyts.core.webapi.ThreadPoolBuilder;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.FileReader;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

public class SSLServerFactory {
    private static final String KEY_STORE_ALIAS = "replyts";
    private static final String DEFAULT_KEY_STORE_PASSWORD = "replyts";

    private final long httpTimeoutMs;
    private final long httpBlockingTimeoutMs;
    private final int soLingerTimeSeconds;
    private final long threadStopTimeoutMs;
    private final ThreadPoolBuilder threadPoolBuilder;
    private final SSLConfiguration sslConfig;
    private final int httpPortNumber;
    private final int httpMaxAcceptRequestQueueSize;

    public SSLServerFactory(int httpPortNumber, long httpTimeoutMs, ThreadPoolBuilder threadPoolBuilder, SSLConfiguration sslConfig,
                            int httpMaxAcceptRequestQueueSize, long httpBlockingTimeoutMs, long threadStopTimeoutMs, int soLingerTimeSeconds) {
        this.httpTimeoutMs = httpTimeoutMs;
        this.threadPoolBuilder = threadPoolBuilder;
        this.sslConfig = sslConfig;
        this.httpPortNumber = httpPortNumber;
        this.httpMaxAcceptRequestQueueSize = httpMaxAcceptRequestQueueSize;
        this.httpBlockingTimeoutMs = httpBlockingTimeoutMs;
        this.threadStopTimeoutMs = threadStopTimeoutMs;
        this.soLingerTimeSeconds = soLingerTimeSeconds;
    }

    public Server createServer() {
        try {
            Server server = new Server(threadPoolBuilder.build());
            final SslContextFactory sslContextFactory = createSSLContextFactory(sslConfig);
            final HttpConfiguration httpsConfig = createHttpsConfiguration(sslConfig);
            final ServerConnector serverConnector = createServerConnector(server, sslConfig, sslContextFactory, httpsConfig);
            if (sslConfig.isAllowHttp()) {
                final ServerConnector httpConnector = new ServerConnector(server);

                // Set blocking-timeout to 0 means to use the idle timeout,
                // see http://download.eclipse.org/jetty/9.3.3.v20150827/apidocs/org/eclipse/jetty/server/HttpConfiguration.html#setBlockingTimeout-long-
                httpsConfig.setBlockingTimeout(httpBlockingTimeoutMs);
                httpConnector.setIdleTimeout(httpTimeoutMs);

                //Number of connection requests that can be queued up before the operating system starts to send rejections.
                //https://wiki.eclipse.org/Jetty/Howto/Configure_Connectors
                httpConnector.setAcceptQueueSize(httpMaxAcceptRequestQueueSize);

                httpConnector.setPort(httpPortNumber);

                httpConnector.setStopTimeout(threadStopTimeoutMs);

                // use -1 to disable, positive values timeout, in seconds
                httpConnector.setSoLingerTime(soLingerTimeSeconds);

                server.setConnectors(new Connector[]{httpConnector, serverConnector});
                return server;
            } else {
                server.setConnectors(new Connector[]{serverConnector});
                return server;
            }
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }

    }

    private ServerConnector createServerConnector(Server server, SSLConfiguration sslConfig, SslContextFactory sslContextFactory, HttpConfiguration httpsConfig) {
        ServerConnector serverConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, SSLConfiguration.NEXT_PROTOCOL),
                new HttpConnectionFactory(httpsConfig));
        serverConnector.setPort(sslConfig.getHttpsPort());
        serverConnector.setIdleTimeout(sslConfig.getIdleTimeoutMs());
        return serverConnector;
    }

    private HttpConfiguration createHttpsConfiguration(SSLConfiguration sslConfig) {
        HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.setSecureScheme("https");
        httpsConfig.setSecurePort(sslConfig.getHttpsPort());
        httpsConfig.setOutputBufferSize(sslConfig.getBufferSize());
        httpsConfig.addCustomizer(new SecureRequestCustomizer());
        return httpsConfig;
    }

    private SslContextFactory createSSLContextFactory(SSLConfiguration sslConfig) throws InvalidKeySpecException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        if (sslConfig.getPemBasedSSLConfiguration().isPresent()) {
            return createSSLContextFactoryUsingPEM(sslConfig.getPemBasedSSLConfiguration().get());
        } else if (sslConfig.getKeystoreBasedSSLConfiguration().isPresent()) {
            return createSSLContextFactoryUsingJksFiles(sslConfig.getKeystoreBasedSSLConfiguration().get());
        } else {
            throw new IllegalStateException("Keystore or Pem based SSL config is necessary");
        }
    }

    private SslContextFactory createSSLContextFactoryUsingPEM(SSLConfiguration.PemBasedSSLConfiguration pemBasedSSLConfiguration) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        final char[] password = pemBasedSSLConfiguration.getPemPassword().toCharArray();
        final String keystorePassword = password.length > 0 ? new String(password) : DEFAULT_KEY_STORE_PASSWORD;

        final Object pemKeyObj = readPemFile(pemBasedSSLConfiguration.getPemKeyFile());
        final Object pemCrtObj = readPemFile(pemBasedSSLConfiguration.getPemCrtFile());

        final PrivateKey privateKey = extractPrivateKey(pemKeyObj, password);
        final X509Certificate certificate = extractX509Certificate(pemCrtObj);

        final KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, keystorePassword.toCharArray());
        keyStore.setKeyEntry(KEY_STORE_ALIAS, privateKey, keystorePassword.toCharArray(), new Certificate[]{certificate});

        final SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStore(keyStore);
        sslContextFactory.setKeyStorePassword(keystorePassword);
        sslContextFactory.setNeedClientAuth(sslConfig.isNeedClientAuth());

        return sslContextFactory;
    }

    private Object readPemFile(String pemKeyFile) throws IOException {
        try (FileReader fileReader = new FileReader(pemKeyFile)) {
            final PEMParser parser = new PEMParser(fileReader);
            return parser.readObject();
        }
    }

    private PrivateKey extractPrivateKey(Object pemKeyObj, char[] password) throws IOException {
        final PEMDecryptorProvider decryptionProv = new JcePEMDecryptorProviderBuilder().build(password);

        PrivateKeyInfo privateKeyInfo;
        if (pemKeyObj instanceof PEMEncryptedKeyPair) {
            privateKeyInfo = ((PEMEncryptedKeyPair) pemKeyObj).decryptKeyPair(decryptionProv).getPrivateKeyInfo();
        } else if (pemKeyObj instanceof PEMKeyPair) {
            privateKeyInfo = ((PEMKeyPair) pemKeyObj).getPrivateKeyInfo();
        } else if (pemKeyObj instanceof PrivateKeyInfo) {
            privateKeyInfo = ((PrivateKeyInfo) pemKeyObj);
        } else {
            throw new IllegalArgumentException("There is no suitable key in the PEM file");
        }

        final JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        return converter.getPrivateKey(privateKeyInfo);
    }

    private X509Certificate extractX509Certificate(Object pemCrtObj) throws CertificateException {
        X509CertificateHolder x509CertificateHolder;
        if (pemCrtObj instanceof X509CertificateHolder) {
            x509CertificateHolder = ((X509CertificateHolder) pemCrtObj);
        } else {
            throw new IllegalArgumentException("There is no suitable certificate in the PEM file");
        }

        return new JcaX509CertificateConverter().setProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())
                .getCertificate(x509CertificateHolder);
    }

    private SslContextFactory createSSLContextFactoryUsingJksFiles(SSLConfiguration.KeystoreBasedSSLConfiguration keystoreBasedSSLConfiguration) {
        SslContextFactory sslContextFactory = new SslContextFactory(keystoreBasedSSLConfiguration.getKeyStoreLocation());
        sslContextFactory.setKeyStorePassword(keystoreBasedSSLConfiguration.getKeyStorePassword());
        sslContextFactory.setKeyStoreType(keystoreBasedSSLConfiguration.getKeyStoreType());
        sslContextFactory.setTrustStorePath(keystoreBasedSSLConfiguration.getTrustStoreLocation());
        sslContextFactory.setTrustStorePassword(keystoreBasedSSLConfiguration.getTrustStorePassword());
        sslContextFactory.setTrustStoreType(keystoreBasedSSLConfiguration.getTrustStoreType());
        sslContextFactory.setNeedClientAuth(sslConfig.isNeedClientAuth());
        return sslContextFactory;
    }
}
