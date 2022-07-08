package dataminimization;

import accesscontrol.ConfigParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        functions.put("hashing", new MinimizationFunction()
                .addStringOperator((value, config) -> {
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
        functions.put("***", new MinimizationFunction()
                .addStringOperator((value, config) -> {
                    int hiddenPrefix = value.length() - Integer.parseInt(config.getOrDefault("readableEnd", "0"));
                    return "*".repeat(hiddenPrefix) + value.substring(hiddenPrefix);
                })
        );
    }

    void defineMinimizationFunction(String name, MinimizationFunction function) {
        functions.put(name, function);
    }

    public <MessageT extends Message> MessageT minimize(MessageT message, String purpose) {
        String objectType = message.getClass().getSimpleName();
        JsonNode purposeConfig = config.at("/purposes/" + purpose + "/minimization/" + objectType);
        if (purposeConfig == null) {
            return message;
        }
        MessageT.Builder builder = message.toBuilder();
        Iterator<Map.Entry<String, JsonNode>> fieldIterator = purposeConfig.fields();
        while (fieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> field = fieldIterator.next();
            Descriptors.FieldDescriptor fieldDescriptor = message.getDescriptorForType().findFieldByName(field.getKey());
            if (fieldDescriptor.isRepeated()) {
                List<Object> values = IntStream.range(0, message.getRepeatedFieldCount(fieldDescriptor))
                        .mapToObj(i -> message.getRepeatedField(fieldDescriptor, i))
                        .map(value -> applyOperation(fieldDescriptor, value, field.getValue().elements()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                builder.clearField(fieldDescriptor);
                values.forEach(value -> builder.addRepeatedField(fieldDescriptor, value));
            } else {
                Object value = message.getField(fieldDescriptor);
                value = applyOperation(fieldDescriptor, value, field.getValue().elements());
                if (value != null) {
                    builder.setField(fieldDescriptor, value);
                } else {
                    builder.clearField(fieldDescriptor);
                }
            }
        }
        return (MessageT) builder.build();
    }

    private Object applyOperation(Descriptors.FieldDescriptor fieldDescriptor, Object value, Iterator<JsonNode> operationIterator) {
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
        return value;
    }
}
