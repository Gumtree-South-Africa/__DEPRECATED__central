package com.ecg.comaas.gtuk.filter.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Utils {
    static String readFileContent(Path name) {
        try {
            return new String(Files.readAllBytes(name.toAbsolutePath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
