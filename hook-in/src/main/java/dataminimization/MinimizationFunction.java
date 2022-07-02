package dataminimization;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Enum;
import com.google.protobuf.Message;

import java.util.HashMap;
import java.util.Map;

public class MinimizationFunction {

    private final Map<JavaType, ConfigurableOperator<?>> operators = new HashMap<>();

    public MinimizationFunction addIntOperator(ConfigurableOperator<Integer> operator) {
        operators.put(JavaType.INT, operator);
        return this;
    }

    public MinimizationFunction addLongOperator(ConfigurableOperator<Long> operator) {
        operators.put(JavaType.LONG, operator);
        return this;
    }

    public MinimizationFunction addFloatOperator(ConfigurableOperator<Float> operator) {
        operators.put(JavaType.FLOAT, operator);
        return this;
    }

    public MinimizationFunction addDoubleOperator(ConfigurableOperator<Double> operator) {
        operators.put(JavaType.DOUBLE, operator);
        return this;
    }

    public MinimizationFunction addBooleanOperator(ConfigurableOperator<Boolean> operator) {
        operators.put(JavaType.BOOLEAN, operator);
        return this;
    }

    public MinimizationFunction addStringOperator(ConfigurableOperator<String> operator) {
        operators.put(JavaType.STRING, operator);
        return this;
    }

    public MinimizationFunction addByteStringOperator(ConfigurableOperator<ByteString> operator) {
        operators.put(JavaType.BYTE_STRING, operator);
        return this;
    }

    public MinimizationFunction addEnumOperator(ConfigurableOperator<Enum> operator) {
        operators.put(JavaType.ENUM, operator);
        return this;
    }

    public MinimizationFunction addMessageOperator(ConfigurableOperator<Message> operator) {
        operators.put(JavaType.MESSAGE, operator);
        return this;
    }

    public MinimizationFunction addOperator(ConfigurableOperator<Object> operator) {
        return addOperator(operator, JavaType.values());
    }

    public MinimizationFunction addOperator(ConfigurableOperator<Object> operator, JavaType[] allowedTypes) {
        for (JavaType allowedType : allowedTypes) {
            operators.put(allowedType, operator);
        }
        return this;
    }

    public <T> T apply(JavaType type, Object value, Map<String, String> config) {
        if (!operators.containsKey(type)) {
            System.out.println("WARNING: Function can not be applied to " + type + " -> Field will be erased!");
            return null;
        }
        return ((ConfigurableOperator<T>) operators.get(type)).apply((T) value, config);
    }

}
