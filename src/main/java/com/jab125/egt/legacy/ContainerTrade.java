package com.jab125.egt.legacy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.jab125.egt.EGobT;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mrcrayfish.goblintraders.trades.CraftingHelper;
import com.mrcrayfish.goblintraders.trades.GoblinTrade;
import com.mrcrayfish.goblintraders.trades.TradeSerializer;
import com.mrcrayfish.goblintraders.trades.type.ITradeType;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.jab125.thonkutil.ThonkUtil.GSON;

public class ContainerTrade implements ITradeType<GoblinTrade> {
    public static final Serializer SERIALIZER = new Serializer();
    private final ItemStack offerStack;
    private final ItemStack paymentStack;
    private final ItemStack secondaryPaymentStack;
    private final SlotItem[] slotItems;
    private final float priceMultiplier;
    private final int maxTrades;
    private final int experience;
    private final boolean isRequired;

    public ContainerTrade(ItemStack offerStack, ItemStack paymentStack, ItemStack secondaryPaymentStack, float priceMultiplier, int maxTrades, int experience, SlotItem[] slotItems, boolean isRequired) {
        this.offerStack = offerStack;
        this.paymentStack = paymentStack;
        this.secondaryPaymentStack = secondaryPaymentStack;
        this.priceMultiplier = priceMultiplier;
        this.maxTrades = maxTrades;
        this.experience = experience;
        this.slotItems = slotItems;
        this.isRequired = isRequired;
    }

    @Override
    public JsonObject serialize() {
        return SERIALIZER.serialize(this);
    }

    @Override
    public GoblinTrade createVillagerTrade() {
        ItemStack container = offerStack.copy();
        NbtList itemList = new NbtList();
        for (SlotItem slotItem : this.slotItems) {
            NbtCompound nbtCompound = container.getOrCreateNbt();
            NbtCompound itemCompound = new NbtCompound();
            writeNbt(itemCompound, slotItem);
            itemList.add(itemCompound);
            if (!nbtCompound.contains("BlockEntityTag")) {
                nbtCompound.put("BlockEntityTag", new NbtCompound());
            }
            NbtCompound blockEntityTag =  nbtCompound.getCompound("BlockEntityTag");
            if (!blockEntityTag.contains("Items")) {
                blockEntityTag.put("Items", itemList);
            }
        }

        return new GoblinTrade(container, this.paymentStack.copy(), this.secondaryPaymentStack.copy(), this.maxTrades, this.experience, this.priceMultiplier);
    }

    public NbtCompound writeNbt(NbtCompound nbt, SlotItem item) {
        Identifier identifier = Registry.ITEM.getId(item.getItemStack().getItem());
        nbt.putString("id", identifier == null ? "minecraft:air" : identifier.toString());
        nbt.putByte("Slot", (byte) item.getSlot());
        nbt.putByte("Count", (byte) item.getItemStack().getCount());
        if (item.getItemStack().getNbt() != null) {
            nbt.put("tag", item.getItemStack().getNbt().copy());
        }

        return nbt;
    }
    public static class Serializer extends TradeSerializer<ContainerTrade> {
        Serializer() {
            super(new Identifier(EGobT.MODID, "container"));
        }

        @Override
        public ContainerTrade deserialize(JsonObject object) {
            Builder builder = Builder.create();
            builder.setOfferStack(CraftingHelper.getItemStack(JsonHelper.getObject(object, "offer_item"), true));
            builder.setPaymentStack(CraftingHelper.getItemStack(JsonHelper.getObject(object, "payment_item"), true));
            if(JsonHelper.hasElement(object, "secondary_payment_item"))
            {
                builder.setSecondaryPaymentStack(CraftingHelper.getItemStack(JsonHelper.getObject(object, "secondary_payment_item"), true));
            }
            builder.setPriceMultiplier(JsonHelper.getFloat(object, "price_multiplier", 0.05F));
            builder.setMaxTrades(JsonHelper.getInt(object, "max_trades", 12));
            builder.setExperience(JsonHelper.getInt(object, "experience", 0));
            if(JsonHelper.hasElement(object, "container_items"))
            {
                Collection<SlotItem> slotItems = this.getSlotItems(JsonHelper.getArray(object, "container_items"));
                for(SlotItem item : slotItems)
                {
                    builder.setContainerItem(item.getItemStack(), item.getSlot());
                }
            }
            return builder.build();
        }
        private static ContainerTrade.SlotItem getSlotItem(JsonObject json, boolean readNBT) {
            String itemName = JsonHelper.getString(json, "item");

            Item item = Registry.ITEM.get(new Identifier(itemName));
            int slot = JsonHelper.getInt(json, "slot");

            if (item == null)
                throw new JsonSyntaxException("Unknown item '" + itemName + "'");

            if (readNBT && json.has("nbt"))
            {
                // Lets hope this works? Needs test
                try
                {
                    JsonElement element = json.get("nbt");
                    NbtCompound nbt;
                    if(element.isJsonObject())
                        nbt = StringNbtReader.parse(GSON.toJson(element));
                    else
                        nbt = StringNbtReader.parse(JsonHelper.asString(element, "nbt"));

                    NbtCompound tmp = new NbtCompound();
                    if (nbt.contains("ForgeCaps"))
                    {
                        tmp.put("ForgeCaps", nbt.get("ForgeCaps"));
                        nbt.remove("ForgeCaps");
                    }

                    tmp.put("tag", nbt);
                    tmp.putString("id", itemName);
                    tmp.putInt("Count", JsonHelper.getInt(json, "count", 1));

                    return new ContainerTrade.SlotItem(slot, ItemStack.fromNbt(tmp));
                }
                catch (CommandSyntaxException e)
                {
                    throw new JsonSyntaxException("Invalid NBT Entry: " + e.toString());
                }
            }

            return new ContainerTrade.SlotItem(slot, new ItemStack(item, JsonHelper.getInt(json, "count", 1)));
        }
        public Collection<SlotItem> getSlotItems(JsonArray slotArray) {
            List<SlotItem> slotItems = new ArrayList<>();
            for(JsonElement slotElement : slotArray)
            {
                JsonObject bundleObject = slotElement.getAsJsonObject();
                SlotItem itemStack = getSlotItem(bundleObject, true);
                slotItems.add(itemStack);
            }
            return slotItems;
        }

