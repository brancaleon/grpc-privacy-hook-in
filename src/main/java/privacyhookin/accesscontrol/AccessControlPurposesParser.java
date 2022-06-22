package privacyhookin.accesscontrol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

}
