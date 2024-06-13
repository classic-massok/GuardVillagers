package dev.sterner.guardvillagers.common.network;

import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record GuardFollowPacket(int guardId) implements CustomPayload {
    public static final CustomPayload.Id<GuardFollowPacket> ID = new CustomPayload.Id<>(Identifier.of(GuardVillagers.MODID, "guard_follow"));

    public static final PacketCodec<RegistryByteBuf, GuardFollowPacket> PACKET_CODEC = PacketCodec.of(
            GuardFollowPacket::read,
            GuardFollowPacket::write
    );

    private static GuardFollowPacket write(RegistryByteBuf registryByteBuf) {
        return new GuardFollowPacket(registryByteBuf.readInt());
    }

    private GuardFollowPacket read(RegistryByteBuf registryByteBuf) {
        return new GuardFollowPacket(registryByteBuf.readInt());
    }

    public void handle(ServerPlayNetworking.Context context) {

        Entity entity = context.player().getWorld().getEntityById(guardId);
        if (entity instanceof GuardEntity guardEntity) {
            guardEntity.setFollowing(!guardEntity.isFollowing());
            guardEntity.setOwnerId(context.player().getUuid());
            guardEntity.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1, 1);
        }
    }


    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
