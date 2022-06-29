package privacyhookin.accesscontrol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.peng.gprc_hook_in.keyserver.KeyServerServiceGrpc;
import com.peng.gprc_hook_in.keyserver.PublicKeyRequest;
import com.peng.gprc_hook_in.keyserver.PublicKeyResponse;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import utils.ServicesParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AccessControlPurposesParser {

    private final JsonNode purposesConfig;
    private static ServicesParser servicesParser;

    public AccessControlPurposesParser(String fileName){
        String configFile = Paths.get(".").toAbsolutePath().normalize() +
                "/src/main/java/privacyhookin/accesscontrol/services.json";
        servicesParser = new ServicesParser(configFile);
        purposesConfig = this.parse(fileName);
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
        if (type.equals("private_key")) {
            String keyPath = Paths.get(".").toAbsolutePath().normalize()
                    + String.format("/src/main/java/privacyhookin/accesscontrol/keyserver/keys/%s_%s.der", type, clientId);
            byte[] key;
            try {
                key = Files.readAllBytes(Paths.get(keyPath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return key;
        } else if (type.equals("public_key")) {
            PublicKeyRequest publicKeyRequest = PublicKeyRequest.newBuilder()
                    .setClient(clientId).build();
            Channel channel = ManagedChannelBuilder
                    .forAddress(servicesParser.getHost("keyserver"),
                                servicesParser.getPort("keyserver"))
                    .usePlaintext().build();
            KeyServerServiceGrpc.KeyServerServiceBlockingStub keyserverStub = KeyServerServiceGrpc
                    .newBlockingStub(channel);
            PublicKeyResponse response = keyserverStub.getPublicKey(publicKeyRequest);
            return response.getKey().toByteArray();
        }
        return null;
    }

    public boolean isAllowedPurpose(String purpose, String clientId){
        // check if he is in the list of allowed services linked to the purpose
        boolean allowed = false;
        ArrayNode allowedServices = (ArrayNode) this.purposesConfig.get("purposes").get(purpose).get("allowed_services");
        if (allowedServices.isArray()) {
            for (JsonNode allowedService : allowedServices) {
                String allowedServiceName = allowedService.asText();
                if (clientId.equals(allowedServiceName)) {
                    allowed = true;
                    break;
                }
            }
        }
        return allowed;
    }

}
