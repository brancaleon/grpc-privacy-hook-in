package privacyhookin.accesscontrol;

import io.grpc.Context;
import io.grpc.Metadata;

import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

/**
 * Constants definition
 */
final public class AccessControlUtils {

    static public final String PURPOSES_FILE = Paths.get(".").toAbsolutePath().normalize() +
            "/src/main/java/privacyhookin/accesscontrol/purposes.json";
    static public final PrivateKey PRIVATE_KEY = getPrivateKey();
    static public final String BEARER_TYPE = "Bearer";

    static public final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER);
    static public final Context.Key<String> CLIENT_ID_CONTEXT_KEY = Context.key("clientId");

    private AccessControlUtils() {
        throw new AssertionError();
    }

    static public PublicKey getPublicKey(String clientId){
        String publicKeyPEM = new AccessControlPurposesParser(PURPOSES_FILE).getPublicKeyPEM(clientId);
        publicKeyPEM = publicKeyPEM.replace("-----BEGIN PUBLIC KEY-----\n", "");
        publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");
        publicKeyPEM = publicKeyPEM.replaceAll("\\s+","");
        byte[] encodedKey = Base64.getDecoder().decode(publicKeyPEM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);

        // KeyFactory fact = KeyFactory.getInstance("RSA");
        // X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(keyContentAsBytes);
        // PublicKey publicKey = fact.generatePublic(pubKeySpec);

        PublicKey pubKey = null;
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            pubKey = kf.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return pubKey;
    }

    static public PrivateKey getPrivateKey(){
        String privateKeyPEM = new AccessControlPurposesParser(PURPOSES_FILE).getPrivateKeyPEM();
        privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----\n", "");
        privateKeyPEM = privateKeyPEM.replace("-----BEGIN OPENSSH PRIVATE KEY-----\n", "");
        privateKeyPEM = privateKeyPEM.replace("-----END PRIVATE KEY-----", "");
        privateKeyPEM = privateKeyPEM.replace("-----END OPENSSH PRIVATE KEY-----", "");
        privateKeyPEM = privateKeyPEM.replaceAll("\\s+","");
        byte[] encodedKey = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);

        PrivateKey privKey = null;
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privKey = kf.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return privKey;
    }
}
