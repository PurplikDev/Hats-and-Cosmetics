package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Matrix4f;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VertexBuffer implements AutoCloseable {
   private int vertextBufferId;
   private int indexBufferId;
   private VertexFormat.IndexType indexType;
   private int arrayObjectId;
   private int indexCount;
   private VertexFormat.Mode mode;
   private boolean sequentialIndices;
   private VertexFormat format;

   public VertexBuffer() {
      RenderSystem.glGenBuffers((p_85928_) -> {
         this.vertextBufferId = p_85928_;
      });
      RenderSystem.glGenVertexArrays((p_166881_) -> {
         this.arrayObjectId = p_166881_;
      });
      RenderSystem.glGenBuffers((p_166872_) -> {
         this.indexBufferId = p_166872_;
      });
   }

   public void bind() {
      RenderSystem.glBindBuffer(34962, () -> {
         return this.vertextBufferId;
      });
      if (this.sequentialIndices) {
         RenderSystem.glBindBuffer(34963, () -> {
            RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(this.mode, this.indexCount);
            this.indexType = rendersystem$autostorageindexbuffer.type();
            return rendersystem$autostorageindexbuffer.name();
         });
      } else {
         RenderSystem.glBindBuffer(34963, () -> {
            return this.indexBufferId;
         });
      }

   }

   public void upload(BufferBuilder pBuilder) {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> {
            this.upload_(pBuilder);
         });
      } else {
         this.upload_(pBuilder);
      }

   }

   public CompletableFuture<Void> uploadLater(BufferBuilder pBuilder) {
      if (!RenderSystem.isOnRenderThread()) {
         return CompletableFuture.runAsync(() -> {
            this.upload_(pBuilder);
         }, (p_166874_) -> {
            RenderSystem.recordRenderCall(p_166874_::run);
         });
      } else {
         this.upload_(pBuilder);
         return CompletableFuture.completedFuture((Void)null);
      }
   }

   private void upload_(BufferBuilder pBuilder) {
      Pair<BufferBuilder.DrawState, ByteBuffer> pair = pBuilder.popNextBuffer();
      if (this.vertextBufferId != 0) {
         BufferUploader.reset();
         BufferBuilder.DrawState bufferbuilder$drawstate = pair.getFirst();
         ByteBuffer bytebuffer = pair.getSecond();
         int i = bufferbuilder$drawstate.vertexBufferSize();
         this.indexCount = bufferbuilder$drawstate.indexCount();
         this.indexType = bufferbuilder$drawstate.indexType();
         this.format = bufferbuilder$drawstate.format();
         this.mode = bufferbuilder$drawstate.mode();
         this.sequentialIndices = bufferbuilder$drawstate.sequentialIndex();
         this.bindVertexArray();
         this.bind();
         if (!bufferbuilder$drawstate.indexOnly()) {
            bytebuffer.limit(i);
            RenderSystem.glBufferData(34962, bytebuffer, 35044);
            bytebuffer.position(i);
         }

         if (!this.sequentialIndices) {
            bytebuffer.limit(bufferbuilder$drawstate.bufferSize());
            RenderSystem.glBufferData(34963, bytebuffer, 35044);
            bytebuffer.position(0);
         } else {
            bytebuffer.limit(bufferbuilder$drawstate.bufferSize());
            bytebuffer.position(0);
         }

         unbind();
         unbindVertexArray();
      }
   }

   private void bindVertexArray() {
      RenderSystem.glBindVertexArray(() -> {
         return this.arrayObjectId;
      });
   }

   public static void unbindVertexArray() {
      RenderSystem.glBindVertexArray(() -> {
         return 0;
      });
   }

   public void draw() {
      if (this.indexCount != 0) {
         RenderSystem.drawElements(this.mode.asGLMode, this.indexCount, this.indexType.asGLType);
      }
   }

   public void drawWithShader(Matrix4f pModelViewMatrix, Matrix4f pProjectionMatrix, ShaderInstance pShaderInstance) {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> {
            this._drawWithShader(pModelViewMatrix.copy(), pProjectionMatrix.copy(), pShaderInstance);
         });
      } else {
         this._drawWithShader(pModelViewMatrix, pProjectionMatrix, pShaderInstance);
      }

   }

   public void _drawWithShader(Matrix4f pModelViewMatrix, Matrix4f pProjectionMatrix, ShaderInstance pShaderInstance) {
      if (this.indexCount != 0) {
         RenderSystem.assertOnRenderThread();
         BufferUploader.reset();

         for(int i = 0; i < 12; ++i) {
            int j = RenderSystem.getShaderTexture(i);
            pShaderInstance.setSampler("Sampler" + i, j);
         }

         if (pShaderInstance.MODEL_VIEW_MATRIX != null) {
            pShaderInstance.MODEL_VIEW_MATRIX.set(pModelViewMatrix);
         }

         if (pShaderInstance.PROJECTION_MATRIX != null) {
            pShaderInstance.PROJECTION_MATRIX.set(pProjectionMatrix);
         }

         if (pShaderInstance.INVERSE_VIEW_ROTATION_MATRIX != null) {
            pShaderInstance.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
         }

         if (pShaderInstance.COLOR_MODULATOR != null) {
            pShaderInstance.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
         }

         if (pShaderInstance.FOG_START != null) {
            pShaderInstance.FOG_START.set(RenderSystem.getShaderFogStart());
         }

         if (pShaderInstance.FOG_END != null) {
            pShaderInstance.FOG_END.set(RenderSystem.getShaderFogEnd());
         }

         if (pShaderInstance.FOG_COLOR != null) {
            pShaderInstance.FOG_COLOR.set(RenderSystem.getShaderFogColor());
         }

         if (pShaderInstance.TEXTURE_MATRIX != null) {
            pShaderInstance.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
         }

         if (pShaderInstance.GAME_TIME != null) {
            pShaderInstance.GAME_TIME.set(RenderSystem.getShaderGameTime());
         }

         if (pShaderInstance.SCREEN_SIZE != null) {
            Window window = Minecraft.getInstance().getWindow();
            pShaderInstance.SCREEN_SIZE.set((float)window.getWidth(), (float)window.getHeight());
         }

         if (pShaderInstance.LINE_WIDTH != null && (this.mode == VertexFormat.Mode.LINES || this.mode == VertexFormat.Mode.LINE_STRIP)) {
            pShaderInstance.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
         }

         RenderSystem.setupShaderLights(pShaderInstance);
         this.bindVertexArray();
         this.bind();
         this.getFormat().setupBufferState();
         pShaderInstance.apply();
         RenderSystem.drawElements(this.mode.asGLMode, this.indexCount, this.indexType.asGLType);
         pShaderInstance.clear();
         this.getFormat().clearBufferState();
         unbind();
         unbindVertexArray();
      }
   }

   public void drawChunkLayer() {
      if (this.indexCount != 0) {
         RenderSystem.assertOnRenderThread();
         this.bindVertexArray();
         this.bind();
         this.format.setupBufferState();
         RenderSystem.drawElements(this.mode.asGLMode, this.indexCount, this.indexType.asGLType);
      }
   }

   public static void unbind() {
      RenderSystem.glBindBuffer(34962, () -> {
         return 0;
      });
      RenderSystem.glBindBuffer(34963, () -> {
         return 0;
      });
   }

   public void close() {
      if (this.indexBufferId >= 0) {
         RenderSystem.glDeleteBuffers(this.indexBufferId);
         this.indexBufferId = -1;
      }

      if (this.vertextBufferId > 0) {
         RenderSystem.glDeleteBuffers(this.vertextBufferId);
         this.vertextBufferId = 0;
      }

      if (this.arrayObjectId > 0) {
         RenderSystem.glDeleteVertexArrays(this.arrayObjectId);
         this.arrayObjectId = 0;
      }

   }

   public VertexFormat getFormat() {
      return this.format;
   }
}