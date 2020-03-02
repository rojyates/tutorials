package com.baeldung.java.io;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.*;
import java.net.Socket;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertTrue;

public class SynchronousIOClientStaticWiremockUnitTest {
    private static final String REQUESTED_RESOURCE = "/test.txt";

    @ClassRule public static WireMockClassRule wireMockRule = new WireMockClassRule(WireMockConfiguration
      .options()
      .dynamicPort()
      .bindAddress("localhost"));

    @Rule public WireMockClassRule instanceRule = wireMockRule;

    @Before
    public void setup() {
        stubFor(get(urlEqualTo(REQUESTED_RESOURCE)).willReturn(aResponse()
          .withStatus(200)
          .withBody("{ \"response\" : \"It worked!\" }\r\n\r\n")));
    }

    @Test
    public void givenJavaIO_whenReadingAndWritingWithStreams_thenReadSuccessfully() throws IOException {
        // given an IO socket and somewhere to store our result
        Socket socket = new Socket("localhost", instanceRule.port());
        StringBuilder ourStore = new StringBuilder();

        // when we write and read (using try-with-resources so our resources are auto-closed)
        try (InputStream serverInput = socket.getInputStream();
          BufferedReader reader = new BufferedReader(new InputStreamReader(serverInput));
          OutputStream clientOutput = socket.getOutputStream();
          PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientOutput))) {
            writer.print("GET " + REQUESTED_RESOURCE + " HTTP/1.0\r\n\r\n");
            writer.flush(); // important - without this the request is never sent, and the test will hang on readLine()

            String line = reader.readLine();
            while (line != null) {
                ourStore.append(line);
                line = reader.readLine();
            }
        }

        // then we read and saved our data
        assertTrue(ourStore.toString().contains("It worked!"));
    }
}