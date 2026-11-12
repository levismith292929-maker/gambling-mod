package com.slotmachine.mod.block;

import com.slotmachine.mod.screen.SlotMachineScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Holds two separate 27-slot inventories:
 *  - the slot machine's own GUI (payment slot + decorative reel/spin slots)
 *  - the "Luck Slot" inventory, opened with a separate shift-right-click GUI
 *
 * Clicking Spin removes the diamonds from the payment slot as the wager, rolls
 * each of the 3 reels independently (weighted per ReelSymbol), reveals them
 * one at a time every 2 seconds, and on an all-match win applies the payout
 * (or the dirt/command-block special effects) — see trySpin/tick/resolveSpin.
 */
public class SlotMachineBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {

    public static final int SIZE = 27;
    public static final int PAYMENT_SLOT = 10; // row 2, col 2 (0-indexed) in the screenshot
    public static final int[] REEL_SLOTS = {12, 13, 14};
    public static final int SPIN_SLOT = 16;

    private static final int TICKS_PER_REEL = 40; // 2 seconds

    private final SimpleBlockInventory mainInventory = new SimpleBlockInventory(SIZE, this::markDirty);
    private final SimpleBlockInventory luckInventory = new SimpleBlockInventory(SIZE, this::markDirty);

    // Transient spin state — not persisted; a spin in progress is abandoned on reload.
    private boolean spinning = false;
    private int spinTicks = 0;
    private int wager = 0;
    private final ReelSymbol[] reelResults = new ReelSymbol[REEL_SLOTS.length];
    private UUID spinningPlayerUuid = null;

    public SlotMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SLOT_MACHINE_BLOCK_ENTITY, pos, state);
        resetDecoration();
    }

    /** Puts the fixed, non-removable display items (reels + Spin button) back in place. */
    private void resetDecoration() {
        for (int reelSlot : REEL_SLOTS) {
            mainInventory.setStack(reelSlot, new ItemStack(Items.DIAMOND_BLOCK));
        }
        mainInventory.setStack(SPIN_SLOT, new ItemStack(Items.EMERALD_BLOCK));
    }

    public Inventory getMainInventory() {
        return mainInventory;
    }

    public Inventory getLuckInventory() {
        return luckInventory;
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Slot Machine");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new SlotMachineScreenHandler(syncId, playerInventory, mainInventory, this);
    }

    /** Called by the screen handler when the player clicks the Spin slot. */
    public void trySpin(PlayerEntity player) {
        if (spinning) {
            return;
        }
        ItemStack payment = mainInventory.getStack(PAYMENT_SLOT);
        if (payment.isEmpty() || !payment.isOf(Items.DIAMOND)) {
            player.sendMessage(Text.literal("Insert diamonds in the slot to play."), true);
            return;
        }

        wager = payment.getCount();
        mainInventory.setStack(PAYMENT_SLOT, ItemStack.EMPTY);
        spinningPlayerUuid = player.getUuid();
        spinning = true;
        spinTicks = 0;
        for (int i = 0; i < reelResults.length; i++) {
            reelResults[i] = null;
        }
        markDirty();
    }

    /** Registered as this block entity's ticker — see SlotMachineBlock#getTicker. */
    public static void tick(World world, BlockPos pos, BlockState state, SlotMachineBlockEntity blockEntity) {
        if (world.isClient || !blockEntity.spinning) {
            return;
        }
        blockEntity.spinTicks++;

        boolean allRevealed = true;
        for (int i = 0; i < REEL_SLOTS.length; i++) {
            if (blockEntity.reelResults[i] == null) {
                if (blockEntity.spinTicks >= (i + 1) * TICKS_PER_REEL) {
                    ReelSymbol result = ReelSymbol.roll(world.getRandom());
                    blockEntity.reelResults[i] = result;
                    blockEntity.mainInventory.setStack(REEL_SLOTS[i], new ItemStack(result.item));
                } else {
                    allRevealed = false;
                }
            }
        }

        if (allRevealed) {
            blockEntity.resolveSpin(world, pos);
            blockEntity.spinning = false;
        }
    }

    private void resolveSpin(World world, BlockPos pos) {
        boolean win = reelResults[0] == reelResults[1] && reelResults[1] == reelResults[2];
        ReelSymbol symbol = reelResults[0];
        PlayerEntity player = spinningPlayerUuid != null ? world.getPlayerByUuid(spinningPlayerUuid) : null;

        if (win && player != null) {
            switch (symbol) {
                case DIRT -> removeHeart(player);
                case COPPER -> awardDiamonds(world, pos, (int) Math.floor(wager * 0.5));
                case IRON -> awardDiamonds(world, pos, wager);
                case GOLD -> awardDiamonds(world, pos, (int) Math.floor(wager * 1.5));
                case DIAMOND -> awardDiamonds(world, pos, (int) Math.ceil(wager * 2.5));
                case EMERALD -> awardDiamonds(world, pos, wager * 5);
                case COMMAND_BLOCK -> grantOp(player);
            }
        }

        spinningPlayerUuid = null;
        markDirty();
    }

    private void removeHeart(PlayerEntity player) {
        EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        // Floor at 1 heart (2 health) rather than letting repeated losses drive max health to 0.
        double newMax = Math.max(2.0, maxHealth.getBaseValue() - 2.0);
        maxHealth.setBaseValue(newMax);
        if (player.getHealth() > newMax) {
            player.setHealth((float) newMax);
        }
    }

    private void awardDiamonds(World world, BlockPos pos, int amount) {
        if (amount <= 0) {
            return;
        }
        int capped = Math.min(amount, 64);
        mainInventory.setStack(PAYMENT_SLOT, new ItemStack(Items.DIAMOND, capped));
        int overflow = amount - capped;
        if (overflow > 0) {
            // A single slot can't hold more than 64 — drop the rest next to the machine.
            ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    new ItemStack(Items.DIAMOND, overflow));
        }
    }

    private void grantOp(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        MinecraftServer server = serverPlayer.getServer();
        if (server != null) {
            server.getPlayerManager().addToOperators(serverPlayer.getGameProfile());
            serverPlayer.sendMessage(Text.literal("The machine grants you... everything."), false);
        }
    }

    /** Separate factory for the shift-right-click "Luck Slot" GUI — plain, unrestricted 27 slots. */
    public NamedScreenHandlerFactory getLuckScreenHandlerFactory() {
        return new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Text.literal("Luck Slot");
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, luckInventory);
            }
        };
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.put("MainInventory", Inventories.writeNbt(new NbtCompound(), mainInventory.getItems(), registryLookup));
        nbt.put("LuckInventory", Inventories.writeNbt(new NbtCompound(), luckInventory.getItems(), registryLookup));
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt.getCompound("MainInventory"), mainInventory.getItems(), registryLookup);
        Inventories.readNbt(nbt.getCompound("LuckInventory"), luckInventory.getItems(), registryLookup);
    }
}
