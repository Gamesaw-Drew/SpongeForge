/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.mod.mixin.core.world.gen;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.interfaces.world.gen.IMixinChunkProviderServer;

@Mixin(value = ChunkProviderServer.class, priority = 1001)
public abstract class MixinChunkProviderServer implements IMixinChunkProviderServer {

    @Shadow @Final public WorldServer world;
    @Shadow @Final public Long2ObjectMap<Chunk> id2ChunkMap;
    @Shadow public abstract Chunk loadChunk(int x, int z);
    @Shadow public abstract void saveChunkExtraData(Chunk chunkIn);

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/ChunkProviderServer;saveChunkExtraData(Lnet/minecraft/world/chunk/Chunk;)V"))
    public void onSaveExtraChunkData(ChunkProviderServer chunkProviderServer, Chunk chunkIn) {
        this.saveChunkExtraData(chunkIn);
        net.minecraftforge.common.ForgeChunkManager.putDormantChunk(ChunkPos.asLong(chunkIn.xPosition, chunkIn.zPosition), chunkIn);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;remove()V", shift = Shift.AFTER, remap = false))
    public void onUnloadQueuedChunksReturn(CallbackInfoReturnable<Boolean> cir) {
        // Remove forge's persistent chunk check since we cache it in the chunk
        if (this.id2ChunkMap.size() == 0 && !this.world.provider.getDimensionType().shouldLoadSpawn()){
            net.minecraftforge.common.DimensionManager.unloadWorld(this.world.provider.getDimension());
        }
    }

    /**
     * @author Aaron1011 - January 28, 2017
     * @reason In SpongeVanilla, it's safe to run this method instead of loadChunk,
     * since the only modification made is the removal of a check we've already done.
     *
     * However, loadChunk is completely different in Forge. therefore, we need to delegate to
     * the original method to ensure that async loadig gets handled properly (Forge's code properly
     * handles a concurrent asychronous load of the same chunk).
     *
     */
    private Chunk loadChunkForce(int x, int z) {
        return this.loadChunk(x, z);
    }
}