        @Override
        public JsonObject serialize(ContainerTrade trade)
        {
            JsonObject object = super.serialize(trade);
            object.add("offer_item", this.serializeItemStack(trade.offerStack));
            object.add("payment_item", this.serializeItemStack(trade.paymentStack));
            if(!trade.secondaryPaymentStack.isEmpty())
            {
                object.add("secondary_payment_item", this.serializeItemStack(trade.secondaryPaymentStack));
            }
            if(trade.priceMultiplier != 0.05F)
            {
                object.addProperty("price_multiplier", trade.priceMultiplier);
            }
            if(trade.maxTrades != 12)
            {
                object.addProperty("max_trades", trade.maxTrades);
            }
            if(trade.experience != 0)
            {
                object.addProperty("experience", trade.experience);
            }
            if(trade.slotItems.length > 0) {
                JsonArray itemsArray = new JsonArray();
                for (SlotItem slotItem : trade.slotItems) {
                    itemsArray.add(this.serializeSlotItem(slotItem));
                }
                object.add("container_items", itemsArray);
            }
//            if(trade.bundleItems.length > 0) {
//                JsonArray bundleArray = new JsonArray();
//                for(ItemStack bundleItem : trade.bundleItems)
//                {
//                    bundleArray.add(this.serializeItemStack(bundleItem));
//                }
//                object.add("bundle_items", bundleArray);
//            }
            return object;
        }

        private JsonObject serializeItemStack(ItemStack stack) {
            JsonObject object = new JsonObject();
            object.addProperty("item", Objects.requireNonNull(Registry.ITEM.getId(stack.getItem())).toString());
            object.addProperty("count", stack.getCount());
            if(stack.hasNbt())
            {
                object.addProperty("nbt", Objects.requireNonNull(stack.getNbt()).toString());
            }
            return object;
        }

        private JsonObject serializeSlotItem(SlotItem stack) {
            JsonObject object = new JsonObject();
            object.addProperty("slot", stack.getSlot());
            object.addProperty("item", Objects.requireNonNull(Registry.ITEM.getId(stack.getItemStack().getItem())).toString());
            object.addProperty("count", stack.getItemStack().getCount());
            if(stack.getItemStack().hasNbt())
            {
                object.addProperty("nbt", Objects.requireNonNull(stack.getItemStack().getNbt()).toString());
            }
            return object;
        }
    }

    public static class SlotItem {
        private int slot;
        private ItemStack itemStack;
        public SlotItem(int slot, ItemStack itemStack) {
            this.slot = slot;
            this.itemStack = itemStack;
        }

        public ItemStack getItemStack() {
            return itemStack;
        }

        public int getSlot() {
            return slot;
        }
    }

    public static class Builder {
        private ItemStack offerStack;
        private ItemStack paymentStack;
        private ArrayList<SlotItem> slotItems = new ArrayList<>();
        private ItemStack secondaryPaymentStack = ItemStack.EMPTY;
        private float priceMultiplier = 0.0F;
        private int maxTrades = 12;
        private int experience = 10;
        private boolean required = true;


        private Builder() {
        }

        public static Builder create() {
            return new Builder();
        }

        public ContainerTrade build() {
            return new ContainerTrade(this.offerStack, this.paymentStack, this.secondaryPaymentStack, this.priceMultiplier, this.maxTrades, this.experience, this.slotItems.toArray(new SlotItem[0]), required);
        }

        public Builder setOfferStack(ItemStack offerStack) {
            if (offerStack.getItem() instanceof BlockItem) {
                if (((BlockItem) offerStack.getItem()).getBlock() instanceof BlockWithEntity) {
                    this.offerStack = offerStack;
                } else {
                    throw new RuntimeException("ItemStack need to be of minecraft:block_with_entity!");
                }
            } else {
                throw new RuntimeException("ItemStack need to be of minecraft:block_with_entity!");
            }
            return this;
        }

        public Builder setPaymentStack(ItemStack paymentStack) {
            this.paymentStack = paymentStack;
            return this;
        }

        public Builder setSecondaryPaymentStack(ItemStack secondaryPaymentStack) {
            this.secondaryPaymentStack = secondaryPaymentStack;
            return this;
        }

        public Builder setPriceMultiplier(float priceMultiplier) {
            this.priceMultiplier = priceMultiplier;
            return this;
        }

        public Builder setMaxTrades(int maxTrades) {
            this.maxTrades = maxTrades;
            return this;
        }

        @Deprecated
        public Builder setExperience(int experience) {
            return this.setMerchantExperience(experience);
        }

        public Builder setMerchantExperience(int merchantExperience) {
            this.experience = merchantExperience;
            return this;
        }

        @Deprecated
        public Builder setPlayerExperience(int playerExperience) {
            return this;
        }
        public Builder setContainerItem(ItemStack itemStack, int slot) {
            for (var slotItem : slotItems) {
                if (slot == slotItem.getSlot()) {
                    throw new RuntimeException("Tried to override existing slot with another item.");
                }
            }
            slotItems.add(new SlotItem(slot, itemStack));
            return this;
        }

        public Builder setRequired(boolean required) {
            this.required = required;
            return this;
        }
    }
}