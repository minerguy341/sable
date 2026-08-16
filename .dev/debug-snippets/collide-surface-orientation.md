# Collision debug ledger — surface-oriented entities

Per-tick diagnostic for the surface-orientation force model (entities adopting the tilt of a
rotated sub-level). Stripped from `feature/entity-surface-orientation` before upstreaming because
it logs every tick per surface-oriented player; archived here because it is the fastest way to see
what the force model is actually doing when the tilt behaviour needs another pass.

## Provenance

- Added in `4798088` ("WIP: surface-orientation force model, picking, lighting (with debug ledgers)")
- Still present at `907d073`, the tip of `feature/entity-surface-orientation`
- A companion light-probe ledger lived in `EntityRendererMixin` and was already removed in `907d073`

## What it prints

| Field | Meaning |
| --- | --- |
| `in` | `collisionMotion` as handed to `collide` |
| `out` | `collisionInfo.motion` after resolution — the difference from `in` is what the solver absorbed |
| `inh` | `collisionInfo.inheritedMotion` — motion carried from the sub-level's own movement |
| `dm` | the entity's `getDeltaMovement()` at resolution time |
| `up` | `sink.entityUpDirection` — the surface normal the entity is oriented to |
| `vBelow` | `collisionInfo.verticalCollisionBelow` |
| `ground` | `entity.onGround()` |

Read `up` together with `in`/`out` to tell a genuine absorption from a tangential leak: on a stable
stand the tangential (perpendicular-to-`up`) part of `out` should be ~0, and `ground` should stay
`true` every tick. A `ground` that flickers false is the fall-catch-slide cycle described in
`LivingEntityMixin.sable$noGravityOnTiltedGround`.

## Where it goes

`common/src/main/java/dev/ryanhcode/sable/sublevel/entity_collision/SubLevelEntityCollision.java`,
in `collide`, immediately after `collisionInfo.firstCollisions` is assigned and before
`return collisionInfo;`.

No import changes needed — `dev.ryanhcode.sable.Sable`, `net.minecraft.world.entity.player.Player`
and `net.minecraft.world.phys.Vec3` are already imported by that file.

```java
        // TEMP diagnostics for surface-orientation force debugging — remove before merging.
        if (entity.level().isClientSide && entity instanceof Player && customEntityOrientation != null) {
            final Vec3 dm = entity.getDeltaMovement();
            Sable.LOGGER.info(String.format(
                    "[collide dbg] in=(%.5f,%.5f,%.5f) out=(%.5f,%.5f,%.5f) inh=%s dm=(%.5f,%.5f,%.5f) up=(%.3f,%.3f,%.3f) vBelow=%b ground=%b",
                    collisionMotionMoj.x, collisionMotionMoj.y, collisionMotionMoj.z,
                    collisionInfo.motion.x, collisionInfo.motion.y, collisionInfo.motion.z,
                    collisionInfo.inheritedMotion,
                    dm.x, dm.y, dm.z,
                    sink.entityUpDirection.x, sink.entityUpDirection.y, sink.entityUpDirection.z,
                    collisionInfo.verticalCollisionBelow,
                    entity.onGround()));
        }
```

## Local variables it depends on

All are in scope at that point in `collide` as of `76e9ae7`; if the method is refactored, re-check
`collisionMotionMoj`, `customEntityOrientation`, `sink.entityUpDirection` and `collisionInfo`
before assuming the block still compiles.
