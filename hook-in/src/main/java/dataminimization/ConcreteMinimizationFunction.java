package dataminimization;

import com.google.protobuf.Descriptors;

import java.util.Map;

public class ConcreteMinimizationFunction {

    private final MinimizationFunction minimizationFunction;
    private final Map<String, String> parameters;

    public ConcreteMinimizationFunction(MinimizationFunction minimizationFunction, Map<String, String> parameters) {
        this.minimizationFunction = minimizationFunction;
        this.parameters = parameters;
    }

    public <T> T apply(Descriptors.FieldDescriptor.JavaType type, Object value) {
        return minimizationFunction.apply(type, value, parameters);
    }
}
