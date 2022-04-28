package net.minecraft.client.gui.screens;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class PresetFlatWorldScreen extends Screen {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int SLOT_TEX_SIZE = 128;
   private static final int SLOT_BG_SIZE = 18;
   private static final int SLOT_STAT_HEIGHT = 20;
   private static final int SLOT_BG_X = 1;
   private static final int SLOT_BG_Y = 1;
   private static final int SLOT_FG_X = 2;
   private static final int SLOT_FG_Y = 2;
   static final List<PresetFlatWorldScreen.PresetInfo> PRESETS = Lists.newArrayList();
   private static final ResourceKey<Biome> DEFAULT_BIOME = Biomes.PLAINS;
   /** The parent GUI */
   final CreateFlatWorldScreen parent;
   private Component shareText;
   private Component listText;
   private PresetFlatWorldScreen.PresetsList list;
   private Button selectButton;
   EditBox export;
   FlatLevelGeneratorSettings settings;

   public PresetFlatWorldScreen(CreateFlatWorldScreen pParent) {
      super(new TranslatableComponent("createWorld.customize.presets.title"));
      this.parent = pParent;
   }

   @Nullable
   private static FlatLayerInfo getLayerInfoFromString(String pLayerInfo, int pCurrentHeight) {
      String[] astring = pLayerInfo.split("\\*", 2);
      int i;
      if (astring.length == 2) {
         try {
            i = Math.max(Integer.parseInt(astring[0]), 0);
         } catch (NumberFormatException numberformatexception) {
            LOGGER.error("Error while parsing flat world string => {}", (Object)numberformatexception.getMessage());
            return null;
         }
      } else {
         i = 1;
      }

      int j = Math.min(pCurrentHeight + i, DimensionType.Y_SIZE);
      int k = j - pCurrentHeight;
      String s = astring[astring.length - 1];

      Block block;
      try {
         block = Registry.BLOCK.getOptional(new ResourceLocation(s)).orElse((Block)null);
      } catch (Exception exception) {
         LOGGER.error("Error while parsing flat world string => {}", (Object)exception.getMessage());
         return null;
      }

      if (block == null) {
         LOGGER.error("Error while parsing flat world string => Unknown block, {}", (Object)s);
         return null;
      } else {
         return new FlatLayerInfo(k, block);
      }
   }

   private static List<FlatLayerInfo> getLayersInfoFromString(String pLayerInfo) {
      List<FlatLayerInfo> list = Lists.newArrayList();
      String[] astring = pLayerInfo.split(",");
      int i = 0;

      for(String s : astring) {
         FlatLayerInfo flatlayerinfo = getLayerInfoFromString(s, i);
         if (flatlayerinfo == null) {
            return Collections.emptyList();
         }

         list.add(flatlayerinfo);
         i += flatlayerinfo.getHeight();
      }

      return list;
   }

   public static FlatLevelGeneratorSettings fromString(Registry<Biome> pRegistry, String pSettings, FlatLevelGeneratorSettings pLevelGeneratorSettings) {
      Iterator<String> iterator = Splitter.on(';').split(pSettings).iterator();
      if (!iterator.hasNext()) {
         return FlatLevelGeneratorSettings.getDefault(pRegistry);
      } else {
         List<FlatLayerInfo> list = getLayersInfoFromString(iterator.next());
         if (list.isEmpty()) {
            return FlatLevelGeneratorSettings.getDefault(pRegistry);
         } else {
            FlatLevelGeneratorSettings flatlevelgeneratorsettings = pLevelGeneratorSettings.withLayers(list, pLevelGeneratorSettings.structureSettings());
            ResourceKey<Biome> resourcekey = DEFAULT_BIOME;
            if (iterator.hasNext()) {
               try {
                  ResourceLocation resourcelocation = new ResourceLocation(iterator.next());
                  resourcekey = ResourceKey.create(Registry.BIOME_REGISTRY, resourcelocation);
                  pRegistry.getOptional(resourcekey).orElseThrow(() -> {
                     return new IllegalArgumentException("Invalid Biome: " + resourcelocation);
                  });
               } catch (Exception exception) {
                  LOGGER.error("Error while parsing flat world string => {}", (Object)exception.getMessage());
                  resourcekey = DEFAULT_BIOME;
               }
            }

            ResourceKey<Biome> resourcekey1 = resourcekey;
            flatlevelgeneratorsettings.setBiome(() -> {
               return pRegistry.getOrThrow(resourcekey1);
            });
            return flatlevelgeneratorsettings;
         }
      }
   }

   static String save(Registry<Biome> pRegistry, FlatLevelGeneratorSettings pLevelGeneratorSettings) {
      StringBuilder stringbuilder = new StringBuilder();

      for(int i = 0; i < pLevelGeneratorSettings.getLayersInfo().size(); ++i) {
         if (i > 0) {
            stringbuilder.append(",");
         }

         stringbuilder.append(pLevelGeneratorSettings.getLayersInfo().get(i));
      }

      stringbuilder.append(";");
      stringbuilder.append((Object)pRegistry.getKey(pLevelGeneratorSettings.getBiome()));
      return stringbuilder.toString();
   }

   protected void init() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      this.shareText = new TranslatableComponent("createWorld.customize.presets.share");
      this.listText = new TranslatableComponent("createWorld.customize.presets.list");
      this.export = new EditBox(this.font, 50, 40, this.width - 100, 20, this.shareText);
      this.export.setMaxLength(1230);
      Registry<Biome> registry = this.parent.parent.worldGenSettingsComponent.registryHolder().registryOrThrow(Registry.BIOME_REGISTRY);
      this.export.setValue(save(registry, this.parent.settings()));
      this.settings = this.parent.settings();
      this.addWidget(this.export);
      this.list = new PresetFlatWorldScreen.PresetsList();
      this.addWidget(this.list);
      this.selectButton = this.addRenderableWidget(new Button(this.width / 2 - 155, this.height - 28, 150, 20, new TranslatableComponent("createWorld.customize.presets.select"), (p_96405_) -> {
         FlatLevelGeneratorSettings flatlevelgeneratorsettings = fromString(registry, this.export.getValue(), this.settings);
         this.parent.setConfig(flatlevelgeneratorsettings);
         this.minecraft.setScreen(this.parent);
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 5, this.height - 28, 150, 20, CommonComponents.GUI_CANCEL, (p_96394_) -> {
         this.minecraft.setScreen(this.parent);
      }));
      this.updateButtonValidity(this.list.getSelected() != null);
   }

   public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
      return this.list.mouseScrolled(pMouseX, pMouseY, pDelta);
   }

   public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
      String s = this.export.getValue();
      this.init(pMinecraft, pWidth, pHeight);
      this.export.setValue(s);
   }

   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   public void removed() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      this.list.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      pPoseStack.pushPose();
      pPoseStack.translate(0.0D, 0.0D, 400.0D);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 8, 16777215);
      drawString(pPoseStack, this.font, this.shareText, 50, 30, 10526880);
      drawString(pPoseStack, this.font, this.listText, 50, 70, 10526880);
      pPoseStack.popPose();
      this.export.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   public void tick() {
      this.export.tick();
      super.tick();
   }

   public void updateButtonValidity(boolean p_96450_) {
      this.selectButton.active = p_96450_ || this.export.getValue().length() > 1;
   }

   private static void preset(Component p_96425_, ItemLike p_96426_, ResourceKey<Biome> p_96427_, List<StructureFeature<?>> p_96428_, boolean p_96429_, boolean p_96430_, boolean p_96431_, FlatLayerInfo... p_96432_) {
      PRESETS.add(new PresetFlatWorldScreen.PresetInfo(p_96426_.asItem(), p_96425_, (p_96423_) -> {
         Map<StructureFeature<?>, StructureFeatureConfiguration> map = Maps.newHashMap();

         for(StructureFeature<?> structurefeature : p_96428_) {
            map.put(structurefeature, StructureSettings.DEFAULTS.get(structurefeature));
         }

         StructureSettings structuresettings = new StructureSettings(p_96429_ ? Optional.of(StructureSettings.DEFAULT_STRONGHOLD) : Optional.empty(), map);
         FlatLevelGeneratorSettings flatlevelgeneratorsettings = new FlatLevelGeneratorSettings(structuresettings, p_96423_);
         if (p_96430_) {
            flatlevelgeneratorsettings.setDecoration();
         }

         if (p_96431_) {
            flatlevelgeneratorsettings.setAddLakes();
         }

         for(int i = p_96432_.length - 1; i >= 0; --i) {
            flatlevelgeneratorsettings.getLayersInfo().add(p_96432_[i]);
         }

         flatlevelgeneratorsettings.setBiome(() -> {
            return p_96423_.getOrThrow(p_96427_);
         });
         flatlevelgeneratorsettings.updateLayers();
         return flatlevelgeneratorsettings.withStructureSettings(structuresettings);
      }));
   }

   static {
      preset(new TranslatableComponent("createWorld.customize.preset.classic_flat"), Blocks.GRASS_BLOCK, Biomes.PLAINS, Arrays.asList(StructureFeature.VILLAGE), false, false, false, new FlatLayerInfo(1, Blocks.GRASS_BLOCK), new FlatLayerInfo(2, Blocks.DIRT), new FlatLayerInfo(1, Blocks.BEDROCK));
      preset(new TranslatableComponent("createWorld.customize.preset.tunnelers_dream"), Blocks.STONE, Biomes.WINDSWEPT_HILLS, Arrays.asList(StructureFeature.MINESHAFT), true, true, false, new FlatLayerInfo(1, Blocks.GRASS_BLOCK), new FlatLayerInfo(5, Blocks.DIRT), new FlatLayerInfo(230, Blocks.STONE), new FlatLayerInfo(1, Blocks.BEDROCK));
      preset(new TranslatableComponent("createWorld.customize.preset.water_world"), Items.WATER_BUCKET, Biomes.DEEP_OCEAN, Arrays.asList(StructureFeature.OCEAN_RUIN, StructureFeature.SHIPWRECK, StructureFeature.OCEAN_MONUMENT), false, false, false, new FlatLayerInfo(90, Blocks.WATER), new FlatLayerInfo(5, Blocks.SAND), new FlatLayerInfo(5, Blocks.DIRT), new FlatLayerInfo(5, Blocks.STONE), new FlatLayerInfo(1, Blocks.BEDROCK));
      preset(new TranslatableComponent("createWorld.customize.preset.overworld"), Blocks.GRASS, Biomes.PLAINS, Arrays.asList(StructureFeature.VILLAGE, StructureFeature.MINESHAFT, StructureFeature.PILLAGER_OUTPOST, StructureFeature.RUINED_PORTAL), true, true, true, new FlatLayerInfo(1, Blocks.GRASS_BLOCK), new FlatLayerInfo(3, Blocks.DIRT), new FlatLayerInfo(59, Blocks.STONE), new FlatLayerInfo(1, Blocks.BEDROCK));
      preset(new TranslatableComponent("createWorld.customize.preset.snowy_kingdom"), Blocks.SNOW, Biomes.SNOWY_PLAINS, Arrays.asList(StructureFeature.VILLAGE, StructureFeature.IGLOO), false, false, false, new FlatLayerInfo(1, Blocks.SNOW), new FlatLayerInfo(1, Blocks.GRASS_BLOCK), new FlatLayerInfo(3, Blocks.DIRT), new FlatLayerInfo(59, Blocks.STONE), new FlatLayerInfo(1, Blocks.BEDROCK));
      preset(new TranslatableComponent("createWorld.customize.preset.bottomless_pit"), Items.FEATHER, Biomes.PLAINS, Arrays.asList(StructureFeature.VILLAGE), false, false, false, new FlatLayerInfo(1, Blocks.GRASS_BLOCK), new FlatLayerInfo(3, Blocks.DIRT), new FlatLayerInfo(2, Blocks.COBBLESTONE));
      preset(new TranslatableComponent("createWorld.customize.preset.desert"), Blocks.SAND, Biomes.DESERT, Arrays.asList(StructureFeature.VILLAGE, StructureFeature.DESERT_PYRAMID, StructureFeature.MINESHAFT), true, true, false, new FlatLayerInfo(8, Blocks.SAND), new FlatLayerInfo(52, Blocks.SANDSTONE), new FlatLayerInfo(3, Blocks.STONE), new FlatLayerInfo(1, Blocks.BEDROCK));
      preset(new TranslatableComponent("createWorld.customize.preset.redstone_ready"), Items.REDSTONE, Biomes.DESERT, Collections.emptyList(), false, false, false, new FlatLayerInfo(52, Blocks.SANDSTONE), new FlatLayerInfo(3, Blocks.STONE), new FlatLayerInfo(1, Blocks.BEDROCK));
      preset(new TranslatableComponent("createWorld.customize.preset.the_void"), Blocks.BARRIER, Biomes.THE_VOID, Collections.emptyList(), false, true, false, new FlatLayerInfo(1, Blocks.AIR));
   }

   @OnlyIn(Dist.CLIENT)
   static class PresetInfo {
      public final Item icon;
      public final Component name;
      public final Function<Registry<Biome>, FlatLevelGeneratorSettings> settings;

      public PresetInfo(Item pIcon, Component pName, Function<Registry<Biome>, FlatLevelGeneratorSettings> pSettings) {
         this.icon = pIcon;
         this.name = pName;
         this.settings = pSettings;
      }

      public Component getName() {
         return this.name;
      }
   }

   @OnlyIn(Dist.CLIENT)
   class PresetsList extends ObjectSelectionList<PresetFlatWorldScreen.PresetsList.Entry> {
      public PresetsList() {
         super(PresetFlatWorldScreen.this.minecraft, PresetFlatWorldScreen.this.width, PresetFlatWorldScreen.this.height, 80, PresetFlatWorldScreen.this.height - 37, 24);

         for(PresetFlatWorldScreen.PresetInfo presetflatworldscreen$presetinfo : PresetFlatWorldScreen.PRESETS) {
            this.addEntry(new PresetFlatWorldScreen.PresetsList.Entry(presetflatworldscreen$presetinfo));
         }

      }

      public void setSelected(@Nullable PresetFlatWorldScreen.PresetsList.Entry pEntry) {
         super.setSelected(pEntry);
         PresetFlatWorldScreen.this.updateButtonValidity(pEntry != null);
      }

      protected boolean isFocused() {
         return PresetFlatWorldScreen.this.getFocused() == this;
      }

      public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
         if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
         } else {
            if ((pKeyCode == 257 || pKeyCode == 335) && this.getSelected() != null) {
               this.getSelected().select();
            }

            return false;
         }
      }

      @OnlyIn(Dist.CLIENT)
      public class Entry extends ObjectSelectionList.Entry<PresetFlatWorldScreen.PresetsList.Entry> {
         private final PresetFlatWorldScreen.PresetInfo preset;

         public Entry(PresetFlatWorldScreen.PresetInfo p_169360_) {
            this.preset = p_169360_;
         }

         public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
            this.blitSlot(pPoseStack, pLeft, pTop, this.preset.icon);
            PresetFlatWorldScreen.this.font.draw(pPoseStack, this.preset.name, (float)(pLeft + 18 + 5), (float)(pTop + 6), 16777215);
         }

         public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            if (pButton == 0) {
               this.select();
            }

            return false;
         }

         void select() {
            PresetsList.this.setSelected(this);
            Registry<Biome> registry = PresetFlatWorldScreen.this.parent.parent.worldGenSettingsComponent.registryHolder().registryOrThrow(Registry.BIOME_REGISTRY);
            PresetFlatWorldScreen.this.settings = this.preset.settings.apply(registry);
            PresetFlatWorldScreen.this.export.setValue(PresetFlatWorldScreen.save(registry, PresetFlatWorldScreen.this.settings));
            PresetFlatWorldScreen.this.export.moveCursorToStart();
         }

         private void blitSlot(PoseStack pPoseStack, int pX, int pY, Item pItem) {
            this.blitSlotBg(pPoseStack, pX + 1, pY + 1);
            PresetFlatWorldScreen.this.itemRenderer.renderGuiItem(new ItemStack(pItem), pX + 2, pY + 2);
         }

         private void blitSlotBg(PoseStack pPoseStack, int pX, int pY) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, GuiComponent.STATS_ICON_LOCATION);
            GuiComponent.blit(pPoseStack, pX, pY, PresetFlatWorldScreen.this.getBlitOffset(), 0.0F, 0.0F, 18, 18, 128, 128);
         }

         public Component getNarration() {
            return new TranslatableComponent("narrator.select", this.preset.getName());
         }
      }
   }
}