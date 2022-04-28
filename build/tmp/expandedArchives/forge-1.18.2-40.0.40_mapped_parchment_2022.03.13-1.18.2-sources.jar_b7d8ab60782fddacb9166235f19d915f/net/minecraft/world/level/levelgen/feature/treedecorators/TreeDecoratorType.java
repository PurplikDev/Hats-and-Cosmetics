package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;

public class TreeDecoratorType<P extends TreeDecorator> extends net.minecraftforge.registries.ForgeRegistryEntry<TreeDecoratorType<?>> {
   public static final TreeDecoratorType<TrunkVineDecorator> TRUNK_VINE = register("trunk_vine", TrunkVineDecorator.CODEC);
   public static final TreeDecoratorType<LeaveVineDecorator> LEAVE_VINE = register("leave_vine", LeaveVineDecorator.CODEC);
   public static final TreeDecoratorType<CocoaDecorator> COCOA = register("cocoa", CocoaDecorator.CODEC);
   public static final TreeDecoratorType<BeehiveDecorator> BEEHIVE = register("beehive", BeehiveDecorator.CODEC);
   public static final TreeDecoratorType<AlterGroundDecorator> ALTER_GROUND = register("alter_ground", AlterGroundDecorator.CODEC);
   private final Codec<P> codec;

   private static <P extends TreeDecorator> TreeDecoratorType<P> register(String pKey, Codec<P> pCodec) {
      return Registry.register(Registry.TREE_DECORATOR_TYPES, pKey, new TreeDecoratorType<>(pCodec));
   }

   public TreeDecoratorType(Codec<P> pCodec) {
      this.codec = pCodec;
   }

   public Codec<P> codec() {
      return this.codec;
   }
}
