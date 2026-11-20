package quantumquest.triviathrone.mod;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModMetadataLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsModDescriptorFromModXml() throws IOException {
        Path modJar = createJar("mod.xml", """
                <mod id="sample">
                    <name>Sample</name>
                    <version>1.2.3</version>
                    <apiVersion>2.0.0</apiVersion>
                    <description>Sample game mod.</description>
                    <providers>
                        <provider class="example.SampleProvider"/>
                    </providers>
                </mod>
                """);

        ModDescriptor descriptor = ModMetadataLoader.load(modJar);

        assertEquals("sample", descriptor.id());
        assertEquals("1.2.3", descriptor.version());
        assertEquals("2.0.0", descriptor.apiVersion());
        assertEquals(java.util.List.of("example.SampleProvider"), descriptor.providerClasses());
    }

    @Test
    void rejectsJarsWithOnlyLegacyPluginXml() throws IOException {
        Path modJar = createJar("plugin.xml", """
                <plugin id="legacy">
                    <name>Legacy</name>
                    <version>1.0.0</version>
                    <apiVersion>2.0.0</apiVersion>
                    <description>Legacy game plugin.</description>
                    <providers>
                        <provider class="example.LegacyProvider"/>
                    </providers>
                </plugin>
                """);

        IOException exception = assertThrows(IOException.class, () -> ModMetadataLoader.load(modJar));

        assertEquals("Game mod JAR is missing mod.xml: " + modJar, exception.getMessage());
    }

    @Test
    void loadsOptionalApiCompatibilityRange() throws IOException {
        Path modJar = createJar("mod.xml", """
                <mod id="sample">
                    <name>Sample</name>
                    <version>1.2.3</version>
                    <apiVersion>2.0.0</apiVersion>
                    <apiCompatibility maximumExclusive="3.0.0"/>
                    <description>Sample game mod.</description>
                    <providers>
                        <provider class="example.SampleProvider"/>
                    </providers>
                </mod>
                """);

        ModDescriptor descriptor = ModMetadataLoader.load(modJar);

        assertEquals("3.0.0", descriptor.apiMaximumExclusive());
    }

    @Test
    void retainsCompatibilityWithTheEarlyMisspelledElement() throws IOException {
        Path modJar = createJar("mod.xml", """
                <mod id="sample">
                    <name>Sample</name>
                    <version>1.2.3</version>
                    <apiVersion>2.0.0</apiVersion>
                    <apiCompatability maximumExclusive="3.0.0"/>
                    <description>Sample game mod.</description>
                    <providers>
                        <provider class="example.SampleProvider"/>
                    </providers>
                </mod>
                """);

        ModDescriptor descriptor = ModMetadataLoader.load(modJar);

        assertEquals("3.0.0", descriptor.apiMaximumExclusive());
    }

    private Path createJar(String entryName, String metadata) throws IOException {
        Path jar = tempDir.resolve(entryName.replace('.', '-') + ".jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry(entryName));
            output.write(metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            output.closeEntry();
        }
        return jar;
    }
}
