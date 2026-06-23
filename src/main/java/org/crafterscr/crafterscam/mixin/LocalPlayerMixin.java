package org.crafterscr.crafterscam.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.crafterscr.crafterscam.client.ClientCinematicController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "isControlledCamera", at = @At("HEAD"), cancellable = true)
    private void crafterscam$allowMovementDuringCinematic(CallbackInfoReturnable<Boolean> cir) {
        if (ClientCinematicController.isActive()) {
            cir.setReturnValue(true);
        }
    }
}