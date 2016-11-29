package hellfirepvp.astralsorcery.client.effect;

import hellfirepvp.astralsorcery.AstralSorcery;
import hellfirepvp.astralsorcery.client.effect.controller.OrbitalEffectController;
import hellfirepvp.astralsorcery.client.effect.fx.EntityComplexFX;
import hellfirepvp.astralsorcery.client.effect.fx.EntityFXFacingParticle;
import hellfirepvp.astralsorcery.client.effect.light.EffectLightbeam;
import hellfirepvp.astralsorcery.client.effect.texture.TexturePlane;
import hellfirepvp.astralsorcery.client.effect.texture.TextureSpritePlane;
import hellfirepvp.astralsorcery.client.render.tile.TESRFakeTree;
import hellfirepvp.astralsorcery.client.util.resource.AssetLibrary;
import hellfirepvp.astralsorcery.client.util.resource.BindableResource;
import hellfirepvp.astralsorcery.client.util.resource.SpriteSheetResource;
import hellfirepvp.astralsorcery.common.util.Axis;
import hellfirepvp.astralsorcery.common.util.data.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: EffectHandler
 * Created by HellFirePvP
 * Date: 12.05.2016 / 17:44
 */
public final class EffectHandler {

    public static final Random STATIC_EFFECT_RAND = new Random();

    private static int clientEffectTick = 0;

    public static final EffectHandler instance = new EffectHandler();

    private static boolean acceptsNewParticles = true, cleanRequested = false;

    private static List<IComplexEffect> toAddBuffer = new LinkedList<>();

    public static final Map<IComplexEffect.RenderTarget, Map<Integer, List<IComplexEffect>>> complexEffects = new HashMap<>();
    public static final List<EntityFXFacingParticle> fastRenderParticles = new LinkedList<>();
    //public static final List<EffectLightbeam> fastRenderBeams = new LinkedList<>();

    private EffectHandler() {}

    public static EffectHandler getInstance() {
        return instance;
    }

