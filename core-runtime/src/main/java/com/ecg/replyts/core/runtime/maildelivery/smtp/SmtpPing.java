package com.ecg.replyts.core.runtime.maildelivery.smtp;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;


public class SmtpPing {

    /**
     * QUIT command for SMTP Server with SMTP conform line terminators.
     */
    private static final String SMTP_QUIT = "QUIT\r\n";

    /**
     * "Exception Pointer": if an IO Err occurs inside the {@link PingRunnable}, it will not be able to throw that one
     * directly. Instead of that, the runnable will get a reference to an {@link ExceptionHolder}, that it can give it's
     * exception and it will keep it for the surrounding ping
     *
     * @author huttar
     */
    private class ExceptionHolder {

        private Exception exception;

        public void passOn() throws Exception {
            if (exception != null) {
                throw exception;
            }
        }
    }

    /**
     * Runnable that pings a SMTP Server ( a socket is passed to it) and performs a handshake. It it times out, the
     * socket, that it get's is closed to terminate that communication.
     *
     * @author huttar
     */
    private class PingRunnable implements Runnable {
        private Socket sock;
        private ExceptionHolder exceptionHolder;

        public PingRunnable(Socket sock, ExceptionHolder h) {
            this.sock = sock;
            this.exceptionHolder = h;
        }

        @Override
        public void run() {

            BufferedReader in = null;
            OutputStreamWriter out = null;

            try {
                in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                out = new OutputStreamWriter(sock.getOutputStream());

                // Read the Welcome from Server
                in.readLine();
                // QUIT!
                out.write(SMTP_QUIT);
                out.flush();
                // Read the Bye from server
                in.readLine();

            } catch (Exception t) {
                exceptionHolder.exception = t;
            } finally {
                closeSilently(in);
                closeSilently(out);
                closeSilently(sock);
            }
        }

    }

    public static void main(String[] args) throws Exception {
        new SmtpPing().ping("localhost", 25, 100);
    }

    /**
     * Pings an SMTP Server by connecting to it and sending a QUIT. It will expect the SMTP server to behave friendly
     * and send a greeting and goodbye message
     *
     * @param hostName host name of SMTP Server to connect to
     * @param port     port of server
     * @param timeout  socket timeout in milliseconds for the total communication
     * @throws Exception when anything goes wrong with communication, the error that occured will be passed on.
     */
    public void ping(final String hostName, final int port, final int timeout) throws Exception {
        final ExceptionHolder exceptionHolder = new ExceptionHolder();
        final Socket sock = new Socket(hostName, port);

        try {
            Thread pingThread = new Thread(new PingRunnable(sock, exceptionHolder));
            pingThread.start();
            pingThread.join(timeout);
            exceptionHolder.passOn();
        } finally {
            if (!sock.isClosed()) {
                closeSilently(sock);
                throw new IOException("Socket Timeout with SMTP Server on " + hostName + ":" + port + ". Timeout was " + timeout);
            }

        }

    }

    private void closeSilently(Closeable c) {
        try {
            c.close();
        } catch (Exception ex) { // NOSONAR

        }
    }

    private void closeSilently(Socket sock) {
        try {
            sock.close();
        } catch (Exception ex) { // NOSONAR
        }
    }

}
