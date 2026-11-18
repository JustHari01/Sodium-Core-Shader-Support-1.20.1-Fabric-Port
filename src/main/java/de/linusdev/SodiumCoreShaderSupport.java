package de.linusdev;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.GameVersion;
import net.minecraft.SharedConstants;
import net.minecraft.resource.Resource;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import de.linusdev.mixin.MixinResourcePackProfile;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SodiumCoreShaderSupport implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("sodiumcoreshadersupport");
    private static final ModContainer SODIUM = FabricLoader.getInstance().getModContainer("sodium").orElse(null);

    public static volatile Map<String, Map<String, Resource>> shaders = Map.of();

    public record PackSodiumCompReturn(
            PackSodiumCompatibility compatibility,
            List<String> sodiumVersions,
            List<String> minecraftVersions,
            Integer correctMcVersionIndex
    ) {
    }

    public static PackSodiumCompReturn isResourcePackCompatible(ResourcePackProfile profile) {
        if (SODIUM == null) {
            return new PackSodiumCompReturn(PackSodiumCompatibility.COMPATIBLE, null, null, null);
        }

        GameVersion currentGameVersion = SharedConstants.getGameVersion();
        Version sodiumVersion = SODIUM.getMetadata().getVersion();
        ResourcePackProfile.PackFactory packFactory = ((MixinResourcePackProfile) profile).getPackFactory();
        List<String> sodiumVersions = new ArrayList<>();
        List<String> minecraftVersions = new ArrayList<>();
        Integer correctMcVersionIndex = null;
        int mcVersionIndex = 0;

        try (ResourcePack resourcePack = packFactory.open(profile.getName())) {
            AtomicBoolean hasVanillaShaders = new AtomicBoolean(false);
            resourcePack.findResources(ResourceType.CLIENT_RESOURCES, "minecraft", "shaders",
                    (identifier, supplier) -> hasVanillaShaders.set(true));
            if (!hasVanillaShaders.get()) {
                return new PackSodiumCompReturn(PackSodiumCompatibility.COMPATIBLE, null, null, null);
            }

            InputSupplier<InputStream> supplier = resourcePack.open(
                    ResourceType.CLIENT_RESOURCES,
                    new Identifier("sodiumcoreshadersupport", "versions.json")
            );
            if (supplier == null) {
                return new PackSodiumCompReturn(PackSodiumCompatibility.MISSING_INFORMATION, null, null, null);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(supplier.get(), StandardCharsets.UTF_8))) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root == null || !root.isJsonObject()) {
                    return malformed(profile, "first JSON element must be an object");
                }

                JsonElement supportedVersions = root.getAsJsonObject().get("supported-versions");
                if (supportedVersions == null || !supportedVersions.isJsonObject()) {
                    return malformed(profile, "missing 'supported-versions' object");
                }

                for (Map.Entry<String, JsonElement> entry : supportedVersions.getAsJsonObject().entrySet()) {
                    if (!entry.getValue().isJsonArray()) {
                        return malformed(profile, "Sodium versions must be arrays");
                    }

                    minecraftVersions.add(entry.getKey());
                    if (!entry.getKey().equals(currentGameVersion.getName())) {
                        mcVersionIndex++;
                        continue;
                    }

                    correctMcVersionIndex = mcVersionIndex;
                    for (JsonElement element : entry.getValue().getAsJsonArray()) {
                        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                            return malformed(profile, "Sodium version entries must be strings");
                        }
                        String supportedSodiumVersion = element.getAsString();
                        sodiumVersions.add(supportedSodiumVersion);
                        if (sodiumVersion.equals(Version.parse(supportedSodiumVersion))) {
                            return new PackSodiumCompReturn(PackSodiumCompatibility.COMPATIBLE, null, null, null);
                        }
                    }
                    break;
                }
            }
        } catch (IOException | VersionParsingException | JsonParseException | IllegalStateException exception) {
            LOGGER.warn("Could not read Sodium compatibility information for resource pack '{}'", profile.getName(), exception);
            return new PackSodiumCompReturn(PackSodiumCompatibility.MALFORMED_INFORMATION, null, null, null);
        }

        return new PackSodiumCompReturn(PackSodiumCompatibility.NOT_COMPATIBLE,
                List.copyOf(sodiumVersions), List.copyOf(minecraftVersions), correctMcVersionIndex);
    }

    private static PackSodiumCompReturn malformed(ResourcePackProfile profile, String reason) {
        LOGGER.warn("Resource pack '{}' has malformed Sodium compatibility information: {}", profile.getName(), reason);
        return new PackSodiumCompReturn(PackSodiumCompatibility.MALFORMED_INFORMATION, null, null, null);
    }

    @Override
    public void onInitializeClient() {
        ResourceManagerHelper
                .get(ResourceType.CLIENT_RESOURCES)
                .registerReloadListener(
                        new SimpleResourceReloadListener<Void>() {

                            @Override
                            public Identifier getFabricId() {
                                return new Identifier("sodium", "shaders");
                            }

                            @Override
                            public CompletableFuture<Void> load(ResourceManager manager, Profiler profiler, Executor executor) {
                                return CompletableFuture.supplyAsync(() -> {
                                    LOGGER.info("Loading shaders...");
                                    Map<String, Map<String, Resource>> loadedShaders = new HashMap<>();

                                    manager.findAllResources("shaders", path -> true).forEach((identifier, resources) -> {
                                        if (resources.isEmpty()) {
                                            return;
                                        }

                                        Map<String, Resource> nameSpace = loadedShaders.computeIfAbsent(
                                                identifier.getNamespace(),
                                                k -> new HashMap<>()
                                        );

                                        nameSpace.put(
                                                identifier.getPath().substring("shaders/".length()),
                                                resources.get(resources.size() - 1)
                                        );
                                    });

                                    loadedShaders.forEach((namespace, resources) -> LOGGER.debug(
                                            "Loaded {} Sodium shader resources from namespace '{}'",
                                            resources.size(), namespace
                                    ));

                                    Map<String, Map<String, Resource>> immutableShaders = new HashMap<>();
                                    loadedShaders.forEach((namespace, resources) ->
                                            immutableShaders.put(namespace, Collections.unmodifiableMap(resources))
                                    );
                                    shaders = Collections.unmodifiableMap(immutableShaders);

                                    return null;
                                }, executor);
                            }

                            @Override
                            public CompletableFuture<Void> apply(Void data, ResourceManager manager, Profiler profiler, Executor executor) {
                                return CompletableFuture.runAsync(() -> {
                                }, executor);
                            }
                        });
    }
}
