package com.baeldung.java.io;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertTrue;

public class AsynchronousIOClientUnitTest {
    private String REQUESTED_RESOURCE = "/test.json";

    @Rule public WireMockRule wireMockRule = new WireMockRule(wireMockConfig()
      .dynamicPort());

    @Before
    public void setup() {
        stubFor(get(urlEqualTo(REQUESTED_RESOURCE)).willReturn(aResponse()
          .withStatus(200)
          .withBody("{ \"response\" : \"It worked!\" }")));
    }

    @Test
    public void whenGetUsingNIO_thenUseBuffers() throws IOException {
        InetSocketAddress address = new InetSocketAddress("localhost", wireMockRule.port());
        SocketChannel socketChannel = SocketChannel.open(address);
        Charset charset = Charset.forName("UTF-8");
        socketChannel.write(charset.encode(CharBuffer.wrap("GET " + REQUESTED_RESOURCE + " HTTP/1.0\r\n\r\n")));

        ByteBuffer buffer = ByteBuffer.allocate(8192); // allocateDirect for direct memory access
        CharBuffer charBuffer = CharBuffer.allocate(8192);
        CharsetDecoder decoder = charset.newDecoder();
        StringBuilder ourStore = new StringBuilder();
        while (socketChannel.read(buffer) != -1 || buffer.position() > 0) {
            buffer.flip();
            storeBufferContents(buffer, charBuffer, decoder, ourStore);
            buffer.compact();
        }
        socketChannel.close();
        assertTrue(ourStore.length() > 0);
        assertTrue(ourStore.toString().contains("It worked!"));
    }

    @Test
    public void whenGetUsingNIO_thenUseSmallBuffers() throws IOException {
        InetSocketAddress address = new InetSocketAddress("localhost", wireMockRule.port());
        SocketChannel socket = SocketChannel.open(address);
        Charset charset = Charset.forName("UTF-8");
        socket.write(charset.encode(CharBuffer.wrap("GET " + REQUESTED_RESOURCE + " HTTP/1.0\r\n\r\n")));

        ByteBuffer buffer = ByteBuffer.allocate(8); // allocateDirect for direct memory access
        CharBuffer charBuffer = CharBuffer.allocate(8);
        CharsetDecoder decoder = charset.newDecoder();
        StringBuilder ourStore = new StringBuilder();
        while (socket.read(buffer) != -1 || buffer.position() > 0) {
            buffer.flip();
            storeBufferContents(buffer, charBuffer, decoder, ourStore);
            buffer.compact();
        }
        socket.close();
        assertTrue(ourStore.length() > 0);
        assertTrue(ourStore.toString().contains("It worked!"));
    }

    void storeBufferContents(ByteBuffer buffer, CharBuffer charBuffer, CharsetDecoder decoder, StringBuilder ourStore) {
        decoder.decode(buffer, charBuffer, true);
        charBuffer.flip();
        ourStore.append(charBuffer);
        charBuffer.clear();
    }

}