package uk.me.desert_island.rer;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.CompositeByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import uk.me.desert_island.rer.client.ClientLootCache;
import uk.me.desert_island.rer.client.ClientWorldGenState;
import uk.me.desert_island.rer.networking.LootTableSyncS2CPacket;

import static io.netty.buffer.Unpooled.compositeBuffer;
import static uk.me.desert_island.rer.RoughlyEnoughResources.*;

@Environment(EnvType.CLIENT)
public class RoughlyEnoughResourcesClient {
    private static CompositeByteBuf world_state_buf;

    public static void onInitializeClient() {
        NetworkManager.registerReceiver(NetworkManager.s2c(), SEND_WORLD_GEN_STATE_START, (this_buf, context) -> {
            // int size = this_buf.readInt();
            RERUtils.LOGGER.debug("got SEND_WORLD_GEN_STATE_START");
            world_state_buf = compositeBuffer();
        });

        NetworkManager.registerReceiver(NetworkManager.s2c(), SEND_WORLD_GEN_STATE_CHUNK, (this_buf, context) -> {
            RERUtils.LOGGER.debug("got SEND_WORLD_GEN_STATE_CHUNK");
            world_state_buf.addComponent(true, this_buf.retainedDuplicate());
        });

        NetworkManager.registerReceiver(NetworkManager.s2c(), SEND_WORLD_GEN_STATE_DONE, (this_buf, context) -> {
            RERUtils.LOGGER.debug("got SEND_WORLD_GEN_STATE_DONE");
            FriendlyByteBuf buf = new FriendlyByteBuf(world_state_buf);
            ResourceLocation worldId = buf.readResourceLocation();
            ResourceKey<Level> world = ResourceKey.create(Registries.DIMENSION, worldId);
            ClientWorldGenState state = ClientWorldGenState.byWorld(world);
            state.fromNetwork(buf);
            RERUtils.LOGGER.debug("Received data for " + worldId);
        });

        NetworkManager.registerReceiver(NetworkManager.s2c(), LootTableSyncS2CPacket.TYPE, LootTableSyncS2CPacket.CODEC, (packet, context) -> {
            int size = packet.lootJson().size();
            RERUtils.LOGGER.debug("Received %d Loot Info", size);
            ClientLootCache.ID_TO_LOOT.putAll(packet.lootJson());
        });
    }
}
