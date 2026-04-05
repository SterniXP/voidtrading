package de.sterni.voidtrading.customtrades.inventoryview;

import de.sterni.voidtrading.customtrades.ListEditor;
import lombok.NonNull;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.IntConsumer;

public class CustomTradesInventoryView {
    private static final int ROWS = 6;
    private static final int COLUMNS = 9;
    private static final int SIZE = ROWS * COLUMNS;
    private static final int CONTENT_SLOTS = 45;

    private static final int NAV_PREVIOUS_SLOT = 45;
    private static final int NAV_PAGE_INFO_SLOT = 49;
    private static final int NAV_CLOSE_SLOT = 51;
    private static final int NAV_NEXT_SLOT = 53;

    private final ListEditor listEditor;

    private record ResultEntry(Identifier identifier, Item item, int tradeCount) {}

    private record TradeEntry(int index, TradeOffer offer) {}

    public CustomTradesInventoryView(@NonNull ListEditor listEditor) {
        this.listEditor = listEditor;
    }

    // TODO: can we use a sub class to allow for also managing, editing and creating trades in the future?

    public void openViewForPlayer(@NonNull ServerPlayerEntity player) {
        if (player instanceof FakePlayer) return;
        openMainPage(player, 0);
    }

    // FIXME: slots are not correctly protected when returning from details view
    protected void openMainPage(@NonNull ServerPlayerEntity player, int requestedPage) {
        List<ResultEntry> entries = createResultEntries();
        int maxPage = getMaxPage(entries.size());
        int page = Math.clamp(requestedPage, 0, maxPage);

        Inventory inventory = new SimpleInventory(SIZE);
        fillMainContent(inventory, entries, page);
        fillMainNavigation(inventory, page, maxPage);

        Text title = Text.literal(listEditor.getShortName() + " | " + (page + 1) + "/" + (maxPage + 1));
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, _) -> createProtectedHandler(syncId, playerInventory, inventory,
                        clickedSlot -> onMainSlotClicked(player, clickedSlot, entries, page, maxPage)), title
        ));
    }

    private void onMainSlotClicked(@NonNull ServerPlayerEntity player,
                                   int clickedSlot,
                                   @NonNull List<ResultEntry> entries,
                                   int page,
                                   int maxPage) {
        if (clickedSlot < CONTENT_SLOTS) {
            int index = page * CONTENT_SLOTS + clickedSlot;
            if (index < entries.size()) {
                openTradeDetailsPage(player, entries.get(index), page);
            }
            return;
        }

        if (clickedSlot == NAV_PREVIOUS_SLOT && page > 0) {
            openMainPage(player, page - 1);
        } else if (clickedSlot == NAV_NEXT_SLOT && page < maxPage) {
            openMainPage(player, page + 1);
        } else if (clickedSlot == NAV_CLOSE_SLOT) {
            player.closeHandledScreen();
        }
    }

    private void openTradeDetailsPage(@NonNull ServerPlayerEntity player, @NonNull ResultEntry resultEntry, int page) {
        List<TradeEntry> tradeEntries = createTradeEntries(resultEntry.item());
        TradeOfferList offers = createPreviewOfferList(tradeEntries);
        PreviewMerchant merchant = new PreviewMerchant(player, offers, this, page);

        Text title = Text.literal("Trades for " + resultEntry.identifier() + " (" + tradeEntries.size() + ")");
        OptionalInt sync = player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, _) -> new ReadOnlyMerchantScreenHandler(syncId, playerInventory, merchant),
                title
        ));
        if (sync.isPresent()) {
            player.sendTradeOffers(sync.getAsInt(), offers, 0, 0, false, false);
        }
    }

    private ScreenHandler createProtectedHandler(int syncId,
                                                 @NonNull PlayerInventory playerInventory,
                                                 @NonNull Inventory inventory,
                                                 @NonNull IntConsumer onInventorySlotClick) {
        return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, inventory, ROWS) {
            @Override
            public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
                if (slotIndex >= 0 && slotIndex < inventory.size() && player instanceof ServerPlayerEntity) {
                    onInventorySlotClick.accept(slotIndex);
                }
            }

            @Override
            public ItemStack quickMove(PlayerEntity player, int slot) {
                return ItemStack.EMPTY;
            }
        };
    }

    private List<ResultEntry> createResultEntries() {
        List<Identifier> sortedIdentifiers = new ArrayList<>(listEditor.getIdentifiers());
        sortedIdentifiers.sort(Comparator.comparing(Identifier::toString));

        List<ResultEntry> entries = new ArrayList<>(sortedIdentifiers.size());
        for (Identifier identifier : sortedIdentifiers) {
            Registries.ITEM.getOptionalValue(identifier).ifPresent(item -> {
                int tradeCount = listEditor.getTradesWithResult(item, false).size();
                entries.add(new ResultEntry(identifier, item, tradeCount));
            });
        }
        return entries;
    }

    private List<TradeEntry> createTradeEntries(@NonNull Item resultItem) {
        LinkedHashSet<TradeOffer> offers = listEditor.getTradesWithResult(resultItem, false);
        List<TradeEntry> entries = new ArrayList<>(offers.size());
        int i = 1;
        for (TradeOffer offer : offers) {
            entries.add(new TradeEntry(i, offer));
            i++;
        }
        return entries;
    }

    private TradeOfferList createPreviewOfferList(@NonNull List<TradeEntry> entries) {
        TradeOfferList offers = new TradeOfferList();
        for (TradeEntry entry : entries) {
            TradeOffer offer = entry.offer().copy();
            offers.add(offer);
        }
        return offers;
    }

    private void fillMainContent(@NonNull Inventory inventory, @NonNull List<ResultEntry> entries, int page) {
        int start = page * CONTENT_SLOTS;
        for (int slot = 0; slot < CONTENT_SLOTS; slot++) {
            int index = start + slot;
            if (index >= entries.size()) {
                break;
            }
            inventory.setStack(slot, createResultStack(entries.get(index)));
        }
    }

    private ItemStack createResultStack(@NonNull ResultEntry resultEntry) {
        int amount = Math.clamp(resultEntry.tradeCount(), 1, resultEntry.item().getMaxCount());
        ItemStack stack = new ItemStack(resultEntry.item(), amount);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(resultEntry.identifier().toString()));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("Trades: " + resultEntry.tradeCount()),
                Text.literal("Click to open detail view")
        )));
        return stack;
    }

    private void fillMainNavigation(@NonNull Inventory inventory, int page, int maxPage) {
        fillBaseNavigation(inventory, page, maxPage);
    }

    private void fillBaseNavigation(@NonNull Inventory inventory, int page, int maxPage) {
        if (page > 0) {
            inventory.setStack(NAV_PREVIOUS_SLOT, createControlStack(Items.ARROW, "Previous page", "Go to page " + page));
        }
        if (page < maxPage) {
            inventory.setStack(NAV_NEXT_SLOT, createControlStack(Items.ARROW, "Next page", "Go to page " + (page + 2)));
        }

        inventory.setStack(NAV_PAGE_INFO_SLOT, createControlStack(Items.BOOK, "Page", (page + 1) + " / " + (maxPage + 1)));
        inventory.setStack(NAV_CLOSE_SLOT, createControlStack(Items.BARRIER, "Close", "Close this menu"));
    }

    private ItemStack createControlStack(@NonNull Item item, @NonNull String name, @NonNull String detail) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal(detail))));
        return stack;
    }

    private static int getMaxPage(int totalEntries) {
        if (totalEntries <= 0) {
            return 0;
        }
        return totalEntries / CONTENT_SLOTS;
    }
}
