package dataminimization;

import accesscontrol.Authorization;
import accesscontrol.ConfigParser;
import com.google.protobuf.Message;
import io.grpc.*;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;

public class DataMinimizerInterceptor implements ServerInterceptor {

    private final DataMinimizer minimizer;
    private final String keyServerHost;
    private final int keyServerPort;

    protected DataMinimizerInterceptor(DataMinimizer minimizer, String keyServerHost, int keyServerPort) {
        this.minimizer = minimizer;
        this.keyServerHost = keyServerHost;
        this.keyServerPort = keyServerPort;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall,
            Metadata metadata,
            ServerCallHandler<ReqT, RespT> serverCallHandler) {
        Authorization authorization = new Authorization(metadata, keyServerHost, keyServerPort);
        if (authorization.getPurposeOrNull() == null) {
            serverCall.close(authorization.getErrorStatus(), new Metadata());
            return new ServerCall.Listener<ReqT>() {
                // noop
            };
        }
        ServerCall.Listener<ReqT> listener = serverCallHandler.startCall(new ForwardingServerCall.SimpleForwardingServerCall<>(serverCall) {
            @Override
            public void sendMessage(RespT message) {
                if (message instanceof Message) {
                    message = (RespT) minimizer.minimize((Message) message, authorization.getPurposeOrNull());
                }
                super.sendMessage(message);
            }
        }, metadata);
        return new SimpleForwardingServerCallListener<>(listener) {
            @Override
            public void onMessage(ReqT req) {
                if (req instanceof Message) {
                    req = (ReqT) minimizer.minimize((Message) req, authorization.getPurposeOrNull());
                }
                super.onMessage(req);
            }
        };
    }

    public static DataMinimizerInterceptor.Builder newBuilder(String configPath) {
        return new Builder(configPath);
    }


    public static class Builder {

        private DataMinimizer minimizer;
        private ConfigParser config;

        private Builder(String configPath) {
            config = new ConfigParser(configPath);
            minimizer = new DataMinimizer();
        }

        public DataMinimizerInterceptor.Builder defineMinimizationFunction(String name, MinimizationFunction function) {
            minimizer.defineMinimizationFunction(name, function);
            return this;
        }

        public DataMinimizerInterceptor build() {
            minimizer.loadConfig(config);
            return new DataMinimizerInterceptor(minimizer, config.getKeyServerHost(), config.getKeyServerPort());
        }
    }
}