    public static int getDebugEffectCount() {
        int amt = 0;
        for (Map<Integer, List<IComplexEffect>> effects : complexEffects.values()) {
            for (List<IComplexEffect> eff : effects.values()) {
                amt += eff.size();
            }
        }
        amt += fastRenderParticles.size();
        return amt;
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.TEXT) {
            acceptsNewParticles = false;
            Map<Integer, List<IComplexEffect>> layeredEffects = complexEffects.get(IComplexEffect.RenderTarget.OVERLAY_TEXT);
            for (int i = 0; i <= 2; i++) {
                for (IComplexEffect effect : layeredEffects.get(i)) {
                    GL11.glPushMatrix();
                    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                    effect.render(event.getPartialTicks());
                    GL11.glPopAttrib();
                    GL11.glPopMatrix();
                }
            }
            acceptsNewParticles = true;
        }
    }

    @SubscribeEvent
    public void onDebugText(RenderGameOverlayEvent.Text event) {
        if(Minecraft.getMinecraft().gameSettings.showDebugInfo) {
            event.getLeft().add("");
            event.getLeft().add("[AstralSorcery] EffectHandler:");
            event.getLeft().add("[AstralSorcery] > Complex effects: " + getDebugEffectCount());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRender(RenderWorldLastEvent event) {
        acceptsNewParticles = false;
        Map<Integer, List<IComplexEffect>> layeredEffects = complexEffects.get(IComplexEffect.RenderTarget.RENDERLOOP);
        EntityFXFacingParticle.renderFast(event.getPartialTicks(), fastRenderParticles);
        //EffectLightbeam.renderFast(fastRenderBeams); Not done atm since translations seem to be wrong w/e i do o_o
        for (int i = 0; i <= 2; i++) {
            for (IComplexEffect effect : layeredEffects.get(i)) {
                GL11.glPushMatrix();
                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                effect.render(event.getPartialTicks());
                GL11.glPopAttrib();
                GL11.glPopMatrix();
            }
        }
        acceptsNewParticles = true;
        TESRFakeTree.renderTranslucentBlocks();
    }

    @SubscribeEvent
    public void onClTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tick();
    }

    public EntityComplexFX registerFX(EntityComplexFX entityComplexFX) {
        register(entityComplexFX);
        return entityComplexFX;
    }

    public OrbitalEffectController orbital(OrbitalEffectController.OrbitPointEffect pointEffect, @Nullable OrbitalEffectController.OrbitPersistence persistence, @Nullable OrbitalEffectController.OrbitTickModifier tickModifier) {
        OrbitalEffectController ctrl = new OrbitalEffectController(pointEffect, persistence, tickModifier);
        register(ctrl);
        return ctrl;
    }

    public TextureSpritePlane textureSpritePlane(SpriteSheetResource sheetResource, Axis rotationAxis) {
        TextureSpritePlane plane = new TextureSpritePlane(sheetResource, rotationAxis);
        register(plane);
        return plane;
    }

    public TexturePlane texturePlane(BindableResource texture, Axis rotationAxis) {
        TexturePlane plane = new TexturePlane(texture, rotationAxis);
        register(plane);
        return plane;
    }

    public EffectLightbeam lightbeam(Vector3 from, Vector3 to, double beamRadSize) {
        EffectLightbeam beam = new EffectLightbeam(from, to, beamRadSize);
        register(beam);
        return beam;
    }

    public EffectLightbeam lightbeam(Vector3 from, Vector3 to, double fromBeamSize, double toBeamSize) {
        EffectLightbeam beam = new EffectLightbeam(from, to, fromBeamSize, toBeamSize);
        register(beam);
        return beam;
    }

    private void register(final IComplexEffect effect) {
        if(AssetLibrary.reloading || Minecraft.getMinecraft().isGamePaused()) return;

        if(acceptsNewParticles) {
            registerUnsafe(effect);
        } else {
            toAddBuffer.add(effect);
        }
    }

    private void registerUnsafe(IComplexEffect effect) {
        if(effect instanceof EntityFXFacingParticle) {
            fastRenderParticles.add((EntityFXFacingParticle) effect);
            //} else if(effect instanceof EffectLightbeam) {
            //    fastRenderBeams.add((EffectLightbeam) effect);
        } else {
            complexEffects.get(effect.getRenderTarget()).get(effect.getLayer()).add(effect);
        }
        effect.clearRemoveFlag();
    }

    public void tick() {
        clientEffectTick++;

        if(cleanRequested) {
            for (IComplexEffect.RenderTarget t : IComplexEffect.RenderTarget.values()) {
                for (int i = 0; i <= 2; i++) {
                    complexEffects.get(t).get(i).clear();
                }
            }
            fastRenderParticles.clear();
            toAddBuffer.clear();
            cleanRequested = false;
        }

        acceptsNewParticles = false;
        for (IComplexEffect.RenderTarget target : complexEffects.keySet()) {
            Map<Integer, List<IComplexEffect>> layeredEffects = complexEffects.get(target);
            for (int i = 0; i <= 2; i++) {
                Iterator<IComplexEffect> iterator = layeredEffects.get(i).iterator();
                while (iterator.hasNext()) {
                    IComplexEffect effect = iterator.next();
                    effect.tick();
                    if(effect.canRemove()) {
                        effect.flagAsRemoved();
                        iterator.remove();
                    }
                }
            }
        }
        Iterator<EntityFXFacingParticle> iterator = fastRenderParticles.iterator();
        while (iterator.hasNext()) {
            EntityFXFacingParticle effect = iterator.next();
            effect.tick();
            if(effect.canRemove()) {
                effect.flagAsRemoved();
                iterator.remove();
            }
        }
        acceptsNewParticles = true;
        toAddBuffer.forEach(this::registerUnsafe);
        toAddBuffer.clear();
    }

    public static int getClientEffectTick() {
        return clientEffectTick;
    }

    static {
        for (IComplexEffect.RenderTarget target : IComplexEffect.RenderTarget.values()) {
            Map<Integer, List<IComplexEffect>> layeredEffects = new HashMap<>();
            for (int i = 0; i <= 2; i++) {
                layeredEffects.put(i, new LinkedList<>());
            }
            complexEffects.put(target, layeredEffects);
        }
    }

    public static void cleanUp() {
        cleanRequested = true;
    }
}
