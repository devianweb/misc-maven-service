package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class Main {

  private static final String II360_CONFIG_SERVICE_URL =
      "https://nexus.devl-int.ii.co.uk/nexus/service/local/repositories/snapshots/content/uk/co/ii/service/ii360-config-service/0.0.1-SNAPSHOT/ii360-config-service-0.0.1-20240531.131520-28-stubs.jar";

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static void main(String[] args) {
    byte[] nexusResponse = getNexusResponse();

    try (JarInputStream jarStream = new JarInputStream(new ByteArrayInputStream(nexusResponse))) {
      ObjectNode json = objectMapper.createObjectNode();
      parseFile(jarStream, json);
      System.out.println(json);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void parseFile(JarInputStream jarStream, ObjectNode json) {
    try {
      JarEntry jarEntry = jarStream.getNextJarEntry();

      while (jarEntry != null) {
        String[] path = jarEntry.getName().split("/");
        ObjectNode currentNode = json;

        for (String node : path) {
          if (!currentNode.has(node)) {
            if (node.endsWith(".json")) {
              currentNode.set(node, parseJson(jarStream));
            } else if (node.endsWith(".groovy")) {
              currentNode.set(node, parseGroovy(jarStream));
            } else {
              currentNode.set(node, objectMapper.createObjectNode());
            }
          }
          currentNode = (ObjectNode) currentNode.get(node);
        }

        jarEntry = jarStream.getNextJarEntry();
      }
    } catch (IOException e) {
      throw new RuntimeException("failed to parse file", e);
    }
  }

  private static ByteArrayOutputStream copyInputStream(JarInputStream jarStream) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int bytesRead;
    while ((bytesRead = jarStream.read(buffer)) != -1) {
      baos.write(buffer, 0, bytesRead);
    }
    return baos;
  }

  private static ObjectNode parseJson(JarInputStream jarStream) throws IOException {
    var copyStream = copyInputStream(jarStream);
    return (ObjectNode) objectMapper.readTree(copyStream.toString());
  }

  private static ObjectNode parseGroovy(JarInputStream jarStream) throws IOException {
    var copyStream = copyInputStream(jarStream);
    return objectMapper.createObjectNode().put("contents", copyStream.toString());
  }

  private static byte[] getNexusResponse() {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpGet httpGet = new HttpGet(II360_CONFIG_SERVICE_URL);
      var response = httpClient.execute(httpGet);
      try (InputStream responseInputStream = response.getEntity().getContent()) {
        return responseInputStream.readAllBytes();
      }
    } catch (IOException e) {
      throw new RuntimeException("failed to get jar file", e);
    }
  }
}