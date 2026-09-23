package org.crafterscr.crafterscam.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.crafterscr.crafterscam.camera.CamVisualSettings;
import org.crafterscr.crafterscam.camera.CameraEasing;
import org.crafterscr.crafterscam.server.CamServerManager;

import java.util.Collection;
import java.util.List;

public class CamCommands {
    private static final CamServerManager MANAGER = CamServerManager.INSTANCE;

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("ccam")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("settings")
                                .then(Commands.literal("info")
                                        .executes(ctx -> settingsInfo(ctx.getSource()))
                                )

                                .then(Commands.literal("fade")
                                        .then(Commands.argument("inSeconds", IntegerArgumentType.integer(0, 20))
                                                .then(Commands.argument("outSeconds", IntegerArgumentType.integer(0, 20))
                                                        .executes(ctx -> setFade(
                                                                ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "inSeconds"),
                                                                IntegerArgumentType.getInteger(ctx, "outSeconds")
                                                        ))
                                                )
                                        )
                                )

                                .then(Commands.literal("bars")
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ctx -> setBars(
                                                        ctx.getSource(),
                                                        BoolArgumentType.getBool(ctx, "enabled")
                                                ))
                                        )
                                )

                                .then(Commands.literal("hidehud")
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ctx -> setHideHud(
                                                        ctx.getSource(),
                                                        BoolArgumentType.getBool(ctx, "enabled")
                                                ))
                                        )
                                )

                                .then(Commands.literal("movement")
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ctx -> setMovement(
                                                        ctx.getSource(),
                                                        BoolArgumentType.getBool(ctx, "enabled")
                                                ))
                                        )
                                )
                        )

                        .then(Commands.literal("point")
                                .then(Commands.literal("save")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> savePoint(ctx.getSource(), StringArgumentType.getString(ctx, "id"), 70.0F))
                                                .then(Commands.argument("fov", FloatArgumentType.floatArg(30.0F, 110.0F))
                                                        .executes(ctx -> savePoint(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "id"),
                                                                FloatArgumentType.getFloat(ctx, "fov")
                                                        ))
                                                )
                                        )
                                )

                                .then(Commands.literal("remove")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MANAGER.pointIds(ctx.getSource().getServer()), builder))
                                                .executes(ctx -> removePoint(ctx.getSource(), StringArgumentType.getString(ctx, "id")))
                                        )
                                )

                                .then(Commands.literal("list")
                                        .executes(ctx -> listPoints(ctx.getSource()))
                                )

                                .then(Commands.literal("play")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MANAGER.pointIds(ctx.getSource().getServer()), builder))
                                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                                        .executes(ctx -> playPoint(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "id"),
                                                                IntegerArgumentType.getInteger(ctx, "seconds"),
                                                                defaultTargets(ctx.getSource())
                                                        ))
                                                        .then(Commands.argument("targets", EntityArgument.players())
                                                                .executes(ctx -> playPoint(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "id"),
                                                                        IntegerArgumentType.getInteger(ctx, "seconds"),
                                                                        EntityArgument.getPlayers(ctx, "targets")
                                                                ))
                                                        )
                                                )
                                        )
                                )

                                .then(Commands.literal("showall")
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ctx -> showAllPoints(
                                                        ctx.getSource(),
                                                        BoolArgumentType.getBool(ctx, "enabled")
                                                ))
                                        )
                                )
                        )

                        .then(Commands.literal("seq")
                                .then(Commands.literal("create")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> createSequence(ctx.getSource(), StringArgumentType.getString(ctx, "id")))
                                        )
                                )

                                .then(Commands.literal("remove")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MANAGER.sequenceIds(ctx.getSource().getServer()), builder))
                                                .executes(ctx -> removeSequence(ctx.getSource(), StringArgumentType.getString(ctx, "id")))
                                        )
                                )

                                .then(Commands.literal("clear")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MANAGER.sequenceIds(ctx.getSource().getServer()), builder))
                                                .executes(ctx -> clearSequence(ctx.getSource(), StringArgumentType.getString(ctx, "id")))
                                        )
                                )

                                .then(Commands.literal("undo")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MANAGER.sequenceIds(ctx.getSource().getServer()), builder))
                                                .executes(ctx -> undoSequence(ctx.getSource(), StringArgumentType.getString(ctx, "id")))
                                        )
                                )

                                .then(Commands.literal("list")
                                        .executes(ctx -> listSequences(ctx.getSource()))
                                )

                                .then(Commands.literal("addhold")
                                        .then(Commands.argument("seq", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MANAGER.sequenceIds(ctx.getSource().getServer()), builder))
                                                .then(Commands.argument("point", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MANAGER.pointIds(ctx.getSource().getServer()), builder))
                                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                                                .executes(ctx -> addHold(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "seq"),
                                                                        StringArgumentType.getString(ctx, "point"),
                                                                        IntegerArgumentType.getInteger(ctx, "seconds")
                                                                ))
                                                        )
                                                )
                                        )
                                )

                                .then(Commands.literal("addmove")
                                        .then(Commands.argument("seq", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MANAGER.sequenceIds(ctx.getSource().getServer()), builder))
                                                .then(Commands.argument("from", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MANAGER.pointIds(ctx.getSource().getServer()), builder))
                                                        .then(Commands.argument("to", StringArgumentType.word())
                                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MANAGER.pointIds(ctx.getSource().getServer()), builder))
                                                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                                                        .executes(ctx -> addMove(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "seq"),
                                                                                StringArgumentType.getString(ctx, "from"),
                                                                                StringArgumentType.getString(ctx, "to"),
                                                                                IntegerArgumentType.getInteger(ctx, "seconds"),
                                                                                "smooth"
                                                                        ))
                                                                        .then(Commands.argument("easing", StringArgumentType.word())
                                                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(CameraEasing.suggestions(), builder))
                                                                                .executes(ctx -> addMove(
                                                                                        ctx.getSource(),
                                                                                        StringArgumentType.getString(ctx, "seq"),
                                                                                        StringArgumentType.getString(ctx, "from"),
                                                                                        StringArgumentType.getString(ctx, "to"),
                                                                                        IntegerArgumentType.getInteger(ctx, "seconds"),
                                                                                        StringArgumentType.getString(ctx, "easing")
                                                                                ))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )

                                .then(Commands.literal("addcut")
                                        .then(Commands.argument("seq", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MANAGER.sequenceIds(ctx.getSource().getServer()), builder))
                                                .then(Commands.argument("point", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MANAGER.pointIds(ctx.getSource().getServer()), builder))
                                                        .executes(ctx -> addCut(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "seq"),
                                                                StringArgumentType.getString(ctx, "point")
                                                        ))
                                                )
                                        )
                                )

                                .then(Commands.literal("addview")
                                        .then(Commands.argument("seq", StringArgumentType.word())
                                                .then(Commands.argument("point", StringArgumentType.word())
                                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                                                .executes(ctx -> addViewHere(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "seq"),
                                                                        StringArgumentType.getString(ctx, "point"),
                                                                        IntegerArgumentType.getInteger(ctx, "seconds"),
                                                                        70.0F
                                                                ))
                                                                .then(Commands.argument("fov", FloatArgumentType.floatArg(30.0F, 110.0F))
                                                                        .executes(ctx -> addViewHere(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "seq"),
                                                                                StringArgumentType.getString(ctx, "point"),
                                                                                IntegerArgumentType.getInteger(ctx, "seconds"),
                                                                                FloatArgumentType.getFloat(ctx, "fov")
                                                                        ))
                                                                )
                                                        )
                                                )
                                        )
                                )

                                .then(Commands.literal("moveview")
                                        .then(Commands.argument("seq", StringArgumentType.word())
                                                .then(Commands.argument("point", StringArgumentType.word())
                                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                                                .executes(ctx -> moveViewHere(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "seq"),
                                                                        StringArgumentType.getString(ctx, "point"),
                                                                        IntegerArgumentType.getInteger(ctx, "seconds"),
                                                                        70.0F,
                                                                        "smooth"
                                                                ))
                                                                .then(Commands.argument("fov", FloatArgumentType.floatArg(30.0F, 110.0F))
                                                                        .executes(ctx -> moveViewHere(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "seq"),
                                                                                StringArgumentType.getString(ctx, "point"),
                                                                                IntegerArgumentType.getInteger(ctx, "seconds"),
                                                                                FloatArgumentType.getFloat(ctx, "fov"),
                                                                                "smooth"
                                                                        ))
                                                                        .then(Commands.argument("easing", StringArgumentType.word())
                                                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(CameraEasing.suggestions(), builder))
                                                                                .executes(ctx -> moveViewHere(
                                                                                        ctx.getSource(),
                                                                                        StringArgumentType.getString(ctx, "seq"),
                                                                                        StringArgumentType.getString(ctx, "point"),
                                                                                        IntegerArgumentType.getInteger(ctx, "seconds"),
                                                                                        FloatArgumentType.getFloat(ctx, "fov"),
                                                                                        StringArgumentType.getString(ctx, "easing")
                                                                                ))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )

                                .then(Commands.literal("path")
                                        .then(Commands.literal("show")
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MANAGER.sequenceIds(ctx.getSource().getServer()), builder))
                                                        .executes(ctx -> showSequencePath(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "id")
                                                        ))
                                                )
                                        )
                                        .then(Commands.literal("hide")
                                                .executes(ctx -> hideSequencePath(ctx.getSource()))
                                        )
                                )

                                .then(Commands.literal("preview")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MANAGER.sequenceIds(ctx.getSource().getServer()), builder))
                                                .executes(ctx -> previewSequence(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "id")
                                                ))
                                        )
                                )

                                .then(Commands.literal("play")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MANAGER.sequenceIds(ctx.getSource().getServer()), builder))
                                                .executes(ctx -> playSequence(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "id"),
                                                        defaultTargets(ctx.getSource())
                                                ))
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .executes(ctx -> playSequence(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "id"),
                                                                EntityArgument.getPlayers(ctx, "targets")
                                                        ))
                                                )
                                        )
                                )
                        )

                        .then(Commands.literal("stop")
                                .executes(ctx -> stop(ctx.getSource(), defaultTargets(ctx.getSource())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> stop(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))
                                )
                        )
        );
    }

    private static int settingsInfo(CommandSourceStack source) {
        CamVisualSettings settings = MANAGER.settings(source.getServer());

        String text = "CraftersCam settings: "
                + "fadeIn=" + (settings.fadeInTicks / 20.0F) + "s, "
                + "fadeOut=" + (settings.fadeOutTicks / 20.0F) + "s, "
                + "bars=" + settings.showBars + ", "
                + "hideHudAll=" + settings.hideHudAll + ", "
                + "movement=" + settings.allowMovement;

        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int setFade(CommandSourceStack source, int inSeconds, int outSeconds) {
        MANAGER.setFade(source.getServer(), inSeconds, outSeconds);
        source.sendSuccess(() -> Component.literal("Fade actualizado: entrada " + inSeconds + "s, salida " + outSeconds + "s."), false);
        return 1;
    }

    private static int setBars(CommandSourceStack source, boolean enabled) {
        MANAGER.setBars(source.getServer(), enabled);
        source.sendSuccess(() -> Component.literal("Barras cinematográficas: " + enabled), false);
        return 1;
    }

    private static int setHideHud(CommandSourceStack source, boolean enabled) {
        MANAGER.setHideHudAll(source.getServer(), enabled);
        source.sendSuccess(() -> Component.literal("Ocultar HUD completo: " + enabled), false);
        return 1;
    }

    private static int setMovement(CommandSourceStack source, boolean enabled) {
        MANAGER.setAllowMovement(source.getServer(), enabled);

        if (enabled) {
            source.sendSuccess(() -> Component.literal("Movimiento durante cinemáticas: true. Los jugadores podrán caminar, girar y brincar."), false);
        } else {
            source.sendSuccess(() -> Component.literal("Movimiento durante cinemáticas: false. Los jugadores podrán girar y brincar, pero no caminar."), false);
        }

        return 1;
    }

    private static int showAllPoints(CommandSourceStack source, boolean enabled) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        MANAGER.setShowAllPoints(player, enabled);

        if (enabled) {
            source.sendSuccess(() -> Component.literal("Mostrando puntos de cámara en esta dimensión."), false);
        } else {
            source.sendSuccess(() -> Component.literal("Puntos de cámara ocultos."), false);
        }

        return 1;
    }

    private static int savePoint(CommandSourceStack source, String id, float fov) throws CommandSyntaxException {
        if (!MANAGER.isValidId(id)) {
            source.sendFailure(Component.literal("ID inválido. Usa letras, números, _, - o ."));
            return 0;
        }

        ServerPlayer player = source.getPlayerOrException();
        MANAGER.savePoint(source.getServer(), player, id, fov);

        source.sendSuccess(() -> Component.literal("Punto de cámara guardado: " + id), false);
        return 1;
    }

    private static int removePoint(CommandSourceStack source, String id) {
        boolean removed = MANAGER.removePoint(source.getServer(), id);

        if (!removed) {
            source.sendFailure(Component.literal("No existe el punto: " + id));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Punto eliminado: " + id), false);
        return 1;
    }

    private static int listPoints(CommandSourceStack source) {
        Collection<String> ids = MANAGER.pointIds(source.getServer());

        if (ids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No hay puntos guardados."), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Puntos: " + String.join(", ", ids)), false);
        return ids.size();
    }

    private static int createSequence(CommandSourceStack source, String id) {
        if (!MANAGER.isValidId(id)) {
            source.sendFailure(Component.literal("ID inválido. Usa letras, números, _, - o ."));
            return 0;
        }

        boolean created = MANAGER.createSequence(source.getServer(), id);

        if (!created) {
            source.sendFailure(Component.literal("Ya existe la secuencia: " + id));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Secuencia creada: " + id), false);
        return 1;
    }

    private static int removeSequence(CommandSourceStack source, String id) {
        boolean removed = MANAGER.removeSequence(source.getServer(), id);

        if (!removed) {
            source.sendFailure(Component.literal("No existe la secuencia: " + id));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Secuencia eliminada: " + id), false);
        return 1;
    }

    private static int clearSequence(CommandSourceStack source, String id) {
        boolean cleared = MANAGER.clearSequence(source.getServer(), id);

        if (!cleared) {
            source.sendFailure(Component.literal("No existe la secuencia: " + id));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Secuencia limpiada: " + id), false);
        return 1;
    }

    private static int undoSequence(CommandSourceStack source, String id) {
        CamServerManager.UndoResult result = MANAGER.undoSequence(source.getServer(), id);

        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(result.message()), false);
        return 1;
    }

    private static int listSequences(CommandSourceStack source) {
        Collection<String> ids = MANAGER.sequenceIds(source.getServer());

        if (ids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No hay secuencias guardadas."), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Secuencias: " + String.join(", ", ids)), false);
        return ids.size();
    }

    private static int addHold(CommandSourceStack source, String seq, String point, int seconds) {
        boolean ok = MANAGER.addHold(source.getServer(), seq, point, seconds);

        if (!ok) {
            source.sendFailure(Component.literal("No se pudo agregar. Revisa que exista el punto: " + point));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Agregado HOLD de " + seconds + "s en " + point + " a " + seq), false);
        return 1;
    }

    private static int addMove(CommandSourceStack source, String seq, String from, String to, int seconds, String easing) {
        boolean ok = MANAGER.addMove(source.getServer(), seq, from, to, seconds, easing);

        if (!ok) {
            source.sendFailure(Component.literal("No se pudo agregar. Revisa que existan los puntos."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Agregado MOVE de " + from + " a " + to + " en " + seconds + "s con " + CameraEasing.safe(easing).name()), false);
        return 1;
    }

    private static int addCut(CommandSourceStack source, String seq, String point) {
        boolean ok = MANAGER.addCut(source.getServer(), seq, point);

        if (!ok) {
            source.sendFailure(Component.literal("No se pudo agregar. Revisa que exista el punto: " + point));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Agregado CUT a " + point + " en " + seq), false);
        return 1;
    }

    private static int addViewHere(CommandSourceStack source, String seq, String point, int seconds, float fov) throws CommandSyntaxException {
        if (!MANAGER.isValidId(point)) {
            source.sendFailure(Component.literal("ID de punto inválido."));
            return 0;
        }

        ServerPlayer player = source.getPlayerOrException();
        MANAGER.addViewHere(source.getServer(), player, seq, point, seconds, fov);

        source.sendSuccess(() -> Component.literal("Vista actual guardada como " + point + " y agregada quieta a " + seq), false);
        return 1;
    }

    private static int moveViewHere(CommandSourceStack source, String seq, String point, int seconds, float fov, String easing) throws CommandSyntaxException {
        if (!MANAGER.isValidId(point)) {
            source.sendFailure(Component.literal("ID de punto inválido."));
            return 0;
        }

        ServerPlayer player = source.getPlayerOrException();
        MANAGER.moveViewHere(source.getServer(), player, seq, point, seconds, fov, easing);

        source.sendSuccess(() -> Component.literal("Vista actual guardada como " + point + " y agregada como movimiento a " + seq + " con " + CameraEasing.safe(easing).name()), false);
        return 1;
    }

    private static int playPoint(CommandSourceStack source, String pointId, int seconds, Collection<ServerPlayer> targets) {
        CamServerManager.PlayResult result = MANAGER.playPoint(source.getServer(), pointId, seconds, targets);

        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(result.message()), false);
        return result.sent();
    }

    private static int showSequencePath(CommandSourceStack source, String sequenceId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CamServerManager.PathResult result = MANAGER.showSequencePath(source.getServer(), player, sequenceId);

        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(result.message()), false);
        return 1;
    }

    private static int hideSequencePath(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean wasVisible = MANAGER.hideSequencePath(player);

        if (wasVisible) {
            source.sendSuccess(() -> Component.literal("Trayectoria de cámara oculta."), false);
        } else {
            source.sendSuccess(() -> Component.literal("No tenías ninguna trayectoria visible."), false);
        }

        return 1;
    }

    private static int previewSequence(CommandSourceStack source, String sequenceId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CamServerManager.PlayResult result = MANAGER.previewSequence(source.getServer(), sequenceId, player);

        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Preview iniciado solo para ti: " + sequenceId), false);
        return result.sent();
    }

    private static int playSequence(CommandSourceStack source, String sequenceId, Collection<ServerPlayer> targets) {
        CamServerManager.PlayResult result = MANAGER.playSequence(source.getServer(), sequenceId, targets);

        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(result.message()), false);
        return result.sent();
    }

    private static int stop(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int stopped = MANAGER.stop(targets);

        source.sendSuccess(() -> Component.literal("Cinemática detenida para " + stopped + " jugador(es)."), false);
        return stopped;
    }

    private static Collection<ServerPlayer> defaultTargets(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        return players;
    }
}
