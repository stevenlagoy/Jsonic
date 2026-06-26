package com.stevenlagoy.jsonic;

import org.jetbrains.annotations.NotNull;

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

/** FileOperations provides utilities for working with files. */
class FileOperations {

    private FileOperations() {
    }

    /**
     * Enum for File Extensions, useful for clearing or listing files in a
     * directory, as well as reading or writing to/from a certain type of file.
     */
    enum FileExtension {
        /** Blank extension */
        ALL(""),
        /** Extension for HTML files */
        HTML(".html"),
        /** Extension for JSON files */
        JSON(".json"),
        /** Extension for Java files */
        JAVA(".java"),
        /** Extension for text files */
        TEXT(".txt");

        private final @NotNull String extension;

        FileExtension(@NotNull String extension) {
            this.extension = extension;
        }

        /**
         * Get the extension string for this FileExtension.
         * 
         * @return String extension, containing a dot '.' followed by valid extension
         *         characters.
         */
        public @NotNull String getExtension() {
            return extension;
        }
    }

    /**
     * ScannerUtil is a scanner that can read from an InputStream, which wraps
     * java.util.Scanner.
     */
    public static class ScannerUtil {

        private ScannerUtil() {
        }

        /**
         * Create a scanner which reads from the given inputStream.
         * 
         * @param inputStream Input Stream which the created scanner can read from.
         * @return New scanner which reads from the Input Stream.
         */
        public static @NotNull Scanner createScanner(@NotNull InputStream inputStream) {
            return new Scanner(inputStream, StandardCharsets.UTF_8);
        }

        /**
         * Create a scanner which can read from the given file.
         * 
         * @param file File the scanner will read from.
         * @return New scanner which reads from the file.
         * @throws IOException When the file does not exist, lacks permissions, or is
         *                     being used by another blocking process.
         */
        public static @NotNull Scanner createScanner(@NotNull File file) throws IOException {
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
    public static @NotNull Set<Path> listFiles(@NotNull Path dir) throws IOException {
        return listFiles(dir, FileExtension.ALL);
    }

    /**
     * Returns a Set of Paths for all the files in the specified directory with the
     * given extension.
     *
     * @param dir       The path to the directory to list the files within.
     * @param extension A FileOperations.FileExtension to filter the Path results
     *                  by.
     *
     * @return A Set of Paths to each file within the directory with the extension.
     *
     * @throws IOException If the directory path is invalid or unable to be located.
     */
    public static @NotNull Set<Path> listFiles(@NotNull Path dir, @NotNull FileExtension extension) throws IOException {
        Set<Path> pathSet = new HashSet<>();
        dir = dir.normalize();
        if (!Files.exists(dir)) {
            throw new IOException("The specified path, " + dir + ", was not found.");
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (path == null || path.getFileName() == null) {
                    continue; // Skip null paths
                }
                Path fileName = path.getFileName();
                if (!Files.isDirectory(path)
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

    /**
     * Empty a directory of all files with the passed extension.
     * 
     * @param dir       Directory to empty.
     * @param extension Extension for files which should be deleted.
     * @throws IOException When the file could not be deleted, either because it
     *                     does not exist, lacks permissions, or is being read by
     *                     another process.
     */
    public static void emptyFiles(@NotNull Path dir, @NotNull String extension) throws IOException {
        Set<Path> paths = listFiles(dir); // does not include ignored files
        for (Path path : paths) {
            // Delete if extension matches or if wildcard
            if (extension.equals("*") || path.toString().endsWith(extension)) {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    System.err.println("Failed to delete file: " + path);
                    throw e;
                }
            }
        }
    }

    /**
     * Read the file with the given path.
     * 
     * @param path Path to the file to read
     * @return List of Strings for the lines in the file.
     */
    public static @NotNull List<String> readFile(@NotNull Path path) throws IOException {
        Scanner scanner = ScannerUtil.createScanner(path.toFile());
        List<String> result = new ArrayList<>();
        while (scanner.hasNextLine()) {
            result.add(scanner.nextLine());
        }
        return result;
    }

    /**
     * Write to a file with the given name, extension, directory, and containing the
     * passed content.
     * 
     * @param filename  Name of the file to write into.
     * @param extension Extension of the file to write into.
     * @param dir       Directory for the location of the file.
     * @param content   The String content to be written into the file.
     */
    public static void writeFile(@NotNull String filename, @NotNull String extension, @NotNull Path dir, @NotNull String content) throws IOException {
        writeFile(filename, extension, dir, Collections.singletonList(content));
    }

    /**
     * Write to a file with the given name, extension, directory, and containing the
     * passed content.
     * 
     * @param filename  Name of the file to write into.
     * @param extension Extension of the file to write into.
     * @param dir       Directory for the location of the file.
     * @param content   The Strings content to be written into the file.
     */
    public static void writeFile(@NotNull String filename, @NotNull String extension, @NotNull Path dir, @NotNull List<String> content) throws IOException {
        Path filePath = dir.resolve(filename + extension);
        File file = filePath.toFile();
        writeFile(file, content);
    }

    /**
     * Write to the passed file with the passed content.
     * 
     * @param file    File to write into. Will be cleared before writing.
     * @param content Content to write into the file.
     */
    public static void writeFile(@NotNull File file, @NotNull List<String> content) throws IOException {
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
    }
}
