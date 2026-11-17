package industries.angelic.vintagepickupnotifier.client.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import industries.angelic.vintagepickupnotifier.Config;
import industries.angelic.vintagepickupnotifier.client.compat.AngelicaFontBatcher;
import industries.angelic.vintagepickupnotifier.client.compat.DashboardPickups;
import industries.angelic.vintagepickupnotifier.client.gui.entry.DisplayEntry;
import industries.angelic.vintagepickupnotifier.client.gui.entry.ExperienceDisplayEntry;
import industries.angelic.vintagepickupnotifier.client.gui.entry.ItemDisplayEntry;
import industries.angelic.vintagepickupnotifier.client.util.DisplayEntryRenderHelper;
import industries.angelic.vintagepickupnotifier.config.AnchorPoint;
import industries.angelic.vintagepickupnotifier.config.CombineEntries;

public class DrawEntriesHandler {

    public static final DrawEntriesHandler INSTANCE = new DrawEntriesHandler();
    private static final int HANDLED_ENTITY_TICKS = 40;
    /** Whether Dashboard is installed, to move the list in its HUD editor (DashboardPickups). */
    public static final boolean DASHBOARD = Loader.isModLoaded("dashboard");
    /** How far the entries' backgrounds reach past their sides. */
    private static final int BACKGROUND_LEFT = 4;
    private static final int BACKGROUND_RIGHT = 5;

    private final Map<Object, DisplayEntry<?>> collector = new LinkedHashMap<>();
    private final Map<Integer, Integer> handledEntities = new HashMap<>();
    private List<DisplayEntry<?>> sample;

    private DrawEntriesHandler() {}

