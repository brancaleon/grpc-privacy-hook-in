package privacyhookin.accesscontrol;

import io.grpc.*;

import static privacyhookin.accesscontrol.AccessControlClientCredentials.PURPOSES_FILE;

/**
 * This interceptor gets the JWT from the metadata, verifies it and sets the client identifier
 * obtained from the token into the context. In order not to complicate the example with additional
 * checks (expiration date, issuer and etc.), it relies only on the signature of the token for
 * verification.
 */
public class AccessControlServerInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
                                                                 Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        Authorization authorization = new Authorization(metadata);
        Status status;
        if (authorization.getPurposeOrNull() != null) {
            AccessControlPurposesParser acpp = new AccessControlPurposesParser(PURPOSES_FILE);
            if (acpp.isAllowedPurpose(authorization.getPurposeOrNull(), authorization.getSubjectOrNull())) {
                // set client id into current context
                Context ctx = Context.current().withValue(AccessControlClientCredentials.CLIENT_ID_CONTEXT_KEY, authorization.getSubjectOrNull());
                return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);
            }
            status = Status.UNAUTHENTICATED.withDescription("Client unauthorized for this purpose");
        } else {
            status = authorization.getErrorStatus();
        }
        serverCall.close(status, new Metadata());
        return new ServerCall.Listener<ReqT>() {
            // noop
        };
    }


}
