package com.baeldung.java.io;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.*;
import java.net.Socket;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertTrue;

public class SynchronousIOClientUnitTest {
    private static final String REQUESTED_RESOURCE = "/test.txt";

    @Rule public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Before
    public void setup() {
        stubFor(get(urlEqualTo(REQUESTED_RESOURCE)).willReturn(aResponse()
          .withStatus(200)
          .withBody("{ \"response\" : \"It worked!\" }\r\n\r\n")));
    }

    @Test
    public void whenGetUsingIO_thenUseStreams() throws IOException {
        Socket socket = new Socket("localhost", wireMockRule.port());
        try (InputStream serverInput = socket.getInputStream();
          BufferedReader reader = new BufferedReader(new InputStreamReader(serverInput));
          OutputStream clientOutput = socket.getOutputStream();
          PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientOutput))) {
            writer.print("GET " + REQUESTED_RESOURCE + " HTTP/1.0\r\n\r\n");
            writer.flush(); // important - without this the request is never sent, and the test will hang on readLine()

            StringBuilder ourStore = new StringBuilder();

            for (String line; (line = reader.readLine()) != null;) {
                ourStore.append(line);
            }
            System.out.println("Our store has length: " + ourStore.length());
            System.out.println("Our store contains: '" + ourStore.toString() + "'");

            assertTrue(ourStore.length() > 0);
            assertTrue(ourStore.toString().contains("It worked!"));
        }
    }
}