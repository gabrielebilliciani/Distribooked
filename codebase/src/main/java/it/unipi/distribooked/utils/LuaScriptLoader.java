package it.unipi.distribooked.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public class LuaScriptLoader {

    private LuaScriptLoader() {
        // Utility class, no need to instantiate
    }

    public static String loadScript(String scriptName) {
        try (InputStream inputStream = LuaScriptLoader.class.getClassLoader().getResourceAsStream("scripts/" + scriptName)) {
            if (inputStream == null) {
                throw new IllegalStateException("Script file not found: " + scriptName);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Error loading Lua script: " + scriptName, e);
        }
    }
}