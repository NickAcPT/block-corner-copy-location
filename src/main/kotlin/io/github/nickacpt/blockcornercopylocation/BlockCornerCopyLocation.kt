package io.github.nickacpt.blockcornercopylocation

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.block.ShapeContext
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import kotlin.math.round

object BlockCornerCopyLocation : ModInitializer {
    private fun doCopyLocation(player: ClientPlayerEntity): Text? {
        val raycast = player.raycast(10.0, 0f, false)

        if (raycast.type != HitResult.Type.BLOCK || raycast !is BlockHitResult) return Text.translatable("block-corner-copy-location.error.not-looking-at-block")
        val block = player.entityWorld.getBlockState(raycast.blockPos)
        val shape = block.getCollisionShape(player.entityWorld, raycast.blockPos, ShapeContext.of(player))
        val boundingBoxes = shape.boundingBoxes.map { it.offset(raycast.blockPos) }

        if (boundingBoxes.isEmpty()) return Text.translatable("block-corner-copy-location.error.not-looking-at-block")

        val raycastPos = raycast.pos
        val closest = boundingBoxes.map { (minX, minY, minZ, maxX, maxY, maxZ) ->
            DoublePosition(
                    raycastPos.x.coerceIn(minX, maxX),
                    raycastPos.y.coerceIn(minY, maxY),
                    raycastPos.z.coerceIn(minZ, maxZ)
            )
        }.minBy { (x, y, z) ->
            raycastPos.squaredDistanceTo(x, y, z)
        }

        val x = closest.x.roundToNearestHalf()
        val y = closest.y.roundToNearestHalf()
        val z = closest.z.roundToNearestHalf()

        MinecraftClient.getInstance().keyboard.clipboard = arrayOf(x, y, z).joinToString(" ")

        return Text.translatable("block-corner-copy-location.success", x, y, z)
    }

    override fun onInitialize() {
        val copyLocationKey = KeyBindingHelper.registerKeyBinding(
                KeyBinding(
                        "key.block-corner-copy-location.copy-location",
                        InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_X, "category.block-corner-copy-location"
                )
        )

        ClientTickEvents.END_CLIENT_TICK.register {
            if (copyLocationKey.wasPressed()) {
                doCopyLocation(it.player ?: return@register)?.let { text ->
                    it.player?.sendMessage(text, false)
                }
            }
        }
    }
}

fun Double.roundToNearestHalf(): String = String.format("%.2f", round(this * 2) / 2).trimEnd('0', '.')

operator fun Box.component1() = minX
operator fun Box.component2() = minY
operator fun Box.component3() = minZ
operator fun Box.component4() = maxX
operator fun Box.component5() = maxY
operator fun Box.component6() = maxZ

data class DoublePosition(val x: Double, val y: Double, val z: Double)