package dataminimization;

import java.util.Map;
import java.util.function.BiFunction;

public interface ConfigurableOperator<T> extends BiFunction<T, Map<String, String>, T> {

}

