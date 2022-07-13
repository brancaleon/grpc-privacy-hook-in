package accesscontrol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ConfigParser {

    private JsonNode purposesConfig;
    private Map<String, List<String>> allowedClients = new HashMap<>();
    private Map<String, List<String>> allowedMethods = new HashMap<>();

    public ConfigParser(String fileName) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String content = new String(Files.readAllBytes(Paths.get(fileName)));
            purposesConfig = mapper.readTree(content);
            Iterator<Map.Entry<String, JsonNode>> purposes = purposesConfig.at("/purposes").fields();
            while (purposes.hasNext()) {
                Map.Entry<String, JsonNode> purpose = purposes.next();
                allowedClients.put(purpose.getKey(), new LinkedList<>());
                JsonNode elements = purpose.getValue().at("/allowed_clients");
                if (elements != null) {
                    Iterator<JsonNode> clientStrings = elements.elements();
                    while (clientStrings.hasNext()) {
                        allowedClients.get(purpose.getKey()).add(clientStrings.next().asText());
                    }
                }
                allowedMethods.put(purpose.getKey(), new LinkedList<>());
                elements = purpose.getValue().at("/allowed_methods");
                if (elements != null) {
                    Iterator<JsonNode> methodStrings = elements.elements();
                    while (methodStrings.hasNext()) {
                        allowedMethods.get(purpose.getKey()).add(methodStrings.next().asText());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JsonNode getJson() {
        return purposesConfig;
    }

    public String getKeyServerHost() {
        return purposesConfig.at("/key_server/host").asText();
    }

    public int getKeyServerPort() {
        return purposesConfig.at("/key_server/port").asInt();
    }

    public boolean isClientRejectedForPurpose(String purpose, String clientId) {
        return !allowedClients.getOrDefault(purpose, new LinkedList<>()).contains(clientId);
    }

    public boolean isMethodRejectedForPurpose(String purpose, String methodName) {
        return !allowedMethods.getOrDefault(purpose, new LinkedList<>()).contains(methodName);
    }

}
