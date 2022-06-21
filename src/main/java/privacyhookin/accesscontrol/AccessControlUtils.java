package privacyhookin.accesscontrol;

import io.grpc.Context;
import io.grpc.Metadata;

import java.security.PublicKey;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

/**
 * Constants definition
 */
final public class AccessControlUtils {
    static public final String JWT_SIGNING_KEY = "L8hHXsaQOUjk5rg7XPGv4eL36anlCrkMz8CJ0i/8E/0=";

    static public final String PRIVATE_KEY = new AccessControlPurposesParser("purposes.json").getPrivateKey();
    static public String getPublicKey(String clientId){
        return new AccessControlPurposesParser("purposes.json").getPublicKey(clientId);
    }
    static public final String BEARER_TYPE = "Bearer";

    static public final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER);
    static public final Context.Key<String> CLIENT_ID_CONTEXT_KEY = Context.key("clientId");

    private AccessControlUtils() {
        throw new AssertionError();
    }
}
