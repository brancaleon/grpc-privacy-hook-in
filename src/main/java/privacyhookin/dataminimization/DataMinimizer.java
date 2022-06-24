package privacyhookin.dataminimization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Descriptors;
import com.peng.gprc_hook_in.common.Position;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.function.UnaryOperator;

public class DataMinimizer {

    // TODO: add method for adding custom stuff
    // TODO: implement the standard operators
    HashMap<String, UnaryOperator<?>> functions = new HashMap<>();

    public DataMinimizer() {
        functions.put("erasure", (value)    -> null);
        // functions.put("noising", (value) -> noising(value));
    }

    public<ReqT> ReqT minimize(ReqT req){
        String configFile = Paths.get(".").toAbsolutePath().normalize()
                + "/src/main/java/privacyhookin/dataminimization/minimizations.json";
        JsonNode config = parseConfig(configFile);
        String objectType = req.getClass().getSimpleName();
        JsonNode requestObjectConfig = config.get(objectType);
        // req.getField()
        // Descriptors.FieldDescriptor fieldDescriptor = req.getDescriptorForType().findFieldByName("fieldXyz");
        // Object value = message.getField(fieldDescriptor);
        // TODO: Read config from file and apply functions to req object.
        //  Config can be JSON file and should map proto fields (as defined in the .proto files)
        //  to the data minimization functions to apply
        // fields can be accessed in this way: ((OrderRequest) req).meal_ or ((OrderRequest) req).getMeal()
        return req;
    }
    public JsonNode parseConfig(String fileName) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String content = new String(Files.readAllBytes(Paths.get(fileName)));
            return mapper.readTree(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object erasure(Object field){
        // TODO: Perform erasure of data field
        return field;
    }

    public static Object generalization(Object base, String field){
        // TODO: Perfom generalization of specified field
        //  Try to support different data types
        return base;
    }

    public static Object noising(Object base, String field){
        // TODO: Perform noising of the specified field
        //  Try to support different data types
        return base;
    }

    public static String hashing(String base, String field) {
        try{
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                final String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }

    public static Float positionToDistance(Position position1, Position position2, String field){
        // TODO: convert coordinates to distance
        return 1F;
    }
}
