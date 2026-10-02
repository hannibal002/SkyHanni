package at.hannibal2.skyhanni.utils.compat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SdlKeybindSchemaTest {
    static class Keys {
        @ConfigEditorKeybind(defaultKey = -1) int a;
        @ConfigEditorKeybind(defaultKey = -1) int b;
        @ConfigEditorKeybind(defaultKey = -1) int rightMouse;
        @ConfigEditorKeybind(defaultKey = -1) int middleMouse;
        @ConfigEditorKeybind(defaultKey = -1) int f13;
        @ConfigEditorKeybind(defaultKey = -1) int leftControl;
        @ConfigEditorKeybind(defaultKey = -1) int rightSuper;
        @ConfigEditorKeybind(defaultKey = -1) int unbound;
        @ConfigEditorKeybind(defaultKey = -1) io.github.notenoughupdates.moulconfig.observer.Property<Integer> property;
        @com.google.gson.annotations.SerializedName("renamedKey")
        @ConfigEditorKeybind(defaultKey = -1) int renamed;
        int ordinaryNumber;
    }

    static class Document {
        int lastVersion;
        Keys keys;
        List<Keys> loadouts;
        Map<String, Keys> profiles;
    }

    @Test
    void physicalKeysAndMouseButtonsNeverCollide() {
        assertEquals(4, SdlKeybindSchema.fromGlfw(65));
        assertEquals(5, SdlKeybindSchema.fromGlfw(66));
        assertEquals(-101, SdlKeybindSchema.fromGlfw(1));
        assertEquals(-102, SdlKeybindSchema.fromGlfw(2));
        assertNotEquals(SdlKeybindSchema.fromGlfw(65), SdlKeybindSchema.fromGlfw(1));
        assertNotEquals(SdlKeybindSchema.fromGlfw(66), SdlKeybindSchema.fromGlfw(2));
        assertEquals(-101, SdlKeybindSchema.fromNativeMouse(3));
        assertEquals(-102, SdlKeybindSchema.fromNativeMouse(2));
        assertEquals(3, SdlKeybindSchema.nativeMouse(SdlKeybindSchema.logicalMouse(-101)));
        assertEquals(2, SdlKeybindSchema.nativeMouse(SdlKeybindSchema.logicalMouse(-102)));
    }

    @Test
    void versionedAnnotatedMigrationSurvivesSaveReloadWithoutSecondConversion() {
        JsonObject document = JsonParser.parseString("""
            {"lastVersion":146,"keys":{"a":65,"b":66,"rightMouse":1,"middleMouse":2,
              "f13":302,"leftControl":341,"rightSuper":347,"unbound":-1,"ordinaryNumber":65,"property":65,"renamedKey":66},
             "loadouts":[{"a":65}],"profiles":{"example":{"b":66}}}
            """).getAsJsonObject();
        SdlKeybindSchema.migrateDocument(document, Document.class);
        JsonObject keys = document.getAsJsonObject("keys");
        assertEquals(4, keys.get("a").getAsInt());
        assertEquals(5, keys.get("b").getAsInt());
        assertEquals(-101, keys.get("rightMouse").getAsInt());
        assertEquals(-102, keys.get("middleMouse").getAsInt());
        assertEquals(104, keys.get("f13").getAsInt());
        assertEquals(224, keys.get("leftControl").getAsInt());
        assertEquals(231, keys.get("rightSuper").getAsInt());
        assertEquals(-1, keys.get("unbound").getAsInt());
        assertEquals(65, keys.get("ordinaryNumber").getAsInt());
        assertEquals(4, keys.get("property").getAsInt());
        assertEquals(5, keys.get("renamedKey").getAsInt());
        assertEquals(4, document.getAsJsonArray("loadouts").get(0).getAsJsonObject().get("a").getAsInt());
        assertEquals(5, document.getAsJsonObject("profiles").getAsJsonObject("example").get("b").getAsInt());
        String saved = new Gson().toJson(document);
        JsonObject reloaded = JsonParser.parseString(saved).getAsJsonObject();
        SdlKeybindSchema.migrateDocument(reloaded, Document.class);
        assertEquals(document, reloaded);
        assertEquals(147, reloaded.get("lastVersion").getAsInt());
    }

    @Test
    void nativeCaptureSaveReloadKeepsKeyboardAndMouseMeanings() {
        JsonObject keys = new JsonObject();
        keys.addProperty("a", 4);
        keys.addProperty("b", 5);
        keys.addProperty("rightMouse", SdlKeybindSchema.fromNativeMouse(3));
        keys.addProperty("middleMouse", SdlKeybindSchema.fromNativeMouse(2));
        JsonObject document = new JsonObject();
        document.addProperty("lastVersion", SdlKeybindSchema.VERSION);
        document.add("keys", keys);
        JsonObject reloaded = JsonParser.parseString(new Gson().toJson(document)).getAsJsonObject();
        SdlKeybindSchema.migrateDocument(reloaded, Document.class);
        assertEquals(document, reloaded);
        JsonObject savedKeys = reloaded.getAsJsonObject("keys");
        assertEquals(4, savedKeys.get("a").getAsInt());
        assertEquals(5, savedKeys.get("b").getAsInt());
        assertEquals(3, SdlKeybindSchema.nativeMouse(SdlKeybindSchema.logicalMouse(savedKeys.get("rightMouse").getAsInt())));
        assertEquals(2, SdlKeybindSchema.nativeMouse(SdlKeybindSchema.logicalMouse(savedKeys.get("middleMouse").getAsInt())));
    }

    @Test
    void freshNativeSchemaKeepsItsCurrentBindings() {
        JsonObject document = JsonParser.parseString("{\"lastVersion\":147,\"keys\":{\"a\":4,\"b\":5,\"rightMouse\":-101}}").getAsJsonObject();
        String before = document.toString();
        SdlKeybindSchema.migrateDocument(document, Document.class);
        assertEquals(before, document.toString());
    }
}
