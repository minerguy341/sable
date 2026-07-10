package dev.ryanhcode.sable.mixin.entity.entity_rotations_and_riding;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Block.class)
public class BlockMixin {

    @Redirect(method = "updateEntityAfterFallOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 sable$rotateWithEntity(final Vec3 instance, final double x, final double y, final double z, @Local(argsOnly = true) final Entity entity) {
        final Quaterniondc orientation = EntitySubLevelUtil.getCustomEntityOrientation(entity, 1.0f);

        if (orientation == null) {
            return instance.multiply(x, y, z);
        }

        final Vector3d up = orientation.transform(OrientedBoundingBox3d.UP, new Vector3d());
        final double dot = up.dot(instance.x, instance.y, instance.z);
        final Vec3 tangential = instance.subtract(up.x * dot, up.y * dot, up.z * dot);

        // Surface adhesion: removing only the surface-normal component leaves the tangential
        // slice of world-frame gravity (~g*sin(tilt)) in the velocity every tick, making entities
        // creep along inclines with no input. Landing/standing motion below a walking-impulse
        // threshold is absorbed entirely, like vanilla's multiply(1, 0, 1) absorbs vertical falls;
        // deliberate movement is well above the threshold and passes through.
        if (tangential.lengthSqr() < 0.04 * 0.04) {
            return Vec3.ZERO;
        }

        return tangential;
    }

}
