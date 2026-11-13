package quantumquest.triviathrone.mod;

import quantumquest.triviathrone.api.system.ISystemProvider;
import quantumquest.triviathrone.api.system.SystemProviderContext;
import quantumquest.triviathrone.api.system.SystemProviderDescriptor;
import quantumquest.triviathrone.api.system.SystemServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SystemModManagerTest {
    public static final AtomicBoolean CLOSED = new AtomicBoolean();
    public static final AtomicInteger CLOSED_COUNT = new AtomicInteger();
    public static final AtomicReference<String> INITIALIZED_DATA_DIRECTORY = new AtomicReference<>();
    public static final AtomicReference<String> INITIALIZED_SETTING = new AtomicReference<>();

    @TempDir
    Path tempDir;

    @BeforeEach
    void resetLifecycleState() {
        CLOSED.set(false);
        CLOSED_COUNT.set(0);
        INITIALIZED_DATA_DIRECTORY.set(null);
        INITIALIZED_SETTING.set(null);
    }

    @Test
    void discoversProvidersRegistersTypedServicesAndClosesLifecycle() throws Exception {
        Path modsDirectory = tempDir.resolve("mods/system");
        Files.createDirectories(modsDirectory);
        writeProviderJar(modsDirectory.resolve("test-system.jar"), TestProvider.class);

        SystemProviderContext context = new SystemProviderContext(
                tempDir.resolve("data/system"),
                Map.of(
                        "triviathrone.system.test.database-url", "jdbc:test",
                        "triviathrone.system.other.database-url", "jdbc:unused"
                )
        );

        try (SystemModManager manager = SystemModManager.load(modsDirectory, context, parentFor(TestProvider.class))) {
            assertEquals("test", manager.providers().getFirst().id());
            assertEquals("registered", manager.find(TestService.class).orElseThrow().value());
            assertEquals(tempDir.resolve("data/system/test").toAbsolutePath().normalize().toString(),
                    INITIALIZED_DATA_DIRECTORY.get());
            assertEquals("jdbc:test", INITIALIZED_SETTING.get());
            assertFalse(CLOSED.get());
            assertThrows(IllegalStateException.class,
                    () -> manager.register(TestService.class, () -> "late"));
        }

        assertTrue(CLOSED.get());
    }

    @Test
    void rollsBackStartedProvidersWhenRegistrationConflicts() throws IOException {
        Path modsDirectory = tempDir.resolve("mods/system");
        Files.createDirectories(modsDirectory);
        writeProviderJar(modsDirectory.resolve("duplicate-system.jar"), TestProvider.class, DuplicateProvider.class);
        SystemProviderContext context = new SystemProviderContext(tempDir.resolve("data/system"), Map.of());

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> SystemModManager.load(modsDirectory, context, parentFor(TestProvider.class, DuplicateProvider.class)));

        assertTrue(failure.getMessage().contains("already registered"));
        assertEquals(2, CLOSED_COUNT.get());
    }

    private static ClassLoader parentFor(Class<?>... providers) {
        Set<String> providerClassNames = Arrays.stream(providers)
                .map(Class::getName)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new ClassLoader(ISystemProvider.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (providerClassNames.contains(name)) {
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name, resolve);
            }
        };
    }

    private static void writeProviderJar(Path jarPath, Class<?>... providerTypes) throws IOException {
        String serviceDescriptor = "META-INF/services/" + ISystemProvider.class.getName();
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            for (Class<?> providerType : providerTypes) {
                String classResource = providerType.getName().replace('.', '/') + ".class";
                jar.putNextEntry(new JarEntry(classResource));
                try (InputStream providerClass = providerType.getClassLoader().getResourceAsStream(classResource)) {
                    if (providerClass == null) {
                        throw new IOException("Could not read test system provider class.");
                    }
                    providerClass.transferTo(jar);
                }
                jar.closeEntry();
            }
            jar.putNextEntry(new JarEntry(serviceDescriptor));
            String providerList = Arrays.stream(providerTypes)
                    .map(Class::getName)
                    .collect(java.util.stream.Collectors.joining("\n", "", "\n"));
            jar.write(providerList.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            jar.closeEntry();
        }
    }

    public interface TestService {
        String value();
    }

    public static class TestProvider implements ISystemProvider {
        public TestProvider() {
        }

        @Override
        public SystemProviderDescriptor descriptor() {
            return new SystemProviderDescriptor("test", "1.0.0", "2.0.0");
        }

        @Override
        public void initialize(SystemProviderContext context, SystemServiceRegistry services) {
            INITIALIZED_DATA_DIRECTORY.set(context.dataDirectory().toString());
            INITIALIZED_SETTING.set(context.property("database-url").orElse(null));
            services.register(TestService.class, () -> "registered");
        }

        @Override
        public void close() {
            CLOSED.set(true);
            CLOSED_COUNT.incrementAndGet();
        }
    }

    public static class DuplicateProvider implements ISystemProvider {
        public DuplicateProvider() {
        }

        @Override
        public SystemProviderDescriptor descriptor() {
            return new SystemProviderDescriptor("duplicate", "1.0.0", "2.0.0");
        }

        @Override
        public void initialize(SystemProviderContext context, SystemServiceRegistry services) {
            services.register(TestService.class, () -> "duplicate");
        }

        @Override
        public void close() {
            CLOSED_COUNT.incrementAndGet();
        }
    }
}
