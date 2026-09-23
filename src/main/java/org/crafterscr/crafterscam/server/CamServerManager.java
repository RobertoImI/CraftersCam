package org.crafterscr.crafterscam.server;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.crafterscr.crafterscam.camera.*;
import org.crafterscr.crafterscam.network.CameraPathPayload;
import org.crafterscr.crafterscam.network.NetCameraSegment;
import org.crafterscr.crafterscam.network.StartCinematicPayload;
import org.crafterscr.crafterscam.network.StopCinematicPayload;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import java.util.*;

public class CamServerManager {
    public static final CamServerManager INSTANCE = new CamServerManager();

    private final CamStorage storage = new CamStorage();

    private final Set<UUID> showAllViewers = new HashSet<>();
    private final Map<UUID, String> sequencePathViewers = new HashMap<>();
    private int showTickCounter = 0;

    private final Map<UUID, MovementLock> movementLocks = new HashMap<>();

    private CamServerManager() {
    }

    public Collection<String> pointIds(MinecraftServer server) {
        return new ArrayList<>(storage.data(server).points.keySet());
    }

    public Collection<String> sequenceIds(MinecraftServer server) {
        return new ArrayList<>(storage.data(server).sequences.keySet());
    }

    public CamVisualSettings settings(MinecraftServer server) {
        return storage.data(server).settings;
    }

    public void setFade(MinecraftServer server, int fadeInSeconds, int fadeOutSeconds) {
        CamVisualSettings settings = storage.data(server).settings;
        settings.fadeInTicks = Math.max(0, fadeInSeconds * 20);
        settings.fadeOutTicks = Math.max(0, fadeOutSeconds * 20);
        storage.save(server);
        refreshPathViewers(server);
    }

    public void setBars(MinecraftServer server, boolean enabled) {
        storage.data(server).settings.showBars = enabled;
        storage.save(server);
        refreshPathViewers(server);
    }

    public void setHideHudAll(MinecraftServer server, boolean enabled) {
        storage.data(server).settings.hideHudAll = enabled;
        storage.save(server);
        refreshPathViewers(server);
    }

    public void setAllowMovement(MinecraftServer server, boolean enabled) {
        storage.data(server).settings.allowMovement = enabled;
        storage.save(server);
        refreshPathViewers(server);
    }

    public void setShowAllPoints(ServerPlayer player, boolean enabled) {
        if (enabled) {
            showAllViewers.add(player.getUUID());
        } else {
            showAllViewers.remove(player.getUUID());
        }
    }

    public boolean isShowingAllPoints(ServerPlayer player) {
        return showAllViewers.contains(player.getUUID());
    }

    public PathResult showSequencePath(MinecraftServer server, ServerPlayer player, String sequenceId) {
        PathDataResult pathData = buildPathData(server, sequenceId);

        if (!pathData.success()) {
            return PathResult.fail(pathData.message());
        }

        String playerDimension = player.level().dimension().location().toString();

        if (!Objects.equals(playerDimension, pathData.dimension())) {
            return PathResult.fail("La secuencia está en " + pathData.dimension() + ". Ve a esa dimensión para visualizarla.");
        }

        sequencePathViewers.put(player.getUUID(), sequenceId);
        PacketDistributor.sendToPlayer(player, CameraPathPayload.show(sequenceId, pathData.dimension(), pathData.segments()));

        return PathResult.ok("Mostrando trayectoria de '" + sequenceId + "' solo para ti. MOVE = línea continua; CUT/saltos = línea discontinua.");
    }

    public boolean hideSequencePath(ServerPlayer player) {
        boolean removed = sequencePathViewers.remove(player.getUUID()) != null;
        PacketDistributor.sendToPlayer(player, CameraPathPayload.hidden());
        return removed;
    }

    public PlayResult previewSequence(MinecraftServer server, String sequenceId, ServerPlayer player) {
        return playSequence(server, sequenceId, List.of(player));
    }

