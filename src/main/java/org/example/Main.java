package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
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
    byte[] nexusResponse = getNexusResponse(); //gets jar file from nexus and returns it as a byte[]

    try (JarInputStream jarStream = new JarInputStream(new ByteArrayInputStream(nexusResponse))) { //creates an InputStream (JarInputStream extends InputStream)
      var json = parseFile(jarStream);
      System.out.println(json.toPrettyString());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static ObjectNode parseFile(JarInputStream jarStream) {
    ObjectNode json = objectMapper.createObjectNode();
    JarEntry jarEntry;
    try {
      while ((jarEntry = jarStream.getNextJarEntry()) != null) {
        String[] path = jarEntry.getName().split("/");
        ObjectNode currentNode = json;
        for (String node : path) {
          if (!currentNode.has(node)) {
            if (node.endsWith(".json")) {
              currentNode.set(node, parseJson(jarStream));
            } else if (node.endsWith(".groovy")) {
              currentNode.set(node, parseGroovy(jarStream));
            }
            else {
              currentNode.set(node, objectMapper.createObjectNode());
            }
          }
          currentNode = (ObjectNode) currentNode.get(node);
        }
      }
      return json;
    } catch (IOException e) {
      throw new RuntimeException("failed to get next jar entry", e);
    }
  }

  private static String parseFileToString(JarInputStream jarStream) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = jarStream.read(buffer)) != -1) {
        baos.write(buffer, 0, bytesRead);
      }
    } catch (IOException e) {
      throw new RuntimeException("failed to parse file to string", e);
    }
    return baos.toString();
  }

  private static ObjectNode parseGroovy(JarInputStream jarStream) {
    HashMap<String, String> map = new HashMap<>();
    map.put("contents", parseFileToString(jarStream));
    return objectMapper.convertValue(map, ObjectNode.class);
  }

  private static ObjectNode parseJson(JarInputStream jarStream)  {
    try {
      return (ObjectNode) objectMapper.readTree(parseFileToString(jarStream));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("failed to parse json", e);
    }
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