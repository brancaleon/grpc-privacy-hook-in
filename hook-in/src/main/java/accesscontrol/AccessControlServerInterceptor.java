package accesscontrol;

import clientside.AccessControlClientCredentials;
import io.grpc.*;

/**
 * This interceptor gets the JWT from the metadata, verifies it and sets the client identifier
 * obtained from the token into the context. In order not to complicate the example with additional
 * checks (expiration date, issuer and etc.), it relies only on the signature of the token for
 * verification.
 */
public class AccessControlServerInterceptor implements ServerInterceptor {

    private final ConfigParser config;

    public AccessControlServerInterceptor(String configPath) {
        config = new ConfigParser(configPath);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
                                                                 Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        Authorization authorization = new Authorization(metadata, config.getKeyServerHost(), config.getKeyServerPort());
        Status status;
        if (authorization.getPurposeOrNull() != null) {
            if (config.isClientRejectedForPurpose(authorization.getPurposeOrNull(), authorization.getSubjectOrNull())) {
                status = Status.UNAUTHENTICATED.withDescription("Client unauthorized for this purpose");
            } else if (config.isMethodRejectedForPurpose(authorization.getPurposeOrNull(), serverCall.getMethodDescriptor().getFullMethodName())) {
                status = Status.UNAUTHENTICATED.withDescription("Requested method not allowed in this purpose");
            } else {
                // set client id into current context
                Context ctx = Context.current().withValue(AccessControlClientCredentials.CLIENT_ID_CONTEXT_KEY, authorization.getSubjectOrNull());
                return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);
            }
        } else {
            status = authorization.getErrorStatus();
        }
        serverCall.close(status, new Metadata());
        return new ServerCall.Listener<ReqT>() {
            // noop
        };
    }


}
