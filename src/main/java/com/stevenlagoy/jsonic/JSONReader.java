package com.stevenlagoy.jsonic;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * JSONReader reads a JSON file and returns its lines. Used with JSONProcessor.
 */
public class JSONReader {

    private JSONReader() {
    }

    /**
     * Reads a JSON file and returns its contents as a List of Strings.
     *
     * @param filepath
     *                 The path to a JSON file
     *
     * @return A List of Strings where each entry is a line from the file
     */
    public static List<String> readLines(Path filepath) {
        ArrayList<String> contents = new ArrayList<String>();
        try {
            // String content = new String(Files.readAllBytes(filepath),
            // StandardCharsets.UTF_8);
            try (Scanner scanner = FileOperations.ScannerUtil.createScanner(filepath.toFile())) {
                while (scanner.hasNextLine()) {
                    contents.add(scanner.nextLine());
                }
            }
            return contents;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
