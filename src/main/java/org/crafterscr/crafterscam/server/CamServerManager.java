package org.crafterscr.crafterscam.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.crafterscr.crafterscam.camera.*;
import org.crafterscr.crafterscam.network.NetCameraSegment;
import org.crafterscr.crafterscam.network.StartCinematicPayload;
import org.crafterscr.crafterscam.network.StopCinematicPayload;

import java.util.*;

public class CamServerManager {
    public static final CamServerManager INSTANCE = new CamServerManager();

    private final CamStorage storage = new CamStorage();

    private CamServerManager() {
    }

    public Collection<String> pointIds(MinecraftServer server) {
        return new ArrayList<>(storage.data(server).points.keySet());
    }

    public Collection<String> sequenceIds(MinecraftServer server) {
        return new ArrayList<>(storage.data(server).sequences.keySet());
    }

    public boolean isValidId(String id) {
        return id != null && id.matches("[a-zA-Z0-9_\\-.]+");
    }

    public void savePoint(MinecraftServer server, ServerPlayer player, String id, float fov) {
        CamStorage.CamData data = storage.data(server);
        CameraPoint point = CameraPoint.fromPlayer(id, player, fov);
        data.points.put(id, point);
        storage.save(server);
    }

    public boolean removePoint(MinecraftServer server, String id) {
        CamStorage.CamData data = storage.data(server);
        boolean removed = data.points.remove(id) != null;

        if (removed) {
            storage.save(server);
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
        return true;
    }

    public boolean removeSequence(MinecraftServer server, String id) {
        CamStorage.CamData data = storage.data(server);
        boolean removed = data.sequences.remove(id) != null;

        if (removed) {
            storage.save(server);
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
        return true;
    }

    public boolean addHold(MinecraftServer server, String sequenceId, String pointId, int seconds) {
        CamStorage.CamData data = storage.data(server);

        if (!data.points.containsKey(pointId)) {
            return false;
        }

        CameraSequence sequence = data.sequences.computeIfAbsent(sequenceId, CameraSequence::new);
        sequence.steps.add(CameraStep.hold(pointId, seconds));
        storage.save(server);
        return true;
    }

    public boolean addMove(MinecraftServer server, String sequenceId, String fromPoint, String toPoint, int seconds) {
        CamStorage.CamData data = storage.data(server);

        if (!data.points.containsKey(fromPoint) || !data.points.containsKey(toPoint)) {
            return false;
        }

        CameraSequence sequence = data.sequences.computeIfAbsent(sequenceId, CameraSequence::new);
        sequence.steps.add(CameraStep.move(fromPoint, toPoint, seconds));
        storage.save(server);
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
        return true;
    }

    public boolean addViewHere(MinecraftServer server, ServerPlayer player, String sequenceId, String pointId, int seconds, float fov) {
        savePoint(server, player, pointId, fov);
        return addHold(server, sequenceId, pointId, seconds);
    }

    public boolean moveViewHere(MinecraftServer server, ServerPlayer player, String sequenceId, String pointId, int seconds, float fov) {
        CamStorage.CamData data = storage.data(server);
        CameraSequence sequence = data.sequences.computeIfAbsent(sequenceId, CameraSequence::new);

        String previousPoint = sequence.getLastPointId();

        savePoint(server, player, pointId, fov);

        if (previousPoint == null || !data.points.containsKey(previousPoint)) {
            sequence.steps.add(CameraStep.hold(pointId, seconds));
        } else {
            sequence.steps.add(CameraStep.move(previousPoint, pointId, seconds));
        }

        storage.save(server);
        return true;
    }

    public PlayResult playPoint(MinecraftServer server, String pointId, int seconds, Collection<ServerPlayer> targets) {
        CamStorage.CamData data = storage.data(server);
        CameraPoint point = data.points.get(pointId);

        if (point == null) {
            return PlayResult.fail("No existe el punto: " + pointId);
        }

        NetCameraSegment segment = NetCameraSegment.hold(point.toNetwork(), Math.max(1, seconds * 20));
        return sendToCompatibleTargets(point.dimension, List.of(segment), targets);
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

        return sendToCompatibleTargets(requiredDimension, networkSegments, targets);
    }

    public int stop(Collection<ServerPlayer> targets) {
        int count = 0;

        for (ServerPlayer player : targets) {
            PacketDistributor.sendToPlayer(player, StopCinematicPayload.INSTANCE);
            count++;
        }

        return count;
    }

    private PlayResult sendToCompatibleTargets(String requiredDimension, List<NetCameraSegment> segments, Collection<ServerPlayer> targets) {
        int sent = 0;

        for (ServerPlayer player : targets) {
            String playerDimension = player.level().dimension().location().toString();

            if (!Objects.equals(requiredDimension, playerDimension)) {
                continue;
            }

            PacketDistributor.sendToPlayer(player, new StartCinematicPayload(segments));
            sent++;
        }

        if (sent == 0) {
            return PlayResult.fail("No se envió a nadie. Revisa que los jugadores estén en la misma dimensión que la cámara.");
        }

        return PlayResult.ok(sent);
    }

    public record PlayResult(boolean success, int sent, String message) {
        public static PlayResult ok(int sent) {
            return new PlayResult(true, sent, "Cinemática enviada a " + sent + " jugador(es).");
        }

        public static PlayResult fail(String message) {
            return new PlayResult(false, 0, message);
        }
    }
}