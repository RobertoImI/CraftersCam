package org.crafterscr.crafterscam.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;

/**
 * Argumento simple para audiencias de CraftersCam.
 *
 * A diferencia de StringArgumentType.word(), permite conservar selectores
 * completos de Minecraft como @a[tag=staff] hasta el siguiente espacio.
 */
public final class AudienceArgumentType implements ArgumentType<String> {
    private static final AudienceArgumentType INSTANCE = new AudienceArgumentType();

    private AudienceArgumentType() {
    }

    public static AudienceArgumentType audience() {
        return INSTANCE;
    }

    public static String getAudience(CommandContext<?> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) {
        int start = reader.getCursor();

        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }

        return reader.getString().substring(start, reader.getCursor());
    }
}
