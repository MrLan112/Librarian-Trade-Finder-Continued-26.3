package de.greenman999.librariantradefinder;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class RotationTools {
    private RotationTools() {}

    public static boolean isRotated = false;
    private static float destPitch = 0;
    private static float destYaw = 0;
    private static float startPitch = 0;
    private static float startYaw = 0;
    private static float speed = 0;
    private static float elapsedTime = 0;
    private static long prevTimeMillis = 0;

    public static void smoothLookAt(Vec3 target, float speed) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Vec3 eyePosition = EntityAnchorArgument.Anchor.EYES.apply(mc.player);
        double dx = target.x - eyePosition.x;
        double dy = target.y - eyePosition.y;
        double dz = target.z - eyePosition.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        destPitch = Mth.wrapDegrees((float) (-(Mth.atan2(dy, horizontalDistance) * 57.2957763671875)));
        destYaw = Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * 57.2957763671875) - 90.0F);
        startPitch = mc.player.getXRot();
        startYaw = mc.player.getYRot();
        if (destPitch == startPitch && destYaw == startYaw) return;
        RotationTools.speed = speed / 10f;
        elapsedTime = 0;
        prevTimeMillis = System.currentTimeMillis();
        isRotated = false;
    }

    public static void render() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || isRotated || speed == 0) return;
        long now = System.currentTimeMillis();
        float delta = (now - (prevTimeMillis == 0 ? now : prevTimeMillis)) / 1000f;
        prevTimeMillis = now;
        elapsedTime += delta;
        float percentageComplete = elapsedTime / speed;
        if (percentageComplete >= 1) {
            isRotated = true;
            speed = 0;
            return;
        }
        mc.player.setXRot(Mth.lerp(percentageComplete, startPitch, destPitch));
        mc.player.setYRot(Mth.lerp(percentageComplete, startYaw, destYaw));
    }
}
