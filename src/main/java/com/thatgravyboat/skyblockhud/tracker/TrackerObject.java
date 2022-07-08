//package com.thatgravyboat.skyblockhud.tracker;
//
//import com.google.gson.JsonObject;
//import com.thatgravyboat.skyblockhud.location.Locations;
//import java.util.EnumSet;
//import java.util.Locale;
//import net.minecraft.item.Item;
//import net.minecraft.item.ItemStack;
//import net.minecraft.nbt.NBTBase;
//import net.minecraft.nbt.NBTTagCompound;
//import net.minecraft.nbt.NBTTagList;
//import net.minecraft.util.ResourceLocation;
//
//public class TrackerObject {
//
//    private final ItemStack stack;
//    private final String internalId;
//    private final EnumSet<Locations> locations;
//    private final boolean isEntity;
//    private int count;
//
//    public TrackerObject(JsonObject jsonObject, EnumSet<Locations> locations) {
//        this.stack = decodeToItemStack(jsonObject);
//        this.internalId = jsonObject.get("id").getAsString();
//        this.isEntity = jsonObject.get("id").getAsString().contains("entity:");
//        this.locations = locations;
//    }
//
//    public static ItemStack decodeToItemStack(JsonObject jsonObject) {
//        jsonObject = jsonObject.getAsJsonObject("displayItem");
//        int meta = jsonObject.get("meta").getAsInt();
//        ResourceLocation itemid = new ResourceLocation(jsonObject.get("item").getAsString());
//        ItemStack stack = new ItemStack(Item.itemRegistry.getObject(itemid), 0, meta);
//        if (jsonObject.has("displayName")) stack.setStackDisplayName(jsonObject.get("displayName").getAsString());
//        if (jsonObject.has("skullData") && itemid.getResourcePath().equals("skull") && meta == 3) {
//            stack.setTagInfo("SkullOwner", getSkullTag(jsonObject.getAsJsonObject("skullData")));
//        }
//        if (jsonObject.has("enchanted") && jsonObject.get("enchanted").getAsBoolean()) {
//            stack.setTagInfo("ench", new NBTTagList());
//        }
//        if (!jsonObject.get("id").getAsString().contains("entity:")) {
//            NBTTagCompound extraAttributes = new NBTTagCompound();
//            extraAttributes.setString("id", jsonObject.get("id").getAsString());
//            stack.setTagInfo("ExtraAttributes", extraAttributes);
//        }
//        return stack;
//    }
//
//    public static NBTBase getSkullTag(JsonObject skullObject) {
//        NBTTagCompound skullOwner = new NBTTagCompound();
//        NBTTagCompound properties = new NBTTagCompound();
//        NBTTagList textures = new NBTTagList();
//        NBTTagCompound value = new NBTTagCompound();
//
//        skullOwner.setString("Id", skullObject.get("id").getAsString());
//
//        value.setString("Value", skullObject.get("texture").getAsString());
//        textures.appendTag(value);
//
//        properties.setTag("textures", textures);
//
//        skullOwner.setTag("Properties", properties);
//        return skullOwner;
//    }
//
//    public void increaseCount(int amount) {
//        count += amount;
//    }
//
//    public void increaseCount() {
//        count++;
//    }
//
//    public void setCount(int count) {
//        this.count = count;
//    }
//
//    public int getCount() {
//        return count;
//    }
//
//    public ItemStack getDisplayStack() {
//        return stack;
//    }
//
//    public EnumSet<Locations> getLocations() {
//        return locations;
//    }
//
//    public String getInternalId() {
//        return internalId.toUpperCase(Locale.ENGLISH);
//    }
//
//    public boolean isEntity() {
//        return isEntity;
//    }
//}
