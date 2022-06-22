package privacyhookin.dataminimization;

import com.peng.gprc_hook_in.common.Position;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class DataMinimizer {
    public<ReqT> ReqT minimize(ReqT req){
        // TODO: Read config from file and apply functions to req object.
        //  Config can be JSON file and should map proto fields (as defined in the .proto files)
        //  to the data minimization functions to apply
        // fields can be accessed in this way: ((OrderRequest) req).meal_ or ((OrderRequest) req).getMeal()
        return req;
    }

    public static Object erasure(Object base, String field){
        // TODO: Perform erasure of data field
        return base;
    }

    public static Object generalization(Object base, String field){
        // TODO: Perfom generalization of specified field
        //  Try to support different data types
        return base;
    }

    public static Object noising(Object base, String field){
        // TODO: Perform noising of the specified field
        //  Try to support different data types
        return base;
    }

    public static String hashing(String base, String field) {
        try{
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                final String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }

    public static Float positionToDistance(Position position1, Position position2, String field){
        // TODO: convert coordinates to distance
        return 1F;
    }
}
