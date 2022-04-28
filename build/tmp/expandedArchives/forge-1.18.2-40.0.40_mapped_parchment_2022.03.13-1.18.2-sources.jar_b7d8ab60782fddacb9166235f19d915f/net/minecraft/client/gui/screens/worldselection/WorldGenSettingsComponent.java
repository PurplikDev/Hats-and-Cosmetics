package net.minecraft.client.gui.screens.worldselection;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class WorldGenSettingsComponent implements Widget {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component CUSTOM_WORLD_DESCRIPTION = new TranslatableComponent("generator.custom");
   private static final Component AMPLIFIED_HELP_TEXT = new TranslatableComponent("generator.amplified.info");
   private static final Component MAP_FEATURES_INFO = new TranslatableComponent("selectWorld.mapFeatures.info");
   private static final Component SELECT_FILE_PROMPT = new TranslatableComponent("selectWorld.import_worldgen_settings.select_file");
   private MultiLineLabel amplifiedWorldInfo = MultiLineLabel.EMPTY;
   private Font font;
   private int width;
   private EditBox seedEdit;
   private CycleButton<Boolean> featuresButton;
   private CycleButton<Boolean> bonusItemsButton;
   private CycleButton<WorldPreset> typeButton;
   private Button customWorldDummyButton;
   private Button customizeTypeButton;
   private Button importSettingsButton;
   private RegistryAccess.Frozen registryHolder;
   private WorldGenSettings settings;
   private Optional<WorldPreset> preset;
   private OptionalLong seed;

   public WorldGenSettingsComponent(RegistryAccess.Frozen pRegistryHolder, WorldGenSettings pSettings, Optional<WorldPreset> pPreset, OptionalLong pSeed) {
      this.registryHolder = pRegistryHolder;
      this.settings = pSettings;
      this.preset = pPreset;
      this.seed = pSeed;
   }

   public void init(CreateWorldScreen pCreateWorldScreen, Minecraft pMinecraft, Font pFont) {
      this.font = pFont;
      this.width = pCreateWorldScreen.width;
      this.seedEdit = new EditBox(this.font, this.width / 2 - 100, 60, 200, 20, new TranslatableComponent("selectWorld.enterSeed"));
      this.seedEdit.setValue(toString(this.seed));
      this.seedEdit.setResponder((p_101465_) -> {
         this.seed = WorldGenSettings.parseSeed(this.seedEdit.getValue());
      });
      pCreateWorldScreen.addWidget(this.seedEdit);
      int i = this.width / 2 - 155;
      int j = this.width / 2 + 5;
      this.featuresButton = pCreateWorldScreen.addRenderableWidget(CycleButton.onOffBuilder(this.settings.generateFeatures()).withCustomNarration((p_170280_) -> {
         return CommonComponents.joinForNarration(p_170280_.createDefaultNarrationMessage(), new TranslatableComponent("selectWorld.mapFeatures.info"));
      }).create(i, 100, 150, 20, new TranslatableComponent("selectWorld.mapFeatures"), (p_170282_, p_170283_) -> {
         this.settings = this.settings.withFeaturesToggled();
      }));
      this.featuresButton.visible = false;
      this.typeButton = pCreateWorldScreen.addRenderableWidget(CycleButton.builder(WorldPreset::description).withValues(WorldPreset.PRESETS.stream().filter(WorldPreset::isVisibleByDefault).collect(Collectors.toList()), WorldPreset.PRESETS).withCustomNarration((p_170264_) -> {
         return p_170264_.getValue() == WorldPreset.AMPLIFIED ? CommonComponents.joinForNarration(p_170264_.createDefaultNarrationMessage(), AMPLIFIED_HELP_TEXT) : p_170264_.createDefaultNarrationMessage();
      }).create(j, 100, 150, 20, new TranslatableComponent("selectWorld.mapType"), (p_170274_, p_170275_) -> {
         this.preset = Optional.of(p_170275_);
         this.settings = p_170275_.create(this.registryHolder, this.settings.seed(), this.settings.generateFeatures(), this.settings.generateBonusChest());
         pCreateWorldScreen.refreshWorldGenSettingsVisibility();
      }));
      this.preset.ifPresent(this.typeButton::setValue);
      this.typeButton.visible = false;
      this.customWorldDummyButton = pCreateWorldScreen.addRenderableWidget(new Button(j, 100, 150, 20, CommonComponents.optionNameValue(new TranslatableComponent("selectWorld.mapType"), CUSTOM_WORLD_DESCRIPTION), (p_170262_) -> {
      }));
      this.customWorldDummyButton.active = false;
      this.customWorldDummyButton.visible = false;
      this.customizeTypeButton = pCreateWorldScreen.addRenderableWidget(new Button(j, 120, 150, 20, new TranslatableComponent("selectWorld.customizeType"), (p_170248_) -> {
         WorldPreset.PresetEditor worldpreset$preseteditor = WorldPreset.EDITORS.get(this.preset);
         worldpreset$preseteditor = net.minecraftforge.client.ForgeHooksClient.getPresetEditor(this.preset, worldpreset$preseteditor);
         if (worldpreset$preseteditor != null) {
            pMinecraft.setScreen(worldpreset$preseteditor.createEditScreen(pCreateWorldScreen, this.settings));
         }

      }));
      this.customizeTypeButton.visible = false;
      this.bonusItemsButton = pCreateWorldScreen.addRenderableWidget(CycleButton.onOffBuilder(this.settings.generateBonusChest() && !pCreateWorldScreen.hardCore).create(i, 151, 150, 20, new TranslatableComponent("selectWorld.bonusItems"), (p_170266_, p_170267_) -> {
         this.settings = this.settings.withBonusChestToggled();
      }));
      this.bonusItemsButton.visible = false;
      this.importSettingsButton = pCreateWorldScreen.addRenderableWidget(new Button(i, 185, 150, 20, new TranslatableComponent("selectWorld.import_worldgen_settings"), (p_170271_) -> {
         String s = TinyFileDialogs.tinyfd_openFileDialog(SELECT_FILE_PROMPT.getString(), (CharSequence)null, (PointerBuffer)null, (CharSequence)null, false);
         if (s != null) {
            RegistryAccess.Writable registryaccess$writable = RegistryAccess.builtinCopy();
            PackRepository packrepository = new PackRepository(PackType.SERVER_DATA, new ServerPacksSource(), new FolderRepositorySource(pCreateWorldScreen.getTempDataPackDir().toFile(), PackSource.WORLD));

            label74: {
               DataResult<WorldGenSettings> dataresult;
               try {
                  MinecraftServer.configurePackRepository(packrepository, pCreateWorldScreen.dataPacks, false);
                  CloseableResourceManager closeableresourcemanager = new MultiPackResourceManager(PackType.SERVER_DATA, packrepository.openAllSelected());

                  label70: {
                     try {
                        DynamicOps<JsonElement> dynamicops = RegistryOps.createAndLoad(JsonOps.INSTANCE, registryaccess$writable, closeableresourcemanager);

                        try {
                           BufferedReader bufferedreader = Files.newBufferedReader(Paths.get(s));

                           try {
                              JsonElement jsonelement = JsonParser.parseReader(bufferedreader);
                              dataresult = WorldGenSettings.CODEC.parse(dynamicops, jsonelement);
                           } catch (Throwable throwable3) {
                              if (bufferedreader != null) {
                                 try {
                                    bufferedreader.close();
                                 } catch (Throwable throwable2) {
                                    throwable3.addSuppressed(throwable2);
                                 }
                              }

                              throw throwable3;
                           }

                           if (bufferedreader != null) {
                              bufferedreader.close();
                           }
                        } catch (Exception exception) {
                           dataresult = DataResult.error("Failed to parse file: " + exception.getMessage());
                        }

                        if (!dataresult.error().isPresent()) {
                           break label70;
                        }

                        Component component1 = new TranslatableComponent("selectWorld.import_worldgen_settings.failure");
                        String s1 = dataresult.error().get().message();
                        LOGGER.error("Error parsing world settings: {}", (Object)s1);
                        Component component = new TextComponent(s1);
                        pMinecraft.getToasts().addToast(SystemToast.multiline(pMinecraft, SystemToast.SystemToastIds.WORLD_GEN_SETTINGS_TRANSFER, component1, component));
                     } catch (Throwable throwable4) {
                        try {
                           closeableresourcemanager.close();
                        } catch (Throwable throwable1) {
                           throwable4.addSuppressed(throwable1);
                        }

                        throw throwable4;
                     }

                     closeableresourcemanager.close();
                     break label74;
                  }

                  closeableresourcemanager.close();
               } catch (Throwable throwable5) {
                  try {
                     packrepository.close();
                  } catch (Throwable throwable) {
                     throwable5.addSuppressed(throwable);
                  }

                  throw throwable5;
               }

               packrepository.close();
               Lifecycle lifecycle = dataresult.lifecycle();
               dataresult.resultOrPartial(LOGGER::error).ifPresent((p_205461_) -> {
                  BooleanConsumer booleanconsumer = (p_205467_) -> {
                     pMinecraft.setScreen(pCreateWorldScreen);
                     if (p_205467_) {
                        this.importSettings(registryaccess$writable.freeze(), p_205461_);
                     }

                  };
                  if (lifecycle == Lifecycle.stable()) {
                     this.importSettings(registryaccess$writable.freeze(), p_205461_);
                  } else if (lifecycle == Lifecycle.experimental()) {
                     pMinecraft.setScreen(new ConfirmScreen(booleanconsumer, new TranslatableComponent("selectWorld.import_worldgen_settings.experimental.title"), new TranslatableComponent("selectWorld.import_worldgen_settings.experimental.question")));
                  } else {
                     pMinecraft.setScreen(new ConfirmScreen(booleanconsumer, new TranslatableComponent("selectWorld.import_worldgen_settings.deprecated.title"), new TranslatableComponent("selectWorld.import_worldgen_settings.deprecated.question")));
                  }

               });
               return;
            }

            packrepository.close();
         }
      }));
      this.importSettingsButton.visible = false;
      this.amplifiedWorldInfo = MultiLineLabel.create(pFont, AMPLIFIED_HELP_TEXT, this.typeButton.getWidth());
   }

   private void importSettings(RegistryAccess.Frozen pRegistryHolder, WorldGenSettings pSettings) {
      this.registryHolder = pRegistryHolder;
      this.settings = pSettings;
      this.preset = WorldPreset.of(pSettings);
      this.selectWorldTypeButton(true);
      this.seed = OptionalLong.of(pSettings.seed());
      this.seedEdit.setValue(toString(this.seed));
   }

   public void tick() {
      this.seedEdit.tick();
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      if (this.featuresButton.visible) {
         this.font.drawShadow(pPoseStack, MAP_FEATURES_INFO, (float)(this.width / 2 - 150), 122.0F, -6250336);
      }

      this.seedEdit.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      if (this.preset.equals(Optional.of(WorldPreset.AMPLIFIED))) {
         this.amplifiedWorldInfo.renderLeftAligned(pPoseStack, this.typeButton.x + 2, this.typeButton.y + 22, 9, 10526880);
      }

   }

   public void updateSettings(WorldGenSettings pSettings) {
      this.settings = pSettings;
   }

   private static String toString(OptionalLong pSeed) {
      return pSeed.isPresent() ? Long.toString(pSeed.getAsLong()) : "";
   }

   public WorldGenSettings makeSettings(boolean p_101455_) {
      OptionalLong optionallong = WorldGenSettings.parseSeed(this.seedEdit.getValue());
      return this.settings.withSeed(p_101455_, optionallong);
   }

   public boolean isDebug() {
      return this.settings.isDebug();
   }

   public void setVisibility(boolean pVisible) {
      this.selectWorldTypeButton(pVisible);
      if (this.settings.isDebug()) {
         this.featuresButton.visible = false;
         this.bonusItemsButton.visible = false;
         this.customizeTypeButton.visible = false;
         this.importSettingsButton.visible = false;
      } else {
         this.featuresButton.visible = pVisible;
         this.bonusItemsButton.visible = pVisible;
         this.customizeTypeButton.visible = pVisible && (WorldPreset.EDITORS.containsKey(this.preset) || net.minecraftforge.client.ForgeHooksClient.hasPresetEditor(this.preset));
         this.importSettingsButton.visible = pVisible;
      }

      this.seedEdit.setVisible(pVisible);
   }

   private void selectWorldTypeButton(boolean pVisible) {
      if (this.preset.isPresent()) {
         this.typeButton.visible = pVisible;
         this.customWorldDummyButton.visible = false;
      } else {
         this.typeButton.visible = false;
         this.customWorldDummyButton.visible = pVisible;
      }

   }

   public RegistryAccess registryHolder() {
      return this.registryHolder;
   }

   void updateDataPacks(WorldStem p_205472_) {
      this.settings = p_205472_.worldData().worldGenSettings();
      this.registryHolder = p_205472_.registryAccess();
   }

   public void switchToHardcore() {
      this.bonusItemsButton.active = false;
      this.bonusItemsButton.setValue(false);
   }

   public void switchOutOfHardcode() {
      this.bonusItemsButton.active = true;
      this.bonusItemsButton.setValue(this.settings.generateBonusChest());
   }
}
