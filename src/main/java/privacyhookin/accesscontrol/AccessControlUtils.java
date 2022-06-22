package privacyhookin.accesscontrol;

import io.grpc.Context;
import io.grpc.Metadata;

import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

/**
 * Constants definition
 */
final public class AccessControlUtils {

    static public final String PURPOSES_FILE = Paths.get(".").toAbsolutePath().normalize() +
            "/src/main/java/privacyhookin/accesscontrol/purposes.json";
    static public final String BEARER_TYPE = "Bearer";

    static public final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER);
    static public final Metadata.Key<String> CLIENT_ID_METADATA_KEY = Metadata.Key.of("clientId", ASCII_STRING_MARSHALLER);
    static public final Context.Key<String> CLIENT_ID_CONTEXT_KEY = Context.key("clientId");

    private AccessControlUtils() {
        throw new AssertionError();
    }

    static public PublicKey getPublicKey(String clientId){
        byte[] publicKeyBytes = new AccessControlPurposesParser(PURPOSES_FILE).getKeyBytes("public_key", clientId);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory kf = null;
        try {
            kf = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try {
            return kf.generatePublic(spec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    static public PrivateKey getPrivateKey(String clientId){
        byte[] privateKeyBytes = new AccessControlPurposesParser(PURPOSES_FILE).getKeyBytes("private_key", clientId);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory kf = null;
        try {
            kf = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try {
            return kf.generatePrivate(spec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
