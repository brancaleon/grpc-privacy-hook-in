package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.jpackage.internal.IOUtils;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class ServicesParser {

    public static Map parse(String fileName) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String content = new String(Files.readAllBytes(Paths.get(fileName)));
            return mapper.readValue(content, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
