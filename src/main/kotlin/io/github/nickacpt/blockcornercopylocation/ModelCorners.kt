package io.github.nickacpt.blockcornercopylocation

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart
import net.minecraft.client.resources.model.geometry.BakedQuad
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import java.math.BigDecimal

private val cullFaces = arrayOf<Direction?>(null, *Direction.values())

/** Hit coordinates and returned vertices are in model-local space. */
internal fun nearestModelCorner(
    parts: List<BlockStateModelPart>, hitX: Double, hitY: Double, hitZ: Double
): Vec3? {
    var bestDistance = Double.POSITIVE_INFINITY
    var bestX = 0.0
    var bestY = 0.0
    var bestZ = 0.0
    for (part in parts) {
        // Include both unculled quads and all six directional quad lists.
        for (face in cullFaces) {
            for (quad in part.getQuads(face)) {
                for (index in 0 until BakedQuad.VERTEX_COUNT) {
                    val vertex = quad.position(index)
                    val x = vertex.x().toDouble()
                    val y = vertex.y().toDouble()
                    val z = vertex.z().toDouble()
                    val dx = x - hitX
                    val dy = y - hitY
                    val dz = z - hitZ
                    val distance = dx * dx + dy * dy + dz * dz
                    if (distance < bestDistance) {
                        bestDistance = distance
                        bestX = x
                        bestY = y
                        bestZ = z
                    }
                }
            }
        }
    }
    return if (bestDistance.isFinite()) Vec3(bestX, bestY, bestZ) else null
}

// Locale-independent, no grid snapping or loss of model precision.
internal fun formatCoordinate(value: Double): String =
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