    public void addEntry(DisplayEntry<?> displayEntry) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer != null && minecraft.thePlayer.capabilities.isCreativeMode
            && Config.disableInCreative) {
            return;
        }

        int maxSize = getMaxEntryCount(minecraft);
        DisplayEntry<?> oldEntry = Config.combineEntries == CombineEntries.NEVER ? null
            : this.collector.remove(displayEntry.getKey());
        if (oldEntry != null) {
            displayEntry = displayEntry.mergeWith(oldEntry);
        }

        while (!this.collector.isEmpty() && this.collector.size() >= maxSize) {
            Object firstKey = this.collector.keySet()
                .iterator()
                .next();
            this.collector.remove(firstKey);
        }

        this.collector.put(displayEntry.getKey(), displayEntry);
    }

    public boolean isEntityHandled(int entityId) {
        return this.handledEntities.containsKey(entityId);
    }

    public void addHandledEntity(int entityId) {
        this.handledEntities.put(entityId, HANDLED_ENTITY_TICKS);
    }

    public void addDebugEntries() {
        Minecraft minecraft = Minecraft.getMinecraft();
        ItemStack stack = minecraft.thePlayer != null ? minecraft.thePlayer.getCurrentEquippedItem() : null;
        if (stack == null) {
            stack = new ItemStack(Items.diamond, 7);
        }
        addEntry(new ItemDisplayEntry(stack, Math.max(1, stack.stackSize)));
        addEntry(new ExperienceDisplayEntry("Experience", 42, 0));
    }

    public void clear() {
        this.collector.clear();
        this.handledEntities.clear();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getMinecraft()
            .isGamePaused()) {
            return;
        }

        if (!this.collector.isEmpty()) {
            for (DisplayEntry<?> entry : this.collector.values()) {
                entry.tick();
            }

            if (Config.displayTime != 0) {
                this.collector.values()
                    .removeIf(DisplayEntry::mayDiscard);
            }
        }

        if (!this.handledEntities.isEmpty()) {
            Iterator<Entry<Integer, Integer>> iterator = this.handledEntities.entrySet()
                .iterator();
            while (iterator.hasNext()) {
                Entry<Integer, Integer> entry = iterator.next();
                int remainingTicks = entry.getValue() - 1;
                if (remainingTicks <= 0) {
                    iterator.remove();
                } else {
                    entry.setValue(remainingTicks);
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        if (this.collector.isEmpty()) {
            return;
        }
        if (DASHBOARD && !DashboardPickups.begin(event.resolution)) {
            return;
        }
        drawEntries(
            this.collector.values(),
            event.resolution.getScaledWidth(),
            event.resolution.getScaledHeight(),
            event.partialTicks,
            false);
        if (DASHBOARD) {
            DashboardPickups.end();
        }
    }

    /** What the list shows, or a sample while it is empty, standing still where it is drawn: for Dashboard's editor. */
    public void drawPreview(int screenWidth, int screenHeight) {
        drawEntries(getShownEntries(), screenWidth, screenHeight, 0.0F, true);
    }

    /**
     * Where the list is drawn on the screen, as x, y, width and height, leaving out entries sliding away; while it is
     * empty, where the sample would be.
     */
    public int[] getShownBox(int screenWidth, int screenHeight) {
        Collection<DisplayEntry<?>> entries = getShownEntries();
        float scale = Config.getDisplayScale();
        int maxWidth = getMaxWidth(entries, Minecraft.getMinecraft().fontRenderer);
        AnchorPoint.Positioner positioner = createPositioner(screenWidth, screenHeight, maxWidth, entries.size());
        int left = positioner.getPosX((int) (Config.offsetX / scale)) - BACKGROUND_LEFT;
        int top = positioner.getPosY((int) (Config.offsetY / scale));
        return new int[] { (int) Math.floor(left * scale), (int) Math.floor(top * scale),
            (int) Math.ceil((maxWidth + BACKGROUND_LEFT + BACKGROUND_RIGHT) * scale),
            (int) Math.ceil(getTotalHeight(entries.size(), Config.entryMargin) * scale) };
    }

    private Collection<DisplayEntry<?>> getShownEntries() {
        if (!this.collector.isEmpty()) {
            return this.collector.values();
        }
        if (this.sample == null) {
            this.sample = Arrays.<DisplayEntry<?>>asList(
                new ItemDisplayEntry(new ItemStack(Items.diamond, 7), 7),
                new ItemDisplayEntry(new ItemStack(Items.bread, 3), 3),
                new ExperienceDisplayEntry(StatCollector.translateToLocal("entity.XPOrb.name"), 42, 0));
        }
        return this.sample;
    }

    /** Draws the entries; still ones neither fade nor slide, as in a preview. */
    private static void drawEntries(Collection<DisplayEntry<?>> shown, int screenWidth, int screenHeight,
        float partialTicks, boolean still) {
        Minecraft minecraft = Minecraft.getMinecraft();
        FontRenderer fontRenderer = minecraft.fontRenderer;
        float scale = Config.getDisplayScale();

        List<DisplayEntry<?>> entries = new ArrayList<>(shown);
        AnchorPoint anchorPoint = Config.position;
        if (!anchorPoint.isTop()) {
            Collections.reverse(entries);
        }

        int maxWidth = getMaxWidth(entries, fontRenderer);
        int entryHeight = getEntryHeight(Config.entryMargin);
        int entryTopOverhang = DisplayEntryRenderHelper.getEntryTopOverhang();
        AnchorPoint.Positioner positioner = createPositioner(screenWidth, screenHeight, maxWidth, entries.size());
        int offsetX = (int) (Config.offsetX / scale);
        int offsetY = (int) (Config.offsetY / scale);
        int moveOffset = still ? 0 : getMoveOffset(entries, partialTicks);
        int posX = positioner.getPosX(offsetX);
        int posY = positioner.getPosY(offsetY - moveOffset) + entryTopOverhang;
        int elementY = posY;

        AngelicaFontBatcher.flush(fontRenderer);
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, 1.0F);
        for (DisplayEntry<?> entry : entries) {
            int elementWidth = entry.getEntryWidth(fontRenderer);
            int elementX = posX + anchorPoint.createPositioner(maxWidth, -1, elementWidth, -1)
                .getPosX(0);

            if (!still && Config.moveOut.moveHorizontally(anchorPoint)) {
                elementX += (int) (maxWidth * (1.0F - entry.getRelativeRemainingTicks(partialTicks))
                    * anchorPoint.getNormalX());
            }

            float alpha = !still && Config.fadeOut ? entry.getRelativeRemainingTicks(partialTicks) : 1.0F;
            entry.render(minecraft, fontRenderer, elementX, elementY, alpha, partialTicks);
            elementY += entryHeight;
        }
        AngelicaFontBatcher.flush(fontRenderer);
        GL11.glPopMatrix();
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        clear();
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        clear();
    }

    private static int getMoveOffset(List<DisplayEntry<?>> entries, float partialTicks) {
        if (!Config.moveOut.moveVertically(Config.position)) {
            return 0;
        }
        return (int) (DisplayEntry.getExpiredRows(entries, partialTicks) * getEntryHeight(Config.entryMargin));
    }

    private static int getMaxEntryCount(Minecraft minecraft) {
        ScaledResolution resolution = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight);
        int scaledHeight = (int) (resolution.getScaledHeight() / Config.getDisplayScale());
        int maxHeight = (int) (scaledHeight * Config.maxHeight);
        return Math.max(1, (maxHeight + Config.entryMargin) / getEntryHeight(Config.entryMargin) - 1);
    }

    private static int getMaxWidth(Collection<DisplayEntry<?>> entries, FontRenderer fontRenderer) {
        int maxWidth = 0;
        for (DisplayEntry<?> entry : entries) {
            maxWidth = Math.max(maxWidth, entry.getEntryWidth(fontRenderer));
        }
        return maxWidth;
    }

    /** Places the list, drawn scaled by the display scale, on a screen this big. */
    private static AnchorPoint.Positioner createPositioner(int screenWidth, int screenHeight, int maxWidth,
        int entryCount) {
        float scale = Config.getDisplayScale();
        return Config.position.createPositioner(
            (int) (screenWidth / scale),
            (int) (screenHeight / scale),
            maxWidth,
            getTotalHeight(entryCount, Config.entryMargin));
    }

    private static int getEntryHeight(int entryMargin) {
        return getEntryVisualHeight() + entryMargin;
    }

    private static int getTotalHeight(int entryCount, int entryMargin) {
        if (entryCount <= 0) {
            return 0;
        }
        return getEntryVisualHeight() * entryCount + entryMargin * (entryCount - 1);
    }

    private static int getEntryVisualHeight() {
        return DisplayEntryRenderHelper.getEntryTopOverhang() + DisplayEntryRenderHelper.getEntryBottomOffset();
    }
}
