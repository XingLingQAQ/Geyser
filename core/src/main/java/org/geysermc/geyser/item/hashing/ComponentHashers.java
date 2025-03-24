/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.item.hashing;

import com.google.common.hash.HashCode;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.CustomModelData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.FoodProperties;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.IntComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemEnchantments;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.TooltipDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Unit;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@SuppressWarnings("UnstableApiUsage")
public class ComponentHashers {
    private static final Map<DataComponentType<?>, MinecraftHasher<?>> hashers = new HashMap<>();

    static {
        register(DataComponentTypes.CUSTOM_DATA, hasher -> hasher.nbtMap(Function.identity()));
        register(DataComponentTypes.MAX_STACK_SIZE);
        register(DataComponentTypes.MAX_DAMAGE);
        register(DataComponentTypes.DAMAGE);
        register(DataComponentTypes.UNBREAKABLE);

        // TODO custom name, component
        // TODO item name, component

        register(DataComponentTypes.ITEM_MODEL, MinecraftHasher.KEY);

        // TODO lore, component

        register(DataComponentTypes.RARITY, MinecraftHasher.RARITY);
        register(DataComponentTypes.ENCHANTMENTS, MinecraftHasher.map(MinecraftHasher.ENCHANTMENT, MinecraftHasher.INT).convert(ItemEnchantments::getEnchantments));

        // TODO can place on/can break on, complicated
        // TODO attribute modifiers, attribute registry and equipment slot group hashers

        registerMap(DataComponentTypes.CUSTOM_MODEL_DATA, builder -> builder
            .optionalList("floats", MinecraftHasher.FLOAT, CustomModelData::floats)
            .optionalList("flags", MinecraftHasher.BOOL, CustomModelData::flags)
            .optionalList("strings", MinecraftHasher.STRING, CustomModelData::strings)
            .optionalList("colors", MinecraftHasher.INT, CustomModelData::colors));

        registerMap(DataComponentTypes.TOOLTIP_DISPLAY, builder -> builder
            .optional("hide_tooltip", MinecraftHasher.BOOL, TooltipDisplay::hideTooltip, false)
            .optionalList("hidden_components", MinecraftHasher.DATA_COMPONENT_TYPE, TooltipDisplay::hiddenComponents));

        register(DataComponentTypes.REPAIR_COST);

        register(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, MinecraftHasher.BOOL);
        register(DataComponentTypes.INTANGIBLE_PROJECTILE); // TODO MCPL is wrong

        registerMap(DataComponentTypes.FOOD, builder -> builder
            .accept("nutrition", MinecraftHasher.INT, FoodProperties::getNutrition)
            .accept("saturation", MinecraftHasher.FLOAT, FoodProperties::getSaturationModifier)
            .optional("can_always_eat", MinecraftHasher.BOOL, FoodProperties::isCanAlwaysEat, false));
    }

    private static void register(DataComponentType<Unit> component) {
        register(component, MinecraftHasher.UNIT);
    }

    private static void register(IntComponentType component) {
        register(component, MinecraftHasher.INT);
    }

    private static <T> void registerMap(DataComponentType<T> component, UnaryOperator<MapHasher<T>> builder) {
        register(component, MinecraftHasher.mapBuilder(builder));
    }

    private static <T> void register(DataComponentType<T> component, MinecraftHasher<T> hasher) {
        if (hashers.containsKey(component)) {
            throw new IllegalArgumentException("Tried to register a hasher for a component twice");
        }
        hashers.put(component, hasher);
    }

    public static <T> HashCode hash(GeyserSession session, DataComponentType<T> component, T value) {
        MinecraftHasher<T> hasher = (MinecraftHasher<T>) hashers.get(component);
        if (hasher == null) {
            throw new IllegalStateException("Unregistered hasher for component " + component + "!");
        }
        return hasher.hash(value, new MinecraftHashEncoder(session));
    }
}
