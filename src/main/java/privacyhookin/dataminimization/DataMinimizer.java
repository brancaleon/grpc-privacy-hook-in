package privacyhookin.dataminimization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DataMinimizer {
    private final HashMap<String, MinimizationFunction> functions = new HashMap<>();
    private JsonNode config = null;

    public DataMinimizer(String configPath) {
        functions.put("erasure", new MinimizationFunction().addOperator((value, config) -> null));
        functions.put("replace", new MinimizationFunction()
                .addStringOperator((value, config) -> config.getOrDefault("replace", ""))
                .addIntOperator((value, config) -> Integer.parseInt(config.getOrDefault("replace", "0")))
                .addLongOperator((value, config) -> Long.parseLong(config.getOrDefault("replace", "0")))
                .addBooleanOperator((value, config) -> config.getOrDefault("replace", "false").equals("true"))
        );
        parseConfig(configPath);
    }

    public void defineMinimizationFunction(String name, MinimizationFunction function) {
        functions.put(name, function);
    }

    public <MessageT extends Message> MessageT minimize(MessageT req, String purpose) {
        String objectType = req.getClass().getSimpleName();
        JsonNode purposeConfig = config.at("/purposes/" + purpose + "/minimization/" + objectType);
        if (purposeConfig == null) {
            return req;
        }
        MessageT.Builder builder = req.toBuilder();
        Iterator<Map.Entry<String, JsonNode>> fieldIterator = purposeConfig.fields();
        while (fieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> field = fieldIterator.next();
            Descriptors.FieldDescriptor fieldDescriptor = req.getDescriptorForType().findFieldByName(field.getKey());
            Object value = req.getField(fieldDescriptor);
            Iterator<JsonNode> operationIterator = field.getValue().elements();
            while (operationIterator.hasNext()) {
                JsonNode operation = operationIterator.next();
                MinimizationFunction function = functions.getOrDefault(operation.get("function").asText(), null);
                if (function != null) {
                    Map<String, String> operationConfig = new HashMap<>();
                    Iterator<Map.Entry<String, JsonNode>> configIterator = operation.fields();
                    while (configIterator.hasNext()) {
                        Map.Entry<String, JsonNode> config = configIterator.next();
                        operationConfig.put(config.getKey(), config.getValue().asText());
                    }
                    value = function.apply(fieldDescriptor.getJavaType(), value, operationConfig);
                }
            }
            builder.setField(fieldDescriptor, value);
        }
        return (MessageT) builder.build();
    }

    public void parseConfig(String fileName) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String content = new String(Files.readAllBytes(Paths.get(fileName)));
            config = mapper.readTree(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Object generalization(Object base, String field) {
        // TODO: Perfom generalization of specified field
        //  Try to support different data types
        return base;
    }

    public static Object noising(Object base, String field) {
        // TODO: Perform noising of the specified field
        //  Try to support different data types
        return base;
    }

    public static String hashing(String base, String field) {
        try {
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
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

//    public static Float positionToDistance(Position position1, Position position2, String field){
//        // TODO: convert coordinates to distance
//        return 1F;
//    }
}
