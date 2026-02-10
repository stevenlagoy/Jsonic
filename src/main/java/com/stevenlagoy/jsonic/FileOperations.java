package com.stevenlagoy.jsonic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class FileOperations {

    public static enum FileExtension {
        ALL(""), HTML(".html"), JSON(".json"), JAVA(".java"), TEXT(".txt");

        private final String extension;

        FileExtension(String extension) {
            this.extension = extension != null ? extension : "";
        }

        public String getExtension() {
            return extension;
        }
    }

    public static class ScannerUtil {
        public static Scanner createScanner(InputStream inputStream) {
            return new Scanner(inputStream, StandardCharsets.UTF_8);
        }

        public static Scanner createScanner(File file) throws IOException {
            return new Scanner(file, StandardCharsets.UTF_8);
        }
    }

    /**
     * Returns a Set of Paths for all the files in the specified directory.
     * <p>
     * Equivalent to {@link FileOperations#listFiles(Path, FileExtension)
     * listFiles(dir, FileExtension.ALL)}
     *
     * @param dir
     *            The path to the directory to list the files within
     *
     * @return A Set of Paths to each file within the directory
     *
     * @throws IOException
     *                     If the directory path is invalid or unable to be located
     *
     * @see FileExtension#ALL
     */
    public static Set<Path> listFiles(Path dir) throws IOException {
        try {
            Set<Path> pathSet = listFiles(dir, FileExtension.ALL);
            return pathSet;
        } catch (IOException e) {
            throw e;
        }
    }

    /**
     * Returns a Set of Paths for all the files in the specificed directory with the
     * given extension.
     *
     * @param dir
     *                  The path to the directory to list the files within.
     * @param extension
     *                  A FileOperations.FileExtension to filter the Path results
     *                  by.
     *
     * @return A Set of Paths to each file within the directory with the extension.
     *
     * @throws IOException
     *                     If the directory path is invalid or unable to be located.
     */
    public static Set<Path> listFiles(Path dir, FileExtension extension) throws IOException {
        if (dir == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        Set<Path> pathSet = new HashSet<>();
        dir = dir.normalize();
        if (!Files.exists(dir)) {
            throw new IOException("The specified path, " + dir.toString() + ", was not found.");
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (path == null || path.getFileName() == null) {
                    continue; // Skip null paths
                }
                Path fileName = path.getFileName();
                if (fileName == null)
                    return null;
                if (!Files.isDirectory(path) && !FilePaths.IGNORED_FILES.contains(fileName.toString())
                        && fileName.endsWith(extension.getExtension())) {
                    pathSet.add(dir.resolve(fileName));
                }
            }
            return pathSet;
        } catch (IOException e) {
            System.err.println("Error accessing directory: " + dir + " - " + e.getMessage());
            throw e;
        }
    }

    public static void emptyFiles(Path dir, String extension) throws IOException {
        Set<Path> paths = listFiles(dir); // does not include ignored files
        for (Path path : paths) {
            // Delete if extension matches or if wildcard
            if (extension.equals("*") || path.toString().endsWith(extension)) {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    System.err.println("Failed to delete file: " + path.toString());
                    throw e;
                }
            }
        }
    }

    public static List<String> readFile(Path path) {
        try {
            Scanner scanner = ScannerUtil.createScanner(path.toFile());
            List<String> result = new ArrayList<>();
            while (scanner.hasNextLine()) {
                result.add(scanner.nextLine());
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void writeFile(String filename, String extension, Path destination, String content) {
        writeFile(filename, extension, destination, Collections.singletonList(content));
    }

    public static void writeFile(String filename, String extension, Path destination, List<String> content) {
        Path filePath = destination.resolve(filename + extension);
        File file = filePath.toFile();
        writeFile(file, content);
    }

    public static void writeFile(File file, List<String> content) {
        try {
            Files.createDirectories(file.getParentFile().toPath());
            if (!file.createNewFile() && !file.exists()) {
                throw new IOException("Failed to create new file: " + file.getAbsolutePath());
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file, false),
                    StandardCharsets.UTF_8)) {
                for (String line : content) {
                    writer.write(line + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
