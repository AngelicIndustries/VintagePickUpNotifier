package industries.angelic.vintagepickupnotifier.client.compat;

import net.minecraft.client.gui.ScaledResolution;

import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.vintagepickupnotifier.client.handler.DrawEntriesHandler;

/**
 * The pickup list as an element of Dashboard's HUD editor, to move, resize or hide there. Its box grows and shrinks
 * with the list, so Dashboard keeps it at the screen edge it was moved to. The only class using Dashboard's API, so it
 * is only loaded with Dashboard installed (DrawEntriesHandler.DASHBOARD).
 */
@SideOnly(Side.CLIENT)
public final class DashboardPickups {

    private static final HudElement LIST = new HudElement(
        "vintagepickupnotifier:pickups",
        (screenWidth, screenHeight) -> {
            int[] box = DrawEntriesHandler.INSTANCE.getShownBox(screenWidth, screenHeight);
            return new HudBounds(box[0], box[1], box[2], box[3]);
        });

    private DashboardPickups() {}

    public static void register() {
        LIST.setPreview(
            (screenWidth, screenHeight) -> HudRegistry
                .render(LIST, () -> DrawEntriesHandler.INSTANCE.drawPreview(screenWidth, screenHeight)));
        HudRegistry.register(LIST);
    }

    /** False when the player hid the list, so it isn't drawn. */
    public static boolean begin(ScaledResolution resolution) {
        return HudRegistry.beginRender(LIST, resolution);
    }

    public static void end() {
        HudRegistry.endRender();
    }
}
