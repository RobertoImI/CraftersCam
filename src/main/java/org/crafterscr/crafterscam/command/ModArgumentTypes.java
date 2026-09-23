package org.crafterscr.crafterscam.command;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.crafterscr.crafterscam.CraftersCam;

public final class ModArgumentTypes {
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES =
            DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, CraftersCam.MOD_ID);

    @SuppressWarnings("unused")
    private static final DeferredHolder<ArgumentTypeInfo<?, ?>, SingletonArgumentInfo<AudienceArgumentType>> AUDIENCE =
            COMMAND_ARGUMENT_TYPES.register(
                    "audience",
                    () -> ArgumentTypeInfos.registerByClass(
                            AudienceArgumentType.class,
                            SingletonArgumentInfo.contextFree(AudienceArgumentType::audience)
                    )
            );

    private ModArgumentTypes() {
    }

    public static void register(IEventBus modEventBus) {
        COMMAND_ARGUMENT_TYPES.register(modEventBus);
    }
}
