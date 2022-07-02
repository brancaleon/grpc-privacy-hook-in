package delivery.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ServicesParser {

    private final JsonNode servicesConfig;

    public ServicesParser(String fileName) {
        this.servicesConfig = this.parse(fileName);
    }

    public JsonNode parse(String fileName) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String content = new String(Files.readAllBytes(Paths.get(fileName)));
            return mapper.readTree(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getHost(String clientId) {
        return servicesConfig.get(clientId).get("host").asText();
    }

    public Integer getPort(String clientId) {
        return servicesConfig.get(clientId).get("port").asInt();
    }


}
