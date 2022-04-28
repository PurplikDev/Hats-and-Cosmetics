package net.minecraft.client.gui.screens;

import com.ibm.icu.text.Collator;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CreateBuffetWorldScreen extends Screen {
   private static final Component BIOME_SELECT_INFO = new TranslatableComponent("createWorld.customize.buffet.biome");
   private final Screen parent;
   private final Consumer<Holder<Biome>> applySettings;
   final Registry<Biome> biomes;
   private CreateBuffetWorldScreen.BiomeList list;
   Holder<Biome> biome;
   private Button doneButton;

   public CreateBuffetWorldScreen(Screen pParent, RegistryAccess pRegistryAccess, Consumer<Holder<Biome>> pApplySettings, Holder<Biome> pBiome) {
      super(new TranslatableComponent("createWorld.customize.buffet.title"));
      this.parent = pParent;
      this.applySettings = pApplySettings;
      this.biome = pBiome;
      this.biomes = pRegistryAccess.registryOrThrow(Registry.BIOME_REGISTRY);
   }

   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   protected void init() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      this.list = new CreateBuffetWorldScreen.BiomeList();
      this.addWidget(this.list);
      this.doneButton = this.addRenderableWidget(new Button(this.width / 2 - 155, this.height - 28, 150, 20, CommonComponents.GUI_DONE, (p_95772_) -> {
         this.applySettings.accept(this.biome);
         this.minecraft.setScreen(this.parent);
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 5, this.height - 28, 150, 20, CommonComponents.GUI_CANCEL, (p_95761_) -> {
         this.minecraft.setScreen(this.parent);
      }));
      this.list.setSelected(this.list.children().stream().filter((p_95763_) -> {
         return Objects.equals(p_95763_.biome, this.biome);
      }).findFirst().orElse((CreateBuffetWorldScreen.BiomeList.Entry)null));
   }

   void updateButtonValidity() {
      this.doneButton.active = this.list.getSelected() != null;
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderDirtBackground(0);
      this.list.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 8, 16777215);
      drawCenteredString(pPoseStack, this.font, BIOME_SELECT_INFO, this.width / 2, 28, 10526880);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   @OnlyIn(Dist.CLIENT)
   class BiomeList extends ObjectSelectionList<CreateBuffetWorldScreen.BiomeList.Entry> {
      BiomeList() {
         super(CreateBuffetWorldScreen.this.minecraft, CreateBuffetWorldScreen.this.width, CreateBuffetWorldScreen.this.height, 40, CreateBuffetWorldScreen.this.height - 37, 16);
         Collator collator = Collator.getInstance(Locale.getDefault());
         CreateBuffetWorldScreen.this.biomes.holders().map((p_205389_) -> {
            return new CreateBuffetWorldScreen.BiomeList.Entry(p_205389_);
         }).sorted(Comparator.comparing((p_203142_) -> {
            return p_203142_.name.getString();
         }, collator)).forEach((p_203138_) -> {
            this.addEntry(p_203138_);
         });
      }

      protected boolean isFocused() {
         return CreateBuffetWorldScreen.this.getFocused() == this;
      }

      public void setSelected(@Nullable CreateBuffetWorldScreen.BiomeList.Entry pEntry) {
         super.setSelected(pEntry);
         if (pEntry != null) {
            CreateBuffetWorldScreen.this.biome = pEntry.biome;
         }

         CreateBuffetWorldScreen.this.updateButtonValidity();
      }

      @OnlyIn(Dist.CLIENT)
      class Entry extends ObjectSelectionList.Entry<CreateBuffetWorldScreen.BiomeList.Entry> {
         final Holder.Reference<Biome> biome;
         final Component name;

         public Entry(Holder.Reference<Biome> p_205392_) {
            this.biome = p_205392_;
            ResourceLocation resourcelocation = p_205392_.key().location();
            String s = "biome." + resourcelocation.getNamespace() + "." + resourcelocation.getPath();
            if (Language.getInstance().has(s)) {
               this.name = new TranslatableComponent(s);
            } else {
               this.name = new TextComponent(resourcelocation.toString());
            }

         }

         public Component getNarration() {
            return new TranslatableComponent("narrator.select", this.name);
         }

         public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
            GuiComponent.drawString(pPoseStack, CreateBuffetWorldScreen.this.font, this.name, pLeft + 5, pTop + 2, 16777215);
         }

         public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            if (pButton == 0) {
               BiomeList.this.setSelected(this);
               return true;
            } else {
               return false;
            }
         }
      }
   }
}