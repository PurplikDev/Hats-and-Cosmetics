package com.mojang.blaze3d.shaders;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class Uniform extends AbstractUniform implements AutoCloseable {
   private static final Logger LOGGER = LogManager.getLogger();
   public static final int UT_INT1 = 0;
   public static final int UT_INT2 = 1;
   public static final int UT_INT3 = 2;
   public static final int UT_INT4 = 3;
   public static final int UT_FLOAT1 = 4;
   public static final int UT_FLOAT2 = 5;
   public static final int UT_FLOAT3 = 6;
   public static final int UT_FLOAT4 = 7;
   public static final int UT_MAT2 = 8;
   public static final int UT_MAT3 = 9;
   public static final int UT_MAT4 = 10;
   private static final boolean TRANSPOSE_MATRICIES = false;
   private int location;
   private final int count;
   private final int type;
   private final IntBuffer intValues;
   private final FloatBuffer floatValues;
   private final String name;
   private boolean dirty;
   private final Shader parent;

   public Uniform(String pName, int pType, int pCount, Shader pParent) {
      this.name = pName;
      this.count = pCount;
      this.type = pType;
      this.parent = pParent;
      if (pType <= 3) {
         this.intValues = MemoryUtil.memAllocInt(pCount);
         this.floatValues = null;
      } else {
         this.intValues = null;
         this.floatValues = MemoryUtil.memAllocFloat(pCount);
      }

      this.location = -1;
      this.markDirty();
   }

   public static int glGetUniformLocation(int pProgram, CharSequence pName) {
      return GlStateManager._glGetUniformLocation(pProgram, pName);
   }

   public static void uploadInteger(int pLocation, int pValue) {
      RenderSystem.glUniform1i(pLocation, pValue);
   }

   public static int glGetAttribLocation(int pProgram, CharSequence pName) {
      return GlStateManager._glGetAttribLocation(pProgram, pName);
   }

   public static void glBindAttribLocation(int pProgram, int pIndex, CharSequence pName) {
      GlStateManager._glBindAttribLocation(pProgram, pIndex, pName);
   }

   public void close() {
      if (this.intValues != null) {
         MemoryUtil.memFree(this.intValues);
      }

      if (this.floatValues != null) {
         MemoryUtil.memFree(this.floatValues);
      }

   }

   private void markDirty() {
      this.dirty = true;
      if (this.parent != null) {
         this.parent.markDirty();
      }

   }

   public static int getTypeFromString(String pTypeName) {
      int i = -1;
      if ("int".equals(pTypeName)) {
         i = 0;
      } else if ("float".equals(pTypeName)) {
         i = 4;
      } else if (pTypeName.startsWith("matrix")) {
         if (pTypeName.endsWith("2x2")) {
            i = 8;
         } else if (pTypeName.endsWith("3x3")) {
            i = 9;
         } else if (pTypeName.endsWith("4x4")) {
            i = 10;
         }
      }

      return i;
   }

   public void setLocation(int pLocation) {
      this.location = pLocation;
   }

   public String getName() {
      return this.name;
   }

   public final void set(float pX) {
      this.floatValues.position(0);
      this.floatValues.put(0, pX);
      this.markDirty();
   }

   public final void set(float pX, float pY) {
      this.floatValues.position(0);
      this.floatValues.put(0, pX);
      this.floatValues.put(1, pY);
      this.markDirty();
   }

   public final void set(int pIndex, float pValue) {
      this.floatValues.position(0);
      this.floatValues.put(pIndex, pValue);
      this.markDirty();
   }

   public final void set(float pX, float pY, float pZ) {
      this.floatValues.position(0);
      this.floatValues.put(0, pX);
      this.floatValues.put(1, pY);
      this.floatValues.put(2, pZ);
      this.markDirty();
   }

   public final void set(Vector3f pVector) {
      this.floatValues.position(0);
      this.floatValues.put(0, pVector.x());
      this.floatValues.put(1, pVector.y());
      this.floatValues.put(2, pVector.z());
      this.markDirty();
   }

   public final void set(float pX, float pY, float pZ, float pW) {
      this.floatValues.position(0);
      this.floatValues.put(pX);
      this.floatValues.put(pY);
      this.floatValues.put(pZ);
      this.floatValues.put(pW);
      this.floatValues.flip();
      this.markDirty();
   }

   public final void set(Vector4f pVector) {
      this.floatValues.position(0);
      this.floatValues.put(0, pVector.x());
      this.floatValues.put(1, pVector.y());
      this.floatValues.put(2, pVector.z());
      this.floatValues.put(3, pVector.w());
      this.markDirty();
   }

   public final void setSafe(float pX, float pY, float pZ, float pW) {
      this.floatValues.position(0);
      if (this.type >= 4) {
         this.floatValues.put(0, pX);
      }

      if (this.type >= 5) {
         this.floatValues.put(1, pY);
      }

      if (this.type >= 6) {
         this.floatValues.put(2, pZ);
      }

      if (this.type >= 7) {
         this.floatValues.put(3, pW);
      }

      this.markDirty();
   }

   public final void setSafe(int pX, int pY, int pZ, int pW) {
      this.intValues.position(0);
      if (this.type >= 0) {
         this.intValues.put(0, pX);
      }

      if (this.type >= 1) {
         this.intValues.put(1, pY);
      }

      if (this.type >= 2) {
         this.intValues.put(2, pZ);
      }

      if (this.type >= 3) {
         this.intValues.put(3, pW);
      }

      this.markDirty();
   }

   public final void set(int pX) {
      this.intValues.position(0);
      this.intValues.put(0, pX);
      this.markDirty();
   }

   public final void set(int pX, int pY) {
      this.intValues.position(0);
      this.intValues.put(0, pX);
      this.intValues.put(1, pY);
      this.markDirty();
   }

   public final void set(int pX, int pY, int pZ) {
      this.intValues.position(0);
      this.intValues.put(0, pX);
      this.intValues.put(1, pY);
      this.intValues.put(2, pZ);
      this.markDirty();
   }

   public final void set(int pX, int pY, int pZ, int pW) {
      this.intValues.position(0);
      this.intValues.put(0, pX);
      this.intValues.put(1, pY);
      this.intValues.put(2, pZ);
      this.intValues.put(3, pW);
      this.markDirty();
   }

   public final void set(float[] pValueArray) {
      if (pValueArray.length < this.count) {
         LOGGER.warn("Uniform.set called with a too-small value array (expected {}, got {}). Ignoring.", this.count, pValueArray.length);
      } else {
         this.floatValues.position(0);
         this.floatValues.put(pValueArray);
         this.floatValues.position(0);
         this.markDirty();
      }
   }

   public final void setMat2x2(float pX, float pY, float pZ, float pW) {
      this.floatValues.position(0);
      this.floatValues.put(0, pX);
      this.floatValues.put(1, pY);
      this.floatValues.put(2, pZ);
      this.floatValues.put(3, pW);
      this.markDirty();
   }

   public final void setMat2x3(float p_166643_, float p_166644_, float p_166645_, float p_166646_, float p_166647_, float p_166648_) {
      this.floatValues.position(0);
      this.floatValues.put(0, p_166643_);
      this.floatValues.put(1, p_166644_);
      this.floatValues.put(2, p_166645_);
      this.floatValues.put(3, p_166646_);
      this.floatValues.put(4, p_166647_);
      this.floatValues.put(5, p_166648_);
      this.markDirty();
   }

   public final void setMat2x4(float p_166650_, float p_166651_, float p_166652_, float p_166653_, float p_166654_, float p_166655_, float p_166656_, float p_166657_) {
      this.floatValues.position(0);
      this.floatValues.put(0, p_166650_);
      this.floatValues.put(1, p_166651_);
      this.floatValues.put(2, p_166652_);
      this.floatValues.put(3, p_166653_);
      this.floatValues.put(4, p_166654_);
      this.floatValues.put(5, p_166655_);
      this.floatValues.put(6, p_166656_);
      this.floatValues.put(7, p_166657_);
      this.markDirty();
   }

   public final void setMat3x2(float p_166719_, float p_166720_, float p_166721_, float p_166722_, float p_166723_, float p_166724_) {
      this.floatValues.position(0);
      this.floatValues.put(0, p_166719_);
      this.floatValues.put(1, p_166720_);
      this.floatValues.put(2, p_166721_);
      this.floatValues.put(3, p_166722_);
      this.floatValues.put(4, p_166723_);
      this.floatValues.put(5, p_166724_);
      this.markDirty();
   }

   public final void setMat3x3(float p_166659_, float p_166660_, float p_166661_, float p_166662_, float p_166663_, float p_166664_, float p_166665_, float p_166666_, float p_166667_) {
      this.floatValues.position(0);
      this.floatValues.put(0, p_166659_);
      this.floatValues.put(1, p_166660_);
      this.floatValues.put(2, p_166661_);
      this.floatValues.put(3, p_166662_);
      this.floatValues.put(4, p_166663_);
      this.floatValues.put(5, p_166664_);
      this.floatValues.put(6, p_166665_);
      this.floatValues.put(7, p_166666_);
      this.floatValues.put(8, p_166667_);
      this.markDirty();
   }

   public final void setMat3x4(float p_166669_, float p_166670_, float p_166671_, float p_166672_, float p_166673_, float p_166674_, float p_166675_, float p_166676_, float p_166677_, float p_166678_, float p_166679_, float p_166680_) {
      this.floatValues.position(0);
      this.floatValues.put(0, p_166669_);
      this.floatValues.put(1, p_166670_);
      this.floatValues.put(2, p_166671_);
      this.floatValues.put(3, p_166672_);
      this.floatValues.put(4, p_166673_);
      this.floatValues.put(5, p_166674_);
      this.floatValues.put(6, p_166675_);
      this.floatValues.put(7, p_166676_);
      this.floatValues.put(8, p_166677_);
      this.floatValues.put(9, p_166678_);
      this.floatValues.put(10, p_166679_);
      this.floatValues.put(11, p_166680_);
      this.markDirty();
   }

   public final void setMat4x2(float p_166726_, float p_166727_, float p_166728_, float p_166729_, float p_166730_, float p_166731_, float p_166732_, float p_166733_) {
      this.floatValues.position(0);
      this.floatValues.put(0, p_166726_);
      this.floatValues.put(1, p_166727_);
      this.floatValues.put(2, p_166728_);
      this.floatValues.put(3, p_166729_);
      this.floatValues.put(4, p_166730_);
      this.floatValues.put(5, p_166731_);
      this.floatValues.put(6, p_166732_);
      this.floatValues.put(7, p_166733_);
      this.markDirty();
   }

   public final void setMat4x3(float p_166735_, float p_166736_, float p_166737_, float p_166738_, float p_166739_, float p_166740_, float p_166741_, float p_166742_, float p_166743_, float p_166744_, float p_166745_, float p_166746_) {
      this.floatValues.position(0);
      this.floatValues.put(0, p_166735_);
      this.floatValues.put(1, p_166736_);
      this.floatValues.put(2, p_166737_);
      this.floatValues.put(3, p_166738_);
      this.floatValues.put(4, p_166739_);
      this.floatValues.put(5, p_166740_);
      this.floatValues.put(6, p_166741_);
      this.floatValues.put(7, p_166742_);
      this.floatValues.put(8, p_166743_);
      this.floatValues.put(9, p_166744_);
      this.floatValues.put(10, p_166745_);
      this.floatValues.put(11, p_166746_);
      this.markDirty();
   }

   public final void setMat4x4(float p_166682_, float p_166683_, float p_166684_, float p_166685_, float p_166686_, float p_166687_, float p_166688_, float p_166689_, float p_166690_, float p_166691_, float p_166692_, float p_166693_, float p_166694_, float p_166695_, float p_166696_, float p_166697_) {
      this.floatValues.position(0);
      this.floatValues.put(0, p_166682_);
      this.floatValues.put(1, p_166683_);
      this.floatValues.put(2, p_166684_);
      this.floatValues.put(3, p_166685_);
      this.floatValues.put(4, p_166686_);
      this.floatValues.put(5, p_166687_);
      this.floatValues.put(6, p_166688_);
      this.floatValues.put(7, p_166689_);
      this.floatValues.put(8, p_166690_);
      this.floatValues.put(9, p_166691_);
      this.floatValues.put(10, p_166692_);
      this.floatValues.put(11, p_166693_);
      this.floatValues.put(12, p_166694_);
      this.floatValues.put(13, p_166695_);
      this.floatValues.put(14, p_166696_);
      this.floatValues.put(15, p_166697_);
      this.markDirty();
   }

   public final void set(Matrix4f pMatrix) {
      this.floatValues.position(0);
      pMatrix.store(this.floatValues);
      this.markDirty();
   }

   public final void set(Matrix3f p_200935_) {
      this.floatValues.position(0);
      p_200935_.store(this.floatValues);
      this.markDirty();
   }

   public void upload() {
      if (!this.dirty) {
      }

      this.dirty = false;
      if (this.type <= 3) {
         this.uploadAsInteger();
      } else if (this.type <= 7) {
         this.uploadAsFloat();
      } else {
         if (this.type > 10) {
            LOGGER.warn("Uniform.upload called, but type value ({}) is not a valid type. Ignoring.", (int)this.type);
            return;
         }

         this.uploadAsMatrix();
      }

   }

   private void uploadAsInteger() {
      this.intValues.rewind();
      switch(this.type) {
      case 0:
         RenderSystem.glUniform1(this.location, this.intValues);
         break;
      case 1:
         RenderSystem.glUniform2(this.location, this.intValues);
         break;
      case 2:
         RenderSystem.glUniform3(this.location, this.intValues);
         break;
      case 3:
         RenderSystem.glUniform4(this.location, this.intValues);
         break;
      default:
         LOGGER.warn("Uniform.upload called, but count value ({}) is  not in the range of 1 to 4. Ignoring.", (int)this.count);
      }

   }

   private void uploadAsFloat() {
      this.floatValues.rewind();
      switch(this.type) {
      case 4:
         RenderSystem.glUniform1(this.location, this.floatValues);
         break;
      case 5:
         RenderSystem.glUniform2(this.location, this.floatValues);
         break;
      case 6:
         RenderSystem.glUniform3(this.location, this.floatValues);
         break;
      case 7:
         RenderSystem.glUniform4(this.location, this.floatValues);
         break;
      default:
         LOGGER.warn("Uniform.upload called, but count value ({}) is not in the range of 1 to 4. Ignoring.", (int)this.count);
      }

   }

   private void uploadAsMatrix() {
      this.floatValues.clear();
      switch(this.type) {
      case 8:
         RenderSystem.glUniformMatrix2(this.location, false, this.floatValues);
         break;
      case 9:
         RenderSystem.glUniformMatrix3(this.location, false, this.floatValues);
         break;
      case 10:
         RenderSystem.glUniformMatrix4(this.location, false, this.floatValues);
      }

   }

   public int getLocation() {
      return this.location;
   }

   public int getCount() {
      return this.count;
   }

   public int getType() {
      return this.type;
   }

   public IntBuffer getIntBuffer() {
      return this.intValues;
   }

   public FloatBuffer getFloatBuffer() {
      return this.floatValues;
   }
}