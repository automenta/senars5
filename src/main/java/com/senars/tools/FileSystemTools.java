package com.senars.tools;

import dev.langchain4j.agent.tool.Tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

/**
 * A set of tools for interacting with the local file system.
 * Methods are designed to be used by an AI agent and return descriptive strings
 * rather than throwing exceptions on common errors (e.g., file not found).
 */
public class FileSystemTools {

    @Tool("Reads the entire content of a file at the specified path.")
    public String readFile(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool("Writes content to a file at the specified path. Overwrites the file if it already exists.")
    public String writeFile(String path, String content) {
        try {
            Files.write(Paths.get(path), content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return "File written successfully to " + path;
        } catch (IOException e) {
            return "Error writing file: " + e.getMessage();
        }
    }

    @Tool("Appends content to a file at the specified path. Creates the file if it does not exist.")
    public String appendFile(String path, String content) {
        try {
            Files.write(Paths.get(path), content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return "Content appended successfully to " + path;
        } catch (IOException e) {
            return "Error appending to file: " + e.getMessage();
        }
    }

    @Tool("Creates a new directory at the specified path. Does nothing if the directory already exists.")
    public String createDirectory(String path) {
        try {
            Files.createDirectories(Paths.get(path));
            return "Directory created successfully at " + path;
        } catch (IOException e) {
            return "Error creating directory: " + e.getMessage();
        }
    }

    @Tool("Lists the contents (files and directories) of a directory at the specified path.")
    public String listDirectory(String path) {
        try {
            return Files.list(Paths.get(path))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return "Error listing directory: " + e.getMessage();
        }
    }
}
