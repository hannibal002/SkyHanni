package at.hannibal2.skyhanni.utils.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

/** Converts only schema-annotated keybindings; other persisted numbers are untouched. */
public final class SdlKeybindSchema {
    public static final int VERSION = 147;
    public static final int UNBOUND = -1;

    private SdlKeybindSchema() {}

    /** GLFW uses mouse indices 0..7; SDL keyboard scancodes must not share those values. */
    public static int encodeMouse(int logicalButton) {
        return logicalButton >= 0 && logicalButton < 8 ? -100 - logicalButton : UNBOUND;
    }

    public static boolean isMouse(int binding) {
        return binding <= -100 && binding >= -107;
    }

    public static int logicalMouse(int binding) {
        return isMouse(binding) ? -100 - binding : UNBOUND;
    }

    /** SDL native numbering: left 1, middle 2, right 3. */
    public static int nativeMouse(int logicalButton) {
        return switch (logicalButton) {
            case 0 -> 1;
            case 1 -> 3;
            case 2 -> 2;
            default -> logicalButton >= 3 && logicalButton < 8 ? logicalButton + 1 : 0;
        };
    }

    public static int fromNativeMouse(int nativeButton) {
        return encodeMouse(switch (nativeButton) {
            case 1 -> 0;
            case 3 -> 1;
            case 2 -> 2;
            default -> nativeButton >= 4 && nativeButton <= 8 ? nativeButton - 1 : UNBOUND;
        });
    }

    /** Physical GLFW keys to SDL scancodes, including the non-contiguous F13..F24 range. */
    public static int fromGlfw(int value) {
        if (value == UNBOUND) return UNBOUND;
        if (value >= 0 && value < 8) return encodeMouse(value);
        if (value >= 65 && value <= 90) return value - 65 + 4;
        if (value >= 49 && value <= 57) return value - 49 + 30;
        if (value >= 290 && value <= 301) return value - 290 + 58;
        if (value >= 302 && value <= 313) return value - 302 + 104;
        if (value >= 321 && value <= 329) return value - 321 + 89;
        return switch (value) {
            case 32 -> 44; // space
            case 39 -> 52; // apostrophe
            case 44 -> 54;
            case 45 -> 45;
            case 46 -> 55;
            case 47 -> 56;
            case 48 -> 39;
            case 59 -> 51;
            case 61 -> 46;
            case 91 -> 47;
            case 92 -> 49;
            case 93 -> 48;
            case 96 -> 53;
            case 161 -> 100; // non-US backslash
            case 162 -> 50;  // non-US hash
            case 256 -> 41;
            case 257 -> 40;
            case 258 -> 43;
            case 259 -> 42;
            case 260 -> 73;
            case 261 -> 76;
            case 262 -> 79;
            case 263 -> 80;
            case 264 -> 81;
            case 265 -> 82;
            case 266 -> 75;
            case 267 -> 78;
            case 268 -> 74;
            case 269 -> 77;
            case 280 -> 57;
            case 281 -> 71;
            case 282 -> 83;
            case 283 -> 70;
            case 284 -> 72;
            case 320 -> 98;
            case 330 -> 99;
            case 331 -> 84;
            case 332 -> 85;
            case 333 -> 86;
            case 334 -> 87;
            case 335 -> 88;
            case 336 -> 103;
            case 340 -> 225;
            case 341 -> 224;
            case 342 -> 226;
            case 343 -> 227;
            case 344 -> 229;
            case 345 -> 228;
            case 346 -> 230;
            case 347 -> 231;
            case 348 -> 101;
            default -> UNBOUND;
        };
    }

    /** Called at the version-147 migration step, never on already-native documents. */
    public static void migrateDocument(JsonObject document, Class<?> schema) {
        JsonElement version = document.get("lastVersion");
        if (version != null && version.isJsonPrimitive() && version.getAsInt() >= VERSION) return;
        migrateObject(document, schema, 0);
        document.addProperty("lastVersion", VERSION);
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> clazz) return clazz;
        if (type instanceof ParameterizedType parameterized) return rawClass(parameterized.getRawType());
        if (type instanceof WildcardType wildcard && wildcard.getUpperBounds().length > 0) {
            return rawClass(wildcard.getUpperBounds()[0]);
        }
        return Object.class;
    }

    private static void migrateValue(JsonElement value, Type type, int depth) {
        if (depth > 64 || value == null || value.isJsonNull()) return;
        Class<?> clazz = rawClass(type);
        if (value.isJsonArray() && type instanceof ParameterizedType parameterized) {
            Type itemType = parameterized.getActualTypeArguments()[0];
            for (JsonElement item : value.getAsJsonArray()) migrateValue(item, itemType, depth + 1);
        } else if (value.isJsonObject() && java.util.Map.class.isAssignableFrom(clazz)
                && type instanceof ParameterizedType parameterized) {
            Type itemType = parameterized.getActualTypeArguments()[1];
            for (JsonElement item : value.getAsJsonObject().asMap().values()) migrateValue(item, itemType, depth + 1);
        } else if (value.isJsonObject()) {
            migrateObject(value.getAsJsonObject(), clazz, depth + 1);
        }
    }

    private static void migrateObject(JsonObject document, Class<?> schema, int depth) {
        if (depth > 64 || schema == Object.class) return;
        for (Class<?> current = schema; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) continue;
                SerializedName annotation = field.getAnnotation(SerializedName.class);
                String name = annotation == null ? field.getName() : annotation.value();
                JsonElement value = document.get(name);
                if (value == null || value.isJsonNull()) continue;
                if (field.isAnnotationPresent(ConfigEditorKeybind.class) && value.isJsonPrimitive()
                        && value.getAsJsonPrimitive().isNumber()) {
                    document.addProperty(name, fromGlfw(value.getAsInt()));
                } else {
                    migrateValue(value, field.getGenericType(), depth + 1);
                }
            }
        }
    }
}
