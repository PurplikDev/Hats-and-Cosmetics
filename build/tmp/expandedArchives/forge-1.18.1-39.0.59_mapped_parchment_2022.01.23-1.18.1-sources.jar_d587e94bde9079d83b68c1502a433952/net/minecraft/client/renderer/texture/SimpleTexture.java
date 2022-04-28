package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.Closeable;
import java.io.IOException;
import javax.annotation.Nullable;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SimpleTexture extends AbstractTexture {
   static final Logger LOGGER = LogManager.getLogger();
   protected final ResourceLocation location;

   public SimpleTexture(ResourceLocation pLocation) {
      this.location = pLocation;
   }

   public void load(ResourceManager pResourceManager) throws IOException {
      SimpleTexture.TextureImage simpletexture$textureimage = this.getTextureImage(pResourceManager);
      simpletexture$textureimage.throwIfError();
      TextureMetadataSection texturemetadatasection = simpletexture$textureimage.getTextureMetadata();
      boolean flag;
      boolean flag1;
      if (texturemetadatasection != null) {
         flag = texturemetadatasection.isBlur();
         flag1 = texturemetadatasection.isClamp();
      } else {
         flag = false;
         flag1 = false;
      }

      NativeImage nativeimage = simpletexture$textureimage.getImage();
      if (!RenderSystem.isOnRenderThreadOrInit()) {
         RenderSystem.recordRenderCall(() -> {
            this.doLoad(nativeimage, flag, flag1);
         });
      } else {
         this.doLoad(nativeimage, flag, flag1);
      }

   }

   private void doLoad(NativeImage pImage, boolean pBlur, boolean pClamp) {
      TextureUtil.prepareImage(this.getId(), 0, pImage.getWidth(), pImage.getHeight());
      pImage.upload(0, 0, 0, 0, 0, pImage.getWidth(), pImage.getHeight(), pBlur, pClamp, false, true);
   }

   protected SimpleTexture.TextureImage getTextureImage(ResourceManager pResourceManager) {
      return SimpleTexture.TextureImage.load(pResourceManager, this.location);
   }

   @OnlyIn(Dist.CLIENT)
   protected static class TextureImage implements Closeable {
      @Nullable
      private final TextureMetadataSection metadata;
      @Nullable
      private final NativeImage image;
      @Nullable
      private final IOException exception;

      public TextureImage(IOException pException) {
         this.exception = pException;
         this.metadata = null;
         this.image = null;
      }

      public TextureImage(@Nullable TextureMetadataSection pMetadata, NativeImage pImage) {
         this.exception = null;
         this.metadata = pMetadata;
         this.image = pImage;
      }

      public static SimpleTexture.TextureImage load(ResourceManager pResourceManager, ResourceLocation pLocation) {
         try {
            Resource resource = pResourceManager.getResource(pLocation);

            SimpleTexture.TextureImage $$5;
            try {
               NativeImage nativeimage = NativeImage.read(resource.getInputStream());
               TextureMetadataSection texturemetadatasection = null;

               try {
                  texturemetadatasection = resource.getMetadata(TextureMetadataSection.SERIALIZER);
               } catch (RuntimeException runtimeexception) {
                  SimpleTexture.LOGGER.warn("Failed reading metadata of: {}", pLocation, runtimeexception);
               }

               $$5 = new SimpleTexture.TextureImage(texturemetadatasection, nativeimage);
            } catch (Throwable throwable1) {
               if (resource != null) {
                  try {
                     resource.close();
                  } catch (Throwable throwable) {
                     throwable1.addSuppressed(throwable);
                  }
               }

               throw throwable1;
            }

            if (resource != null) {
               resource.close();
            }

            return $$5;
         } catch (IOException ioexception) {
            return new SimpleTexture.TextureImage(ioexception);
         }
      }

      @Nullable
      public TextureMetadataSection getTextureMetadata() {
         return this.metadata;
      }

      public NativeImage getImage() throws IOException {
         if (this.exception != null) {
            throw this.exception;
         } else {
            return this.image;
         }
      }

      public void close() {
         if (this.image != null) {
            this.image.close();
         }

      }

      public void throwIfError() throws IOException {
         if (this.exception != null) {
            throw this.exception;
         }
      }
   }
}