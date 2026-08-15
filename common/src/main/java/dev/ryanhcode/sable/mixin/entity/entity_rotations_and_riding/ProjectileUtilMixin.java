package dev.ryanhcode.sable.mixin.entity.entity_rotations_and_riding;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/**
 * Makes attack/projectile picking respect surface-oriented entities.
 * <p>
 * Entity picking clips the look ray against the target's world-aligned AABB, but a
 * surface-oriented entity renders tilted — aiming at the visible model can miss the box. AABBs
 * cannot rotate, so instead the ray is counter-rotated into the entity's frame around its feet
 * pivot (matching the render pivot), clipped against the plain box, and the hit transformed back:
 * geometrically identical to picking an oriented box.
 */
@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {

    @WrapOperation(
            method = {
                    "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
                    "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;"))
    private static Optional<Vec3> sable$orientedClip(final AABB box, final Vec3 from, final Vec3 to,
                                                     final Operation<Optional<Vec3>> original,
                                                     @Local(ordinal = 1) final Entity target) {
        final Quaterniondc orientation = target == null ? null
                : EntitySubLevelUtil.getCustomEntityOrientation(target, 1.0f);
        if (orientation == null) {
            return original.call(box, from, to);
        }

        final Vec3 pivot = target.position();
        final Vector3d localFrom = orientation.transformInverse(new Vector3d(from.x - pivot.x, from.y - pivot.y, from.z - pivot.z));
        final Vector3d localTo = orientation.transformInverse(new Vector3d(to.x - pivot.x, to.y - pivot.y, to.z - pivot.z));

        return original.call(box,
                        new Vec3(localFrom.x + pivot.x, localFrom.y + pivot.y, localFrom.z + pivot.z),
                        new Vec3(localTo.x + pivot.x, localTo.y + pivot.y, localTo.z + pivot.z))
                .map(hit -> {
                    final Vector3d world = orientation.transform(new Vector3d(hit.x - pivot.x, hit.y - pivot.y, hit.z - pivot.z));
                    return new Vec3(world.x + pivot.x, world.y + pivot.y, world.z + pivot.z);
                });
    }
}
