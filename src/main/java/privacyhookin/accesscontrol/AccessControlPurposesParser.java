package privacyhookin.accesscontrol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AccessControlPurposesParser {

    private final JsonNode purposesConfig;

    public AccessControlPurposesParser(String fileName){
        this.purposesConfig = this.parse(fileName);
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

    public byte[] getKeyBytes(String type, String clientId) {
        String keyPath = Paths.get(".").toAbsolutePath().normalize()
                        + String.format("/src/main/java/privacyhookin/accesscontrol/%s_%s.der", type, clientId);
        byte[] key;
        try {
            key = Files.readAllBytes(Paths.get(keyPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return key;
    }

    public boolean isAllowedPurpose(String purpose, String clientId){
        boolean allowed = true; // TODO: restore to false
        ArrayNode allowedServices = ((ObjectNode) this.purposesConfig)._children
                .get("purposes")
                .get(purpose)
                .get("allowed_services")._children;
        // TODO: check if he is in the list of allowed services linked to the purpose
        /*
        for (Node allowedService: allowedServices) {
            if(allowedService == clientId){
                allowed = true;
            }
        }
        */
        return allowed;
    }

}
