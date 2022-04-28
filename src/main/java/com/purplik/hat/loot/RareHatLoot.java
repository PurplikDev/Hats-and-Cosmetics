package com.purplik.hat.loot;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RareHatLoot extends LootModifier {

    private final Item addition;

    protected RareHatLoot(LootItemCondition[] conditionsIn, Item addition) {
        super(conditionsIn);
        this.addition = addition;
    }

    @NotNull
    @Override
    protected List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
        if(context.getRandom().nextFloat() > 0.5) {
            generatedLoot.add(new ItemStack(addition, 1));
        }

        return generatedLoot;
    }

    public static class Serializer extends GlobalLootModifierSerializer<RareHatLoot> {

        @Override
        public RareHatLoot read(ResourceLocation location, JsonObject object, LootItemCondition[] ailootcondition) {
            Item addition = ForgeRegistries.ITEMS.getValue(
                    new ResourceLocation(GsonHelper.getAsString(object, "addition")));

            return new RareHatLoot(ailootcondition, addition);
        }

        @Override
        public JsonObject write(RareHatLoot instance) {
            JsonObject json = makeConditions(instance.conditions);
            json.addProperty("addition", ForgeRegistries.ITEMS.getKey(instance.addition).toString());

            return json;
        }
    }
}
