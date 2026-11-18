package de.linusdev.mixin;

import de.linusdev.PackSodiumCompatibility;
import de.linusdev.SodiumCoreShaderSupport;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Collectors;

@Mixin(PackListWidget.ResourcePackEntry.class)
public abstract class MixinResourcePackEntry {

    @Shadow @Final private ResourcePackOrganizer.Pack pack;
    @Shadow @Final protected MinecraftClient client;
    @Shadow @Final private PackListWidget widget;

    @Inject(method = "enable", at = @At("HEAD"), cancellable = true)
    private void sodiumCoreShaderSupport$checkCompatibility(CallbackInfoReturnable<Boolean> cir) {
        ResourcePackProfile profile = ((MixinAbstractPack) pack).getProfile();
        var result = SodiumCoreShaderSupport.isResourcePackCompatible(profile);
        if (result.compatibility() == PackSodiumCompatibility.COMPATIBLE) {
            return;
        }

        Text title;
        Text message;
        Text yes = Text.literal("Enable Anyway").setStyle(Style.EMPTY.withColor(0xFF0000));
        Text no = Text.of("OK");

        switch (result.compatibility()) {
            case NOT_COMPATIBLE -> {
                String versions = result.minecraftVersions().stream().collect(Collectors.joining(", "));
                if (result.correctMcVersionIndex() != null) {
                    title = Text.of("Compatible Sodium versions for this Minecraft version:");
                    String sodiumVersions = result.sodiumVersions().stream().collect(Collectors.joining(", "));
                    if (sodiumVersions.isBlank()) {
                        versions = result.minecraftVersions().stream()
                                .filter(version -> !version.equals(result.minecraftVersions().get(result.correctMcVersionIndex())))
                                .collect(Collectors.joining(", "));
                        sodiumVersions = "No listed Sodium version";
                        message = Text.of(sodiumVersions + ". Other Minecraft versions listed: " + versions);
                    } else {
                        message = Text.of(sodiumVersions);
                    }
                } else {
                    title = Text.of("Compatible Minecraft versions:");
                    message = Text.of(versions.isBlank() ? "None listed" : versions);
                }
            }
            case MISSING_INFORMATION -> {
                title = Text.of("Compatibility information missing");
                message = Text.of("This resource pack replaces vanilla shaders but does not declare Sodium compatibility. It may not work correctly. Enable it anyway?");
            }
            case MALFORMED_INFORMATION -> {
                title = Text.of("Compatibility information is invalid");
                message = Text.of("This resource pack has invalid Sodium compatibility information. See the log for details. Enable it anyway?");
            }
            default -> throw new IllegalStateException("Unexpected compatibility state: " + result.compatibility());
        }

        this.client.setScreen(new ConfirmScreen(
                confirmed -> {
                    this.client.setScreen(((MixinPackListWidget) this.widget).getScreen());
                    if (confirmed) {
                        this.pack.enable();
                    }
                },
                title,
                message,
                yes,
                no
        ));

        cir.setReturnValue(false);
    }
}
