package industries.angelic.vintagepickupnotifier;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import industries.angelic.vintagepickupnotifier.client.command.CommandTestEntries;
import industries.angelic.vintagepickupnotifier.client.handler.AddEntriesHandler;
import industries.angelic.vintagepickupnotifier.client.handler.DrawEntriesHandler;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        MinecraftForge.EVENT_BUS.register(DrawEntriesHandler.INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(DrawEntriesHandler.INSTANCE);
        if (VintagePickUpNotifier.isDebugMode()) {
            VintagePickUpNotifier.debug("Registering /vpun_test command");
            ClientCommandHandler.instance.registerCommand(new CommandTestEntries());
        }
    }

    @Override
    public void handleTakeItemStackMessage(final int entityId, ItemStack itemStack) {
        final ItemStack copiedStack = itemStack == null ? null : itemStack.copy();
        Minecraft.getMinecraft()
            .func_152344_a(new Runnable() {

                @Override
                public void run() {
                    AddEntriesHandler.addItemStackEntry(entityId, copiedStack);
                }
            });
    }

    @Override
    public void handleTakeExperienceMessage(final int entityId, final int experienceValue, final int tickCount) {
        Minecraft.getMinecraft()
            .func_152344_a(new Runnable() {

                @Override
                public void run() {
                    AddEntriesHandler.addExperienceEntry(entityId, experienceValue, tickCount);
                }
            });
    }
}
