package dataminimization;

import accesscontrol.ConfigParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class DataMinimizer {
    private final HashMap<String, MinimizationFunction> functions = new HashMap<>();
    private final JsonNode config;

    public DataMinimizer(ConfigParser configParser) {
        config = configParser.getJson();
        functions.put("erasure", new MinimizationFunction().addOperator((value, config) -> null));
        functions.put("replace", new MinimizationFunction()
                .addStringOperator((value, config) -> config.getOrDefault("replace", ""))
                .addIntOperator((value, config) -> Integer.parseInt(config.getOrDefault("replace", "0")))
                .addLongOperator((value, config) -> Long.parseLong(config.getOrDefault("replace", "0")))
                .addBooleanOperator((value, config) -> config.getOrDefault("replace", "false").equals("true"))
        );
        functions.put("generalize", new MinimizationFunction()
                .addIntOperator((value, config) -> {
                    int offset = Integer.parseInt(config.getOrDefault("offset", "0"));
                    int binSize = Integer.parseInt(config.getOrDefault("binSize", "10"));
                    int representerOffset = Integer.parseInt(config.getOrDefault("representerOffset", "0"));
                    return value - ((value - offset) % binSize) + representerOffset;
                })
                .addLongOperator((value, config) -> {
                    long offset = Long.parseLong(config.getOrDefault("offset", "0"));
                    long binSize = Long.parseLong(config.getOrDefault("binSize", "10"));
                    long representerOffset = Long.parseLong(config.getOrDefault("representerOffset", "0"));
                    return value - ((value - offset) % binSize) + representerOffset;
                })
                .addDoubleOperator((value, config) -> {
                    double offset = Double.parseDouble(config.getOrDefault("offset", "0.0"));
                    double binSize = Double.parseDouble(config.getOrDefault("binSize", "10.0"));
                    double representerOffset = Double.parseDouble(config.getOrDefault("representerOffset", "0.0"));
                    return value - ((value - offset) % binSize) + representerOffset;
                }));
        functions.put("gaussianNoise", new MinimizationFunction()
                .addDoubleOperator((value, config) -> {
                    double variance = Double.parseDouble(config.getOrDefault("variance", "1.0"));
                    return value + new Random().nextGaussian() * Math.sqrt(variance);
                }));
        functions.put("hashing", new MinimizationFunction().addStringOperator((value, config) -> {
            try {
                final MessageDigest digest = MessageDigest.getInstance("SHA-256");
                final byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
                final StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    final String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1)
                        hexString.append('0');
                    hexString.append(hex);
                }
                return hexString.toString();
            } catch (Exception ex) {
                return "error";
            }
        }));
    }

    void defineMinimizationFunction(String name, MinimizationFunction function) {
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
            if (value != null) {
                builder.setField(fieldDescriptor, value);
            } else {
                builder.clearField(fieldDescriptor);
            }
        }
        return (MessageT) builder.build();
    }
}