    public void tickShowPoints(MinecraftServer server) {
        tickMovementLocks(server);
        cleanupPathViewers(server);

        if (showAllViewers.isEmpty()) {
            return;
        }

        showTickCounter++;

        if (showTickCounter < 8) {
            return;
        }

        showTickCounter = 0;

        Iterator<UUID> iterator = showAllViewers.iterator();

        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);

            if (player == null || player.hasDisconnected()) {
                iterator.remove();
                continue;
            }

            showPointsToPlayer(server, player);
        }
    }

    private void showPointsToPlayer(MinecraftServer server, ServerPlayer player) {
        CamStorage.CamData data = storage.data(server);

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        String playerDimension = player.level().dimension().location().toString();

        for (CameraPoint point : data.points.values()) {
            if (!Objects.equals(point.dimension, playerDimension)) {
                continue;
            }

            // Partícula principal donde está la cámara.
            level.sendParticles(
                    player,
                    ParticleTypes.END_ROD,
                    true,
                    point.x,
                    point.y,
                    point.z,
                    2,
                    0.05D,
                    0.05D,
                    0.05D,
                    0.01D
            );

            // Línea indicando hacia dónde mira la cámara.
            Vec3 direction = Vec3.directionFromRotation(point.pitch, point.yaw).normalize();

            for (int i = 1; i <= 8; i++) {
                double distance = i * 0.35D;

                level.sendParticles(
                        player,
                        ParticleTypes.ELECTRIC_SPARK,
                        true,
                        point.x + direction.x * distance,
                        point.y + direction.y * distance,
                        point.z + direction.z * distance,
                        1,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D
                );
            }
        }
    }

    public boolean isValidId(String id) {
        return id != null && id.matches("[a-zA-Z0-9_\\-.]+");
    }

    public void savePoint(MinecraftServer server, ServerPlayer player, String id, float fov) {
        CamStorage.CamData data = storage.data(server);
        CameraPoint point = CameraPoint.fromPlayer(id, player, fov);
        data.points.put(id, point);
        storage.save(server);
        refreshPathViewers(server);
    }

    public boolean removePoint(MinecraftServer server, String id) {
        CamStorage.CamData data = storage.data(server);
        boolean removed = data.points.remove(id) != null;

        if (removed) {
            storage.save(server);
        refreshPathViewers(server);
        }

        return removed;
    }

    public boolean createSequence(MinecraftServer server, String id) {
        CamStorage.CamData data = storage.data(server);

        if (data.sequences.containsKey(id)) {
            return false;
        }

        data.sequences.put(id, new CameraSequence(id));
        storage.save(server);
        refreshPathViewers(server);
        return true;
    }

    public boolean removeSequence(MinecraftServer server, String id) {
        CamStorage.CamData data = storage.data(server);
        boolean removed = data.sequences.remove(id) != null;

        if (removed) {
            storage.save(server);
        refreshPathViewers(server);
        }

        return removed;
    }

    public boolean clearSequence(MinecraftServer server, String id) {
        CamStorage.CamData data = storage.data(server);
        CameraSequence sequence = data.sequences.get(id);

        if (sequence == null) {
            return false;
        }

        sequence.steps.clear();
        storage.save(server);
        refreshPathViewers(server);
        return true;
    }

    public UndoResult undoSequence(MinecraftServer server, String id) {
        CamStorage.CamData data = storage.data(server);
        CameraSequence sequence = data.sequences.get(id);

        if (sequence == null) {
            return UndoResult.fail("No existe la secuencia: " + id);
        }

        if (sequence.steps.isEmpty()) {
            return UndoResult.fail("La secuencia ya está vacía: " + id);
        }

        CameraStep removed = sequence.steps.remove(sequence.steps.size() - 1);
        storage.save(server);
        refreshPathViewers(server);

        return UndoResult.ok("Se borró el último paso: " + removed.type + " " + removed.from + " -> " + removed.to);
    }

    public boolean addHold(MinecraftServer server, String sequenceId, String pointId, int seconds) {
        CamStorage.CamData data = storage.data(server);

        if (!data.points.containsKey(pointId)) {
            return false;
        }

        CameraSequence sequence = data.sequences.computeIfAbsent(sequenceId, CameraSequence::new);
        sequence.steps.add(CameraStep.hold(pointId, seconds));
        storage.save(server);
        refreshPathViewers(server);
        return true;
    }

    public boolean addMove(MinecraftServer server, String sequenceId, String fromPoint, String toPoint, int seconds, String easing) {
        CamStorage.CamData data = storage.data(server);

        if (!data.points.containsKey(fromPoint) || !data.points.containsKey(toPoint)) {
            return false;
        }

        CameraSequence sequence = data.sequences.computeIfAbsent(sequenceId, CameraSequence::new);
        sequence.steps.add(CameraStep.move(fromPoint, toPoint, seconds, easing));
        storage.save(server);
        refreshPathViewers(server);
        return true;
    }

    public boolean addCut(MinecraftServer server, String sequenceId, String pointId) {
        CamStorage.CamData data = storage.data(server);

        if (!data.points.containsKey(pointId)) {
            return false;
        }

        CameraSequence sequence = data.sequences.computeIfAbsent(sequenceId, CameraSequence::new);
        sequence.steps.add(CameraStep.cut(pointId));
        storage.save(server);
        refreshPathViewers(server);
        return true;
    }

    public boolean addViewHere(MinecraftServer server, ServerPlayer player, String sequenceId, String pointId, int seconds, float fov) {
        savePoint(server, player, pointId, fov);
        return addHold(server, sequenceId, pointId, seconds);
    }

    public boolean moveViewHere(MinecraftServer server, ServerPlayer player, String sequenceId, String pointId, int seconds, float fov, String easing) {
        CamStorage.CamData data = storage.data(server);
        CameraSequence sequence = data.sequences.computeIfAbsent(sequenceId, CameraSequence::new);

        String previousPoint = sequence.getLastPointId();

        savePoint(server, player, pointId, fov);

        if (previousPoint == null || !data.points.containsKey(previousPoint)) {
            sequence.steps.add(CameraStep.hold(pointId, seconds));
        } else {
            sequence.steps.add(CameraStep.move(previousPoint, pointId, seconds, easing));
        }

        storage.save(server);
        refreshPathViewers(server);
        return true;
    }

    public PlayResult playPoint(MinecraftServer server, String pointId, int seconds, Collection<ServerPlayer> targets) {
        CamStorage.CamData data = storage.data(server);
        CameraPoint point = data.points.get(pointId);

        if (point == null) {
            return PlayResult.fail("No existe el punto: " + pointId);
        }

        NetCameraSegment segment = NetCameraSegment.hold(point.toNetwork(), Math.max(1, seconds * 20));
        return sendToCompatibleTargets(server, point.dimension, List.of(segment), targets);
    }

    public PlayResult playSequence(MinecraftServer server, String sequenceId, Collection<ServerPlayer> targets) {
        CamStorage.CamData data = storage.data(server);
        CameraSequence sequence = data.sequences.get(sequenceId);

        if (sequence == null) {
            return PlayResult.fail("No existe la secuencia: " + sequenceId);
        }

        if (sequence.steps.isEmpty()) {
            return PlayResult.fail("La secuencia está vacía: " + sequenceId);
        }

        List<NetCameraSegment> networkSegments = new ArrayList<>();
        String requiredDimension = null;

        for (CameraStep step : sequence.steps) {
            StepType type = StepType.safe(step.type);
            CameraEasing easing = CameraEasing.safe(step.easing);

            CameraPoint from = data.points.get(step.from);
            CameraPoint to = data.points.get(type == StepType.MOVE ? step.to : step.from);

            if (from == null) {
                return PlayResult.fail("Falta el punto: " + step.from);
            }

            if (to == null) {
                return PlayResult.fail("Falta el punto: " + step.to);
            }

            if (requiredDimension == null) {
                requiredDimension = from.dimension;
            }

            if (!Objects.equals(requiredDimension, from.dimension) || !Objects.equals(requiredDimension, to.dimension)) {
                return PlayResult.fail("Todos los puntos de una secuencia deben estar en la misma dimensión.");
            }

            networkSegments.add(new NetCameraSegment(
                    type.networkId(),
                    from.toNetwork(),
                    to.toNetwork(),
                    Math.max(1, step.durationTicks),
                    easing.networkId()
            ));
        }

        return sendToCompatibleTargets(server, requiredDimension, networkSegments, targets);
    }

    private PathDataResult buildPathData(MinecraftServer server, String sequenceId) {
        CamStorage.CamData data = storage.data(server);
        CameraSequence sequence = data.sequences.get(sequenceId);

        if (sequence == null) {
            return PathDataResult.fail("No existe la secuencia: " + sequenceId);
        }

        if (sequence.steps.isEmpty()) {
            return PathDataResult.fail("La secuencia está vacía: " + sequenceId);
        }

        List<NetCameraSegment> networkSegments = new ArrayList<>();
        String requiredDimension = null;

        for (CameraStep step : sequence.steps) {
            StepType type = StepType.safe(step.type);
            CameraEasing easing = CameraEasing.safe(step.easing);

            CameraPoint from = data.points.get(step.from);
            CameraPoint to = data.points.get(type == StepType.MOVE ? step.to : step.from);

            if (from == null) {
                return PathDataResult.fail("Falta el punto: " + step.from);
            }

            if (to == null) {
                return PathDataResult.fail("Falta el punto: " + step.to);
            }

            if (requiredDimension == null) {
                requiredDimension = from.dimension;
            }

            if (!Objects.equals(requiredDimension, from.dimension) || !Objects.equals(requiredDimension, to.dimension)) {
                return PathDataResult.fail("Todos los puntos de una secuencia deben estar en la misma dimensión.");
            }

            networkSegments.add(new NetCameraSegment(
                    type.networkId(),
                    from.toNetwork(),
                    to.toNetwork(),
                    Math.max(1, step.durationTicks),
                    easing.networkId()
            ));
        }

        return PathDataResult.ok(requiredDimension, networkSegments);
    }

    private void refreshPathViewers(MinecraftServer server) {
        if (sequencePathViewers.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, String>> iterator = sequencePathViewers.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, String> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

            if (player == null || player.hasDisconnected()) {
                iterator.remove();
                continue;
            }

            PathDataResult pathData = buildPathData(server, entry.getValue());

            if (!pathData.success()) {
                PacketDistributor.sendToPlayer(player, CameraPathPayload.hidden());
                iterator.remove();
                continue;
            }

            PacketDistributor.sendToPlayer(
                    player,
                    CameraPathPayload.show(entry.getValue(), pathData.dimension(), pathData.segments())
            );
        }
    }

    private void cleanupPathViewers(MinecraftServer server) {
        if (sequencePathViewers.isEmpty()) {
            return;
        }

        sequencePathViewers.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            return player == null || player.hasDisconnected();
        });
    }

    private record PathDataResult(boolean success, String dimension, List<NetCameraSegment> segments, String message) {
        private static PathDataResult ok(String dimension, List<NetCameraSegment> segments) {
            return new PathDataResult(true, dimension, List.copyOf(segments), "");
        }

        private static PathDataResult fail(String message) {
            return new PathDataResult(false, "", List.of(), message);
        }
    }

    public int stop(Collection<ServerPlayer> targets) {
        int count = 0;

        for (ServerPlayer player : targets) {
            PacketDistributor.sendToPlayer(player, StopCinematicPayload.INSTANCE);
            unlockMovement(player);
            count++;
        }

        return count;
    }

    private PlayResult sendToCompatibleTargets(MinecraftServer server, String requiredDimension, List<NetCameraSegment> segments, Collection<ServerPlayer> targets) {
        CamVisualSettings settings = settings(server);
        int sent = 0;

        for (ServerPlayer player : targets) {
            String playerDimension = player.level().dimension().location().toString();

            if (!Objects.equals(requiredDimension, playerDimension)) {
                continue;
            }

            PacketDistributor.sendToPlayer(player, new StartCinematicPayload(
                    segments,
                    settings.fadeInTicks,
                    settings.fadeOutTicks,
                    settings.showBars,
                    settings.hideHudAll,
                    settings.allowMovement
            ));

            if (!settings.allowMovement) {
                int totalTicks = 0;

                for (NetCameraSegment segment : segments) {
                    totalTicks += Math.max(1, segment.durationTicks());
                }

                // Margen extra para cubrir fade/sincronización.
                lockMovement(player, totalTicks + 40);
            } else {
                unlockMovement(player);
            }

            sent++;
        }

        if (sent == 0) {
            return PlayResult.fail("No se envió a nadie. Revisa que los jugadores estén en la misma dimensión que la cámara.");
        }

        return PlayResult.ok(sent);
    }

    private static class MovementLock {
        private final String dimension;
        private final double x;
        private final double z;
        private int ticksLeft;

        private MovementLock(String dimension, double x, double z, int ticksLeft) {
            this.dimension = dimension;
            this.x = x;
            this.z = z;
            this.ticksLeft = ticksLeft;
        }
    }

    public record PathResult(boolean success, String message) {
        public static PathResult ok(String message) {
            return new PathResult(true, message);
        }

        public static PathResult fail(String message) {
            return new PathResult(false, message);
        }
    }

    public record PlayResult(boolean success, int sent, String message) {
        public static PlayResult ok(int sent) {
            return new PlayResult(true, sent, "Cinemática enviada a " + sent + " jugador(es).");
        }

        public static PlayResult fail(String message) {
            return new PlayResult(false, 0, message);
        }
    }

    public record UndoResult(boolean success, String message) {
        public static UndoResult ok(String message) {
            return new UndoResult(true, message);
        }

        public static UndoResult fail(String message) {
            return new UndoResult(false, message);
        }
    }

    private void lockMovement(ServerPlayer player, int ticks) {
        movementLocks.put(
                player.getUUID(),
                new MovementLock(
                        player.level().dimension().location().toString(),
                        player.getX(),
                        player.getZ(),
                        Math.max(1, ticks)
                )
        );
    }

    private void unlockMovement(ServerPlayer player) {
        movementLocks.remove(player.getUUID());
    }

    private void tickMovementLocks(MinecraftServer server) {
        if (movementLocks.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, MovementLock>> iterator = movementLocks.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, MovementLock> entry = iterator.next();

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

            if (player == null || player.hasDisconnected()) {
                iterator.remove();
                continue;
            }

            MovementLock lock = entry.getValue();

            String currentDimension = player.level().dimension().location().toString();

            if (!Objects.equals(currentDimension, lock.dimension)) {
                iterator.remove();
                continue;
            }

            lock.ticksLeft--;

            if (lock.ticksLeft <= 0) {
                iterator.remove();
                continue;
            }

            Vec3 velocity = player.getDeltaMovement();

            // Mantiene Y libre para permitir salto/caída.
            player.setDeltaMovement(0.0D, velocity.y, 0.0D);

            double currentY = player.getY();

            // Mantiene X/Z bloqueado, pero deja la altura libre.
            if (Math.abs(player.getX() - lock.x) > 0.01D || Math.abs(player.getZ() - lock.z) > 0.01D) {
                player.teleportTo(lock.x, currentY, lock.z);
            }
        }
    }
}