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

    public boolean isClientRejectedForPurpose(String purpose, String clientId) {
        return isRejectedForPurpose(purpose, clientId, "allowed_clients");
    }

    public boolean isMethodRejectedForPurpose(String purpose, String methodName) {
        return isRejectedForPurpose(purpose, methodName, "allowed_methods");
    }

    private boolean isRejectedForPurpose(String purpose, String givenValue, String jsonKeyword) {
        // TODO: caching json file
        // TODO: nullPointerException when purpose name is not found
        ArrayNode allowedValues = (ArrayNode) purposesConfig.get("purposes").get(purpose).get(jsonKeyword);
        if (allowedValues.isArray()) {
            for (JsonNode allowedValue : allowedValues) {
                if (givenValue.equals(allowedValue.asText())) {
                    return false;
                }
            }
        }
        return true;
    }

}
