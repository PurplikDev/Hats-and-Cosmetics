package net.minecraft.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Screenshot {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
   private int rowHeight;
   private final DataOutputStream outputStream;
   private final byte[] bytes;
   private final int width;
   private final int height;
   private File file;

   /**
    * Saves a screenshot in the game directory with a time-stamped filename.
    */
   public static void grab(File pGameDirectory, RenderTarget pBuffer, Consumer<Component> pMessageConsumer) {
      grab(pGameDirectory, (String)null, pBuffer, pMessageConsumer);
   }

   /**
    * Saves a screenshot in the game directory with the given file name (or null to generate a time-stamped name).
    */
   public static void grab(File pGameDirectory, @Nullable String pScreenshotName, RenderTarget pBuffer, Consumer<Component> pMessageConsumer) {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> {
            _grab(pGameDirectory, pScreenshotName, pBuffer, pMessageConsumer);
         });
      } else {
         _grab(pGameDirectory, pScreenshotName, pBuffer, pMessageConsumer);
      }

   }

   private static void _grab(File pGameDirectory, @Nullable String pScreenshotName, RenderTarget pBuffer, Consumer<Component> pMessageConsumer) {
      NativeImage nativeimage = takeScreenshot(pBuffer);
      File file1 = new File(pGameDirectory, "screenshots");
      file1.mkdir();
      File file2;
      if (pScreenshotName == null) {
         file2 = getFile(file1);
      } else {
         file2 = new File(file1, pScreenshotName);
      }

      net.minecraftforge.client.event.ScreenshotEvent event = net.minecraftforge.client.ForgeHooksClient.onScreenshot(nativeimage, file2);
      if (event.isCanceled()) {
         pMessageConsumer.accept(event.getCancelMessage());
         return;
      }
      final File target = event.getScreenshotFile();

      Util.ioPool().execute(() -> {
         try {
            nativeimage.writeToFile(target);
            Component component = (new TextComponent(file2.getName())).withStyle(ChatFormatting.UNDERLINE).withStyle((p_168608_) -> {
               return p_168608_.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, target.getAbsolutePath()));
            });
            if (event.getResultMessage() != null)
               pMessageConsumer.accept(event.getResultMessage());
            else
               pMessageConsumer.accept(new TranslatableComponent("screenshot.success", component));
         } catch (Exception exception) {
            LOGGER.warn("Couldn't save screenshot", (Throwable)exception);
            pMessageConsumer.accept(new TranslatableComponent("screenshot.failure", exception.getMessage()));
         } finally {
            nativeimage.close();
         }

      });
   }

   public static NativeImage takeScreenshot(RenderTarget pFramebuffer) {
      int i = pFramebuffer.width;
      int j = pFramebuffer.height;
      NativeImage nativeimage = new NativeImage(i, j, false);
      RenderSystem.bindTexture(pFramebuffer.getColorTextureId());
      nativeimage.downloadTexture(0, true);
      nativeimage.flipY();
      return nativeimage;
   }

   /**
    * Creates a unique PNG file in the given directory named by a timestamp.  Handles cases where the timestamp alone is
    * not enough to create a uniquely named file, though it still might suffer from an unlikely race condition where the
    * filename was unique when this method was called, but another process or thread created a file at the same path
    * immediately after this method returned.
    */
   private static File getFile(File pGameDirectory) {
      String s = DATE_FORMAT.format(new Date());
      int i = 1;

      while(true) {
         File file1 = new File(pGameDirectory, s + (i == 1 ? "" : "_" + i) + ".png");
         if (!file1.exists()) {
            return file1;
         }

         ++i;
      }
   }

   public Screenshot(File pGameDirectory, int pWidth, int pHeight, int pRowHeight) throws IOException {
      this.width = pWidth;
      this.height = pHeight;
      this.rowHeight = pRowHeight;
      File file1 = new File(pGameDirectory, "screenshots");
      file1.mkdir();
      String s = "huge_" + DATE_FORMAT.format(new Date());

      for(int i = 1; (this.file = new File(file1, s + (i == 1 ? "" : "_" + i) + ".tga")).exists(); ++i) {
      }

      byte[] abyte = new byte[18];
      abyte[2] = 2;
      abyte[12] = (byte)(pWidth % 256);
      abyte[13] = (byte)(pWidth / 256);
      abyte[14] = (byte)(pHeight % 256);
      abyte[15] = (byte)(pHeight / 256);
      abyte[16] = 24;
      this.bytes = new byte[pWidth * pRowHeight * 3];
      this.outputStream = new DataOutputStream(new FileOutputStream(this.file));
      this.outputStream.write(abyte);
   }

   public void addRegion(ByteBuffer p_168610_, int p_168611_, int p_168612_, int p_168613_, int p_168614_) {
      int i = p_168613_;
      int j = p_168614_;
      if (p_168613_ > this.width - p_168611_) {
         i = this.width - p_168611_;
      }

      if (p_168614_ > this.height - p_168612_) {
         j = this.height - p_168612_;
      }

      this.rowHeight = j;

      for(int k = 0; k < j; ++k) {
         p_168610_.position((p_168614_ - j) * p_168613_ * 3 + k * p_168613_ * 3);
         int l = (p_168611_ + k * this.width) * 3;
         p_168610_.get(this.bytes, l, i * 3);
      }

   }

   public void saveRow() throws IOException {
      this.outputStream.write(this.bytes, 0, this.width * 3 * this.rowHeight);
   }

   public File close() throws IOException {
      this.outputStream.close();
      return this.file;
   }
}
