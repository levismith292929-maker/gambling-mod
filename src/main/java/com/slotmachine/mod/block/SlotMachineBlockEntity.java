package com.slotmachine.mod.block;

import com.slotmachine.mod.menu.SlotMachineMenu;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Holds two separate 27-slot containers:
 *  - the slot machine's own GUI (payment slot + decorative reel/spin slots)
 *  - the "Luck Slot" container, opened with a separate shift-right-click GUI
 *
 * Clicking Spin takes the diamonds from the payment slot as the wager, rolls each of the
 * 3 reels independently (weighted per ReelSymbol), reveals them one at a time every
 * 2 seconds, and on an all-match win applies the payout (or the dirt/command-block
 * special effects) - see trySpin / tick / resolveSpin.
 */
public class SlotMachineBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SIZE = 27;
    public static final int PAYMENT_SLOT = 10; // row 2, col 2 (0-indexed) in the GUI image
    public static final int[] REEL_SLOTS = {12, 13, 14};
    public static final int SPIN_SLOT = 16;

    private static final int TICKS_PER_REEL = 40; // 2 seconds
    private static final int TICKS_PER_FRAME = 4; // how fast unrevealed reels cycle symbols

    private final SimpleBlockContainer mainContainer =
            new SimpleBlockContainer(SIZE, this::setChanged, player -> Container.stillValidBlockEntity(this, player));
    private final SimpleBlockContainer luckContainer =
            new SimpleBlockContainer(SIZE, this::setChanged, player -> Container.stillValidBlockEntity(this, player));

    // Spin state. A spin in progress is abandoned on reload (the wager is refunded, see loadAdditional).
    private boolean spinning = false;
    private int spinTicks = 0;
    private int wager = 0;
    private final ReelSymbol[] reelResults = new ReelSymbol[REEL_SLOTS.length];
    private UUID spinningPlayerUuid = null;

    public SlotMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SLOT_MACHINE_BLOCK_ENTITY, pos, state);
        resetDecoration();
    }

    /** Fills unused machine slots with black panes and restores the fixed reel/spin displays. */
    private void resetDecoration() {
        for (int slot = 0; slot < SIZE; slot++) {
            if (slot != PAYMENT_SLOT) {
                mainContainer.setItem(slot, new ItemStack(Items.BLACK_STAINED_GLASS_PANE));
            }
        }
        for (int reelSlot : REEL_SLOTS) {
            mainContainer.setItem(reelSlot, new ItemStack(Items.DIAMOND_BLOCK));
        }
        mainContainer.setItem(SPIN_SLOT, new ItemStack(Items.EMERALD_BLOCK));
    }

    // ---- Menus ----------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.literal("Slot Machine");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SlotMachineMenu(containerId, inventory, mainContainer, this);
    }

    /** Separate provider for the shift-right-click "Luck Slot" GUI - plain, unrestricted 27 slots. */
    public MenuProvider getLuckMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Luck Slot");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return ChestMenu.threeRows(containerId, inventory, luckContainer);
            }
        };
    }

    // ---- Spinning -------------------------------------------------------------------------

    /** Called by the menu when the player clicks the Spin slot. */
    public void trySpin(Player player) {
        if (spinning) {
            return;
        }
        ItemStack payment = mainContainer.getItem(PAYMENT_SLOT);
        if (payment.isEmpty() || !payment.is(Items.DIAMOND)) {
            tell(player, "Insert diamonds in the slot to play.");
            return;
        }

        wager = payment.getCount();
        mainContainer.setItem(PAYMENT_SLOT, ItemStack.EMPTY);
        spinningPlayerUuid = player.getUUID();
        spinning = true;
        spinTicks = 0;
        for (int i = 0; i < reelResults.length; i++) {
            reelResults[i] = null;
        }
        setChanged();
    }

    /** Registered as this block entity's ticker - see SlotMachineBlock#getTicker. */
    public static void tick(Level level, BlockPos pos, BlockState state, SlotMachineBlockEntity blockEntity) {
        if (level.isClientSide() || !blockEntity.spinning) {
            return;
        }
        blockEntity.spinTicks++;

        ReelSymbol[] symbols = ReelSymbol.values();
        boolean allRevealed = true;
        for (int i = 0; i < REEL_SLOTS.length; i++) {
            if (blockEntity.reelResults[i] != null) {
                continue;
            }
            if (blockEntity.spinTicks >= (i + 1) * TICKS_PER_REEL) {
                // This reel stops: roll its final symbol.
                ReelSymbol result = ReelSymbol.roll(level.getRandom());
                blockEntity.reelResults[i] = result;
                blockEntity.mainContainer.setItem(REEL_SLOTS[i], new ItemStack(result.item));
            } else {
                allRevealed = false;
                // Still spinning: cycle through the symbols in order (dirt > copper > ... > command block).
                if (blockEntity.spinTicks % TICKS_PER_FRAME == 0) {
                    int frame = blockEntity.spinTicks / TICKS_PER_FRAME + i;
                    ReelSymbol shown = symbols[frame % symbols.length];
                    blockEntity.mainContainer.setItem(REEL_SLOTS[i], new ItemStack(shown.item));
                }
            }
        }

        if (allRevealed) {
            blockEntity.resolveSpin(level, pos);
            blockEntity.spinning = false;
        }
    }

    private void resolveSpin(Level level, BlockPos pos) {
        boolean win = reelResults[0] == reelResults[1] && reelResults[1] == reelResults[2];
        ReelSymbol symbol = reelResults[0];
        Player player = spinningPlayerUuid != null ? level.getPlayerByUUID(spinningPlayerUuid) : null;

        if (win) {
            switch (symbol) {
                case DIRT -> removeHeart(player);
                case COPPER -> awardDiamonds(level, pos, player, wager / 2);
                case IRON -> awardDiamonds(level, pos, player, wager);
                case GOLD -> awardDiamonds(level, pos, player, wager * 3 / 2);
                case DIAMOND -> awardDiamonds(level, pos, player, wager * 5 / 2);
                case EMERALD -> awardDiamonds(level, pos, player, wager * 5);
                case COMMAND_BLOCK -> grantOp(level, player);
            }
        } else if (player != null) {
            tell(player, "No match.");
        }

        spinningPlayerUuid = null;
        wager = 0;
        setChanged();
    }

    private void removeHeart(Player player) {
        if (player == null) {
            return;
        }
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        // One heart = 2 health. Floor at 1 heart so repeated losses can't drive max health to 0.
        double newMax = Math.max(2.0, maxHealth.getBaseValue() - 2.0);
        maxHealth.setBaseValue(newMax);
        if (player.getHealth() > newMax) {
            player.setHealth((float) newMax);
        }
        tell(player, "Three dirt... you lose a heart.");
    }

    private void awardDiamonds(Level level, BlockPos pos, Player player, int amount) {
        if (amount <= 0) {
            if (player != null) {
                tell(player, "You win nothing.");
            }
            return;
        }
        int inSlot = Math.min(amount, 64);
        mainContainer.setItem(PAYMENT_SLOT, new ItemStack(Items.DIAMOND, inSlot));
        // A single slot holds at most 64, so drop any overflow next to the machine.
        int overflow = amount - inSlot;
        while (overflow > 0) {
            int stack = Math.min(overflow, 64);
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    new ItemStack(Items.DIAMOND, stack));
            overflow -= stack;
        }
        if (player != null) {
            tell(player, "You win " + amount + " diamonds!");
        }
    }

    private void grantOp(Level level, Player player) {
        if (player == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        var server = serverLevel.getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                "op " + player.getName().getString());
        tell(player, "The machine grants you... everything.");
    }

    private static void tell(Player player, String message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal(message), true);
        }
    }

    /** Drops the payment/prize slot and the Luck Slot contents (called when the machine is broken). */
    public void dropContents(Level level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        // If the machine is broken during an animation, return the wager too.
        if (spinning && wager > 0) {
            int remaining = wager;
            while (remaining > 0) {
                int stackSize = Math.min(remaining, 64);
                Containers.dropItemStack(level, x, y, z, new ItemStack(Items.DIAMOND, stackSize));
                remaining -= stackSize;
            }
            spinning = false;
            wager = 0;
            spinningPlayerUuid = null;
        }

        ItemStack payment = mainContainer.getItem(PAYMENT_SLOT);
        if (!payment.isEmpty()) {
            Containers.dropItemStack(level, x, y, z, payment.copy());
            mainContainer.setItem(PAYMENT_SLOT, ItemStack.EMPTY);
        }
        for (int i = 0; i < luckContainer.getContainerSize(); i++) {
            ItemStack stack = luckContainer.getItem(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, x, y, z, stack.copy());
                luckContainer.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    // ---- Saving ---------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output.child("MainInventory"), mainContainer.getItems());
        ContainerHelper.saveAllItems(output.child("LuckInventory"), luckContainer.getItems());
        // If the world is saved mid-spin, remember the wager so it can be refunded on load.
        output.putInt("PendingWager", spinning ? wager : 0);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mainContainer.getItems().replaceAll(stack -> ItemStack.EMPTY);
        luckContainer.getItems().replaceAll(stack -> ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input.childOrEmpty("MainInventory"), mainContainer.getItems());
        ContainerHelper.loadAllItems(input.childOrEmpty("LuckInventory"), luckContainer.getItems());

        int pending = input.getIntOr("PendingWager", 0);
        if (pending > 0 && mainContainer.getItem(PAYMENT_SLOT).isEmpty()) {
            mainContainer.getItems().set(PAYMENT_SLOT, new ItemStack(Items.DIAMOND, Math.min(pending, 64)));
        }
        spinning = false;
        resetDecoration();
    }
}
