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

    public String getPrivateKeyPEM() {
        String keyPath = Paths.get(".").toAbsolutePath().normalize()
                        + "/src/main/java/privacyhookin/accesscontrol/jwtRS512.key";
        String key;
        try {
            key = new String(Files.readAllBytes(Paths.get(keyPath)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return key;
    }

    public String getPublicKeyPEM(String clientId){
        String keyPath = Paths.get(".").toAbsolutePath().normalize()
                        + "/src/main/java/privacyhookin/accesscontrol/jwtRS512.key.pub";
        String key;
        try {
            key = new String(Files.readAllBytes(Paths.get(keyPath)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return key;
    }

}
