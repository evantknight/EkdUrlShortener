package dev.evanknight.urlshortener.service.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {
    public static final Path RESOURCES_ROOT = Path.of(".");
    public static final Path INDEX_PATH = RESOURCES_ROOT.resolve("index.html");
    public static final Path NOT_FOUND_PATH = RESOURCES_ROOT.resolve("404.html");

    public static String readFile(final Path path) {
        try {
            return Files.readString(path);
        } catch (final IOException e) {
            throw new RuntimeException("Could not read file: " + path.toAbsolutePath(), e);
        }
    }

}
