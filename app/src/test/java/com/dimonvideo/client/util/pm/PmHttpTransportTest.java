package com.dimonvideo.client.util.pm;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** Uses a local socket to validate the production client's behavior after a sent request loses its response. */
public class PmHttpTransportTest {
    /** A server-side socket close after the request must never trigger a second destructive GET. */
    @Test
    public void lostResponseDoesNotReplayTheHttpRequest() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        AtomicReference<Throwable> serverFailure = new AtomicReference<>();
        try (ServerSocket listener = new ServerSocket(0)) {
            listener.setSoTimeout(1500);
            Thread server = new Thread(() -> {
                try {
                    try (Socket connection = listener.accept()) {
                        connection.setSoTimeout(1500);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(
                                connection.getInputStream(), StandardCharsets.US_ASCII));
                        String requestLine = reader.readLine();
                        if (requestLine != null && requestLine.startsWith("GET /?pm=10")) {
                            requests.incrementAndGet();
                        }
                        while (true) {
                            String line = reader.readLine();
                            if (line == null || line.isEmpty()) break;
                        }
                        // Apply an imaginary deletion and drop the connection before its response.
                    }
                    try (Socket unexpectedReplay = listener.accept()) {
                        requests.incrementAndGet();
                    } catch (SocketTimeoutException expected) {
                        // No second connection is the required outcome.
                    }
                } catch (Throwable failure) {
                    serverFailure.set(failure);
                }
            }, "pm-test-server");
            server.start();
            assertThrows(IOException.class, () -> PmHttpTransport.get(
                    "http://127.0.0.1:" + listener.getLocalPort() + "/?pm=10&delete=0",
                    1024, () -> { }));
            server.join(4000);
            assertTrue("Local server must finish", !server.isAlive());
            assertEquals(null, serverFailure.get());
            assertEquals(1, requests.get());
        }
    }

    /** Even a server's Retry-After:0 instruction cannot replay a non-idempotent PM request. */
    @Test
    public void serviceUnavailableFollowUpCannotSendAnotherRequest() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        AtomicReference<Throwable> serverFailure = new AtomicReference<>();
        try (ServerSocket listener = new ServerSocket(0)) {
            listener.setSoTimeout(1500);
            Thread server = new Thread(() -> {
                try {
                    try (Socket connection = listener.accept()) {
                        connection.setSoTimeout(1500);
                        readRequest(connection, requests);
                        connection.getOutputStream().write(("HTTP/1.1 503 Service Unavailable\r\n"
                                + "Retry-After: 0\r\nContent-Length: 0\r\nConnection: close\r\n\r\n")
                                .getBytes(StandardCharsets.US_ASCII));
                        connection.getOutputStream().flush();
                    }
                    try (Socket followUpConnection = listener.accept()) {
                        followUpConnection.setSoTimeout(1500);
                        readRequest(followUpConnection, requests);
                    } catch (SocketTimeoutException expected) {
                        // A speculative connection is harmless; a second HTTP request is forbidden.
                    }
                } catch (Throwable failure) {
                    serverFailure.set(failure);
                }
            }, "pm-test-503-server");
            server.start();
            assertThrows(IOException.class, () -> PmHttpTransport.get(
                    "http://127.0.0.1:" + listener.getLocalPort() + "/?pm=10&delete=0",
                    1024, () -> { }));
            server.join(4000);
            assertTrue("Local server must finish", !server.isAlive());
            assertEquals(null, serverFailure.get());
            assertEquals(1, requests.get());
        }
    }

    /** Counts complete local mutation requests and consumes their headers before returning a response. */
    private static void readRequest(Socket connection, AtomicInteger requests) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), StandardCharsets.US_ASCII));
        String requestLine = reader.readLine();
        if (requestLine == null) return;
        if (requestLine.startsWith("GET /?pm=10")) requests.incrementAndGet();
        while (true) {
            String line = reader.readLine();
            if (line == null || line.isEmpty()) return;
        }
    }
}
