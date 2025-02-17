/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package com.example.addon.modules;

import meteordevelopment.meteorclient.systems.modules.Module;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

public class TridentDupe extends Module {
    // Coded by Killet Laztec & Ionar :3
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
        .name("dupe-delay")
        .description("Raise this if it isn't working. This is how fast you'll dupe. 5 is good for most.")
        .defaultValue(5)
        .build()
    );

    private final Setting<Boolean> dropTridents = sgGeneral.add(new BoolSetting.Builder()
        .name("dropTridents")
        .description("Drops tridents in your last hotbar slot.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> durabilityManagement = sgGeneral.add(new BoolSetting.Builder()
        .name("durabilityManagement")
        .description("(More AFKable) Attempts to dupe the highest durability trident in your hotbar.")
        .defaultValue(true)
        .build()
    );

    private final Queue<Packet<?>> delayedPackets = new LinkedList<>();

    public TridentDupe() {
        super(com.example.addon.TridentDupe.CATEGORY, "trident-dupe", "Dupes tridents in first hotbar slot. / / Killet / / Laztec / / Ionar");
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onSendPacket(PacketEvent.Send event) {

        if (event.packet instanceof ClientTickEndC2SPacket
            || event.packet instanceof PlayerMoveC2SPacket
            || event.packet instanceof CloseHandledScreenC2SPacket)
            return;

        if (!(event.packet instanceof ClickSlotC2SPacket)
            && !(event.packet instanceof PlayerActionC2SPacket))
        {
            return;
        }
        if (!cancel)
            return;

        MutableText packetStr = Text.literal(event.packet.toString()).formatted(Formatting.WHITE);

        event.cancel();
    }

    @Override
    public void onActivate()
    {
        if (mc.player == null)
            return;

        for (int i = 0; i < 9; i++)
        {
            if (mc.player.getInventory().getStack((i)).getItem() == Items.TRIDENT)
            {
                Integer currentHotbarDamage = mc.player.getInventory().getStack((i)).getDamage();

            }
        }

        PlayerInteractItemC2SPacket pckt = new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 10, -57.0f, 66.29f);

        Int2ObjectMap<ItemStack> modifiedStacks = new Int2ObjectOpenHashMap<>();

        modifiedStacks.put(3,  mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot));
        modifiedStacks.put(36,  mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot));

        ClickSlotC2SPacket packet = new ClickSlotC2SPacket(0, 15, 0, 0, SlotActionType.SWAP,
            new ItemStack(Items.AIR), modifiedStacks);

        scheduledTasks.clear();
        dupe();

    }

    private void dupe()
    {
        int delayInt = (delay.get()).intValue()*100;

        System.out.println(delayInt);

        int lowestHotbarSlot = 0;
        int lowestHotbarDamage = 1000;
        for (int i = 0; i < 9; i++)
        {
            if (mc.player.getInventory().getStack((i)).getItem() == Items.TRIDENT)
            {
                Integer currentHotbarDamage = mc.player.getInventory().getStack((i)).getDamage();
                if(lowestHotbarDamage > currentHotbarDamage) { lowestHotbarSlot = i; lowestHotbarDamage = currentHotbarDamage;}

            }
        }

        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        cancel = true;

        int finalLowestHotbarSlot = lowestHotbarSlot;
        scheduleTask(() -> {
            cancel = false;

            if(durabilityManagement.get()) {
                if(finalLowestHotbarSlot != 0) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, (44), 0, SlotActionType.SWAP, mc.player);
                    if(dropTridents.get())mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 44, 0, SlotActionType.THROW, mc.player);
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, (36 + finalLowestHotbarSlot), 0, SlotActionType.SWAP, mc.player);
                }
            }

            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 3, 0, SlotActionType.SWAP, mc.player);

            PlayerActionC2SPacket packet2 = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN, 0);
            mc.getNetworkHandler().sendPacket(packet2);

            if(dropTridents.get()) mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 44, 0, SlotActionType.THROW, mc.player);

            cancel = true;
            scheduleTask2(this::dupe, delayInt);
        }, delayInt);
    }


    private boolean cancel = true;

    private final List<Pair<Long, Runnable>> scheduledTasks = new ArrayList<>();
    private final List<Pair<Long, Runnable>> scheduledTasks2 = new ArrayList<>();

    public void scheduleTask(Runnable task, long delayMillis) {
        long executeTime = System.currentTimeMillis() + delayMillis;
        scheduledTasks.add(new Pair<>(executeTime, task));
    }
    public void scheduleTask2(Runnable task, long delayMillis) {
        long executeTime = System.currentTimeMillis() + delayMillis;
        scheduledTasks2.add(new Pair<>(executeTime, task));
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        long currentTime = System.currentTimeMillis();
        {
            Iterator<Pair<Long, Runnable>> iterator = scheduledTasks.iterator();

            while (iterator.hasNext()) {
                Pair<Long, Runnable> entry = iterator.next();
                if (entry.getLeft() <= currentTime) {
                    entry.getRight().run();
                    iterator.remove(); // Remove executed task from the list
                }
            }
        }
        {
            Iterator<Pair<Long, Runnable>> iterator = scheduledTasks2.iterator();

            while (iterator.hasNext()) {
                Pair<Long, Runnable> entry = iterator.next();
                if (entry.getLeft() <= currentTime) {
                    entry.getRight().run();
                    iterator.remove(); // Remove executed task from the list
                }
            }
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        toggle();
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {
            toggle();
        }
    }

}
