package com.slotmachine.mod.block;

import com.slotmachine.mod.ModSounds;
import com.slotmachine.mod.item.ModItems;
import com.slotmachine.mod.menu.SlotMachineMenu;
import com.slotmachine.mod.menu.LuckSlotMenu;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
public class SlotMachineBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {

    public static final int SIZE = 45;
    public static final int PAYMENT_SLOT = 30;
    public static final int PRIZE_SLOT = 32;
    public static final int[] REEL_SLOTS = {12, 13, 14};
    public static final int SPIN_SLOT = 16;

    private static final int TICKS_PER_REEL = 40;
    private static final int TICKS_PER_FRAME = 4;

    private final SimpleBlockContainer mainContainer =
            new SimpleBlockContainer(SIZE, this::setChanged, player -> Container.stillValidBlockEntity(this, player));
    private final SimpleBlockContainer luckContainer =
            new SimpleBlockContainer(SIZE, this::setChanged, player -> Container.stillValidBlockEntity(this, player));

    private boolean spinning = false;
    private int spinTicks = 0;
    private int wager = 0;
    private final ReelSymbol[] reelResults = new ReelSymbol[REEL_SLOTS.length];
    private UUID spinningPlayerUuid = null;

    public SlotMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SLOT_MACHINE_BLOCK_ENTITY, pos, state);
        resetDecoration();
    }

    private void resetDecoration() {
        for (int slot = 0; slot < SIZE; slot++) {
            if (slot != PAYMENT_SLOT && slot != PRIZE_SLOT && slot != SPIN_SLOT
                    && slot != REEL_SLOTS[0] && slot != REEL_SLOTS[1] && slot != REEL_SLOTS[2]) {
                mainContainer.setItem(slot, named(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", "black_stained_glass_pane"))), ""));
            }
        }
        for (int reelSlot : REEL_SLOTS) {
            mainContainer.setItem(reelSlot, named(new ItemStack(Items.DIAMOND), ""));
        }
        mainContainer.setItem(SPIN_SLOT, named(new ItemStack(Items.EMERALD), "Spin to Win"));
    }


    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Slot Machine");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SlotMachineMenu(containerId, inventory, mainContainer, this);
    }

    public MenuProvider getLuckMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Luck Slot");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new LuckSlotMenu(containerId, inventory, luckContainer);
            }
        };
    }

    private int getLuckCharmTier() {
        return ModItems.getLuckCharmTier(luckContainer.getItem(13));
    }

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
        if (level != null && !level.isClientSide()) {
            level.playSound(null, worldPosition, ModSounds.SPIN_START, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SlotMachineBlockEntity blockEntity) {
        if (level.isClientSide() || !blockEntity.spinning) {
            return;
        }
        blockEntity.spinTicks++;

        if (blockEntity.spinTicks % TICKS_PER_FRAME == 0) {
            float pitch = 0.95f + level.getRandom().nextFloat() * 0.10f;
            level.playSound(null, pos, ModSounds.REEL_TICK, SoundSource.BLOCKS, 0.55f, pitch);
        }

        ReelSymbol[] symbols = ReelSymbol.values();
        boolean allRevealed = true;
        for (int i = 0; i < REEL_SLOTS.length; i++) {
            if (blockEntity.reelResults[i] != null) {
                continue;
            }
            if (blockEntity.spinTicks >= (i + 1) * TICKS_PER_REEL) {
                ReelSymbol result = ReelSymbol.roll(level.getRandom(), blockEntity.getLuckCharmTier());
                blockEntity.reelResults[i] = result;
                blockEntity.mainContainer.setItem(REEL_SLOTS[i], named(new ItemStack(result.item), ""));
                float pitch = 0.95f + level.getRandom().nextFloat() * 0.10f;
                level.playSound(null, pos, ModSounds.REEL_STOP, SoundSource.BLOCKS, 1.25f, pitch);
            } else {
                allRevealed = false;
                if (blockEntity.spinTicks % TICKS_PER_FRAME == 0) {
                    int frame = blockEntity.spinTicks / TICKS_PER_FRAME + i;
                    ReelSymbol shown = symbols[frame % symbols.length];
                    blockEntity.mainContainer.setItem(REEL_SLOTS[i], named(new ItemStack(shown.item), ""));
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
                case IRON -> awardDiamonds(level, pos, player, wager * 3 / 2);
                case GOLD -> awardDiamonds(level, pos, player, wager * 3);
                case DIAMOND -> awardDiamonds(level, pos, player, wager * 5);
                case EMERALD -> awardDiamonds(level, pos, player, wager * 10);
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

        ItemStack existingPrize = mainContainer.getItem(PRIZE_SLOT);
        int space = existingPrize.isEmpty() ? 64
                : (existingPrize.is(Items.DIAMOND) ? 64 - existingPrize.getCount() : 0);
        int stored = Math.min(amount, space);
        if (stored > 0) {
            if (existingPrize.isEmpty()) {
                mainContainer.setItem(PRIZE_SLOT, new ItemStack(Items.DIAMOND, stored));
            } else {
                ItemStack updatedPrize = existingPrize.copy();
                updatedPrize.grow(stored);
                mainContainer.setItem(PRIZE_SLOT, updatedPrize);
            }
        }

        int overflow = amount - stored;
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

    public void dropContents(Level level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

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


    // Hoppers and other sided automation can only feed diamonds into the payment slot.
    // The bottom face exposes only the prize slot for extraction.
    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? new int[]{PRIZE_SLOT} : new int[]{PAYMENT_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == PAYMENT_SLOT && stack.is(Items.DIAMOND) && !spinning;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && slot == PRIZE_SLOT && stack.is(Items.DIAMOND);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == PAYMENT_SLOT && stack.is(Items.DIAMOND) && !spinning;
    }

    // WorldlyContainer exposes the machine's automation inventory, not its decorative GUI slots.
    @Override
    public int getContainerSize() {
        return mainContainer.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return mainContainer.getItem(PAYMENT_SLOT).isEmpty() && mainContainer.getItem(PRIZE_SLOT).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == PAYMENT_SLOT || slot == PRIZE_SLOT ? mainContainer.getItem(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return slot == PRIZE_SLOT ? mainContainer.removeItem(slot, amount) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return slot == PRIZE_SLOT ? mainContainer.removeItemNoUpdate(slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == PAYMENT_SLOT && (stack.isEmpty() || stack.is(Items.DIAMOND))) {
            mainContainer.setItem(slot, stack);
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        mainContainer.setItem(PAYMENT_SLOT, ItemStack.EMPTY);
        mainContainer.setItem(PRIZE_SLOT, ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output.child("MainInventory"), mainContainer.getItems());
        ContainerHelper.saveAllItems(output.child("LuckInventory"), luckContainer.getItems());
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
