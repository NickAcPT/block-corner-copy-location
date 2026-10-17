package io.github.nickacpt.blockcornercopylocation

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

object BlockCornerCopyLocation : ClientModInitializer {
    private val random = RandomSource.createThreadLocalInstance(0)
    private val parts = ArrayList<BlockStateModelPart>()
    private val cornerDust = DustParticleOptions(DustParticleOptions.REDSTONE_PARTICLE_COLOR, 0.25f)

    private fun doCopyLocation(client: Minecraft, player: LocalPlayer): Component {
        val level = client.level
            ?: return Component.translatable("block-corner-copy-location.error.not-looking-at-block")
        val hit = player.pick(10.0, 1f, false)
        if (hit !is BlockHitResult || hit.type != HitResult.Type.BLOCK) {
            return Component.translatable("block-corner-copy-location.error.not-looking-at-block")
        }

        val pos = hit.blockPos
        val state = level.getBlockState(pos)
        if (state.renderShape != RenderShape.MODEL) {
            return Component.translatable("block-corner-copy-location.error.no-corners-found")
        }

        // Match the renderer's variant selection and positional offset (e.g. flowers).
        random.setSeed(state.getSeed(pos))
        parts.clear()
        val corner = try {
            client.modelManager.blockStateModelSet.get(state).collectParts(random, parts)
            val offset = state.getOffset(pos)
            nearestModelCorner(
                parts,
                hit.location.x - pos.x - offset.x,
                hit.location.y - pos.y - offset.y,
                hit.location.z - pos.z - offset.z
            )?.add(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z)
        } finally {
            parts.clear()
        } ?: return Component.translatable("block-corner-copy-location.error.no-corners-found")

        val x = formatCoordinate(corner.x)
        val y = formatCoordinate(corner.y)
        val z = formatCoordinate(corner.z)
        client.keyboardHandler.clipboard = "$x $y $z"
        repeat(8) {
            client.particleEngine.createParticle(
                cornerDust, corner.x, corner.y, corner.z, 0.0, 0.0, 0.0
            )?.apply {
                // Dust randomizes its initial velocity even when given zero speed.
                setParticleSpeed(0.0, 0.0, 0.0)
                lifetime = 40
            }
        }
        return Component.translatable("block-corner-copy-location.success", x, y, z)
    }

    override fun onInitializeClient() {
        val copyLocationKey = KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                "key.block-corner-copy-location.copy-location",
                InputConstants.Type.KEYBOARD, InputConstants.KEY_X,
                KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath("block-corner-copy-location", "category")
                )
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register {
            if (copyLocationKey.consumeClick()) {
                val player = it.player ?: return@register
                player.sendSystemMessage(doCopyLocation(it, player))
            }
        }
    }
}
