package accesscontrol;

import clientside.AccessControlClientCredentials;
import com.peng.gprc_hook_in.keyserver.KeyServerServiceGrpc;
import com.peng.gprc_hook_in.keyserver.PublicKeyRequest;
import com.peng.gprc_hook_in.keyserver.PublicKeyResponse;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Status;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class Authorization {

    private Status errorStatus = null;
    private String purpose = null;
    private String subject = null;
    private final String keyServerHost;
    private final int keyServerPort;

    public Authorization(Metadata metadata, String keyServerHost, int keyServerPort) {
        this.keyServerHost = keyServerHost;
        this.keyServerPort = keyServerPort;
        String value = metadata.get(AccessControlClientCredentials.AUTHORIZATION_METADATA_KEY);
        if (value == null) {
            errorStatus = Status.UNAUTHENTICATED.withDescription("Authorization token is missing");
        } else if (!value.startsWith(AccessControlClientCredentials.BEARER_TYPE)) {
            errorStatus = Status.UNAUTHENTICATED.withDescription("Unknown authorization type");
        } else {
            String client_id = metadata.get(AccessControlClientCredentials.CLIENT_ID_METADATA_KEY);
            String keyPath = Paths.get(".").toAbsolutePath().normalize()
                    + String.format("/delivery/src/main/java/delivery/%s/public_key_%s.der", client_id, client_id);
            PublicKey public_key = null;
            File f = new File(keyPath);
            // check if public key is already stored/cached
            if(f.exists() && !f.isDirectory()) {
                byte[] publicKeyBytes;
                try {
                    publicKeyBytes = Files.readAllBytes(Paths.get(keyPath));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
                KeyFactory kf = null;
                try {
                    kf = KeyFactory.getInstance("RSA");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                try {
                    public_key = kf.generatePublic(spec);
                } catch (InvalidKeySpecException e) {
                    throw new RuntimeException(e);
                }
            }
            else{
                byte[] publicKeyBytes = getPublicKeyBytes(client_id);
                public_key = getPublicKeyFromBytes(publicKeyBytes);
                try (FileOutputStream stream = new FileOutputStream(keyPath)) {
                    stream.write(publicKeyBytes);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            String token = value.substring(AccessControlClientCredentials.BEARER_TYPE.length()).trim();
            try {
                JwtParser parser = Jwts.parser().setSigningKey(public_key);
                // verify token signature and parse claims
                Jws<Claims> claims = parser.parseClaimsJws(token);
                subject = claims.getBody().getSubject();
                purpose = (String) claims.getBody().get("purpose");
            } catch (Exception e) {
                errorStatus = Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e);
            }
        }
    }

    private byte[] getPublicKeyBytes(String clientId){
        PublicKeyRequest publicKeyRequest = PublicKeyRequest.newBuilder()
                .setClient(clientId).build();
        Channel channel = ManagedChannelBuilder
                .forAddress(keyServerHost, keyServerPort)
                .usePlaintext().build();
        KeyServerServiceGrpc.KeyServerServiceBlockingStub keyserverStub = KeyServerServiceGrpc
                .newBlockingStub(channel);
        PublicKeyResponse response = keyserverStub.getPublicKey(publicKeyRequest);
        return response.getKey().toByteArray();
    }

    private PublicKey getPublicKeyFromBytes(byte[] publicKeyBytes) {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public String getPurposeOrNull() {
        return purpose;
    }

    public String getSubjectOrNull() {
        return subject;
    }

    public Status getErrorStatus() {
        return errorStatus;
    }
}
