package net.minecraft.client.gui.screens.worldselection;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SelectWorldScreen extends Screen {
   private static final Logger LOGGER = LogManager.getLogger();
   protected final Screen lastScreen;
   @Nullable
   private List<FormattedCharSequence> toolTip;
   private Button deleteButton;
   private Button selectButton;
   private Button renameButton;
   private Button copyButton;
   protected EditBox searchBox;
   private WorldSelectionList list;

   public SelectWorldScreen(Screen pLastScreen) {
      super(new TranslatableComponent("selectWorld.title"));
      this.lastScreen = pLastScreen;
   }

   public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
      return super.mouseScrolled(pMouseX, pMouseY, pDelta);
   }

   public void tick() {
      this.searchBox.tick();
   }

   protected void init() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      this.searchBox = new EditBox(this.font, this.width / 2 - 100, 22, 200, 20, this.searchBox, new TranslatableComponent("selectWorld.search"));
      this.searchBox.setResponder((p_101362_) -> {
         this.list.refreshList(() -> {
            return p_101362_;
         }, false);
      });
      this.list = new WorldSelectionList(this, this.minecraft, this.width, this.height, 48, this.height - 64, 36, () -> {
         return this.searchBox.getValue();
      }, this.list);
      this.addWidget(this.searchBox);
      this.addWidget(this.list);
      this.selectButton = this.addRenderableWidget(new Button(this.width / 2 - 154, this.height - 52, 150, 20, new TranslatableComponent("selectWorld.select"), (p_101378_) -> {
         this.list.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::joinWorld);
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 4, this.height - 52, 150, 20, new TranslatableComponent("selectWorld.create"), (p_101376_) -> {
         this.minecraft.setScreen(CreateWorldScreen.create(this));
      }));
      this.renameButton = this.addRenderableWidget(new Button(this.width / 2 - 154, this.height - 28, 72, 20, new TranslatableComponent("selectWorld.edit"), (p_101373_) -> {
         this.list.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::editWorld);
      }));
      this.deleteButton = this.addRenderableWidget(new Button(this.width / 2 - 76, this.height - 28, 72, 20, new TranslatableComponent("selectWorld.delete"), (p_101366_) -> {
         this.list.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::deleteWorld);
      }));
      this.copyButton = this.addRenderableWidget(new Button(this.width / 2 + 4, this.height - 28, 72, 20, new TranslatableComponent("selectWorld.recreate"), (p_101360_) -> {
         this.list.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::recreateWorld);
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 82, this.height - 28, 72, 20, CommonComponents.GUI_CANCEL, (p_101356_) -> {
         this.minecraft.setScreen(this.lastScreen);
      }));
      this.updateButtonStatus(false);
      this.setInitialFocus(this.searchBox);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      return super.keyPressed(pKeyCode, pScanCode, pModifiers) ? true : this.searchBox.keyPressed(pKeyCode, pScanCode, pModifiers);
   }

   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   public boolean charTyped(char pCodePoint, int pModifiers) {
      return this.searchBox.charTyped(pCodePoint, pModifiers);
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.toolTip = null;
      this.list.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      this.searchBox.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 8, 16777215);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      if (this.toolTip != null) {
         this.renderTooltip(pPoseStack, this.toolTip, pMouseX, pMouseY);
      }

   }

   public void setToolTip(List<FormattedCharSequence> pToolTip) {
      this.toolTip = pToolTip;
   }

   public void updateButtonStatus(boolean pActive) {
      this.selectButton.active = pActive;
      this.deleteButton.active = pActive;
      this.renameButton.active = pActive;
      this.copyButton.active = pActive;
   }

   public void removed() {
      if (this.list != null) {
         this.list.children().forEach(WorldSelectionList.WorldListEntry::close);
      }

   }
}