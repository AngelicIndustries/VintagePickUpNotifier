package industries.angelic.vintagepickupnotifier.server.handler;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import industries.angelic.vintagepickupnotifier.Config;
import industries.angelic.vintagepickupnotifier.network.ClientboundTakeExperienceMessage;
import industries.angelic.vintagepickupnotifier.network.NetworkHandler;

public class ExperiencePickupHandler {

    public static final ExperiencePickupHandler INSTANCE = new ExperiencePickupHandler();

    private ExperiencePickupHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerPickupXp(PlayerPickupXpEvent event) {
        if (Config.clientOnly || event.isCanceled()
            || event.orb == null
            || event.orb.worldObj.isRemote
            || !(event.entityPlayer instanceof EntityPlayerMP)
            || event.orb.xpValue <= 0) {
            return;
        }

        NetworkHandler.CHANNEL.sendTo(
            new ClientboundTakeExperienceMessage(event.orb.getEntityId(), event.orb.xpValue, event.orb.xpColor),
            (EntityPlayerMP) event.entityPlayer);
    }
}
