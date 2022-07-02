package accesscontrol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigParser {

    private JsonNode purposesConfig;

    public ConfigParser(String fileName) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String content = new String(Files.readAllBytes(Paths.get(fileName)));
            purposesConfig = mapper.readTree(content);
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

    public boolean isAllowedPurpose(String purpose, String clientId) {
        // check if he is in the list of allowed services linked to the purpose
        boolean allowed = false;
        ArrayNode allowedClients = (ArrayNode) this.purposesConfig.get("purposes").get(purpose).get("allowed_clients");
        if (allowedClients.isArray()) {
            for (JsonNode allowedClient : allowedClients) {
                String allowedServiceName = allowedClient.asText();
                if (clientId.equals(allowedServiceName)) {
                    allowed = true;
                    break;
                }
            }
        }
        return allowed;
    }

}
