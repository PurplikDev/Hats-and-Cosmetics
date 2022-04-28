package com.mojang.blaze3d.shaders;

import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AbstractUniform {
   public void set(float pX) {
   }

   public void set(float pX, float pY) {
   }

   public void set(float pX, float pY, float pZ) {
   }

   public void set(float pX, float pY, float pZ, float pW) {
   }

   public void setSafe(float pX, float pY, float pZ, float pW) {
   }

   public void setSafe(int pX, int pY, int pZ, int pW) {
   }

   public void set(int pX) {
   }

   public void set(int pX, int pY) {
   }

   public void set(int pX, int pY, int pZ) {
   }

   public void set(int pX, int pY, int pZ, int pW) {
   }

   public void set(float[] pValueArray) {
   }

   public void set(Vector3f pVector) {
   }

   public void set(Vector4f pVector) {
   }

   public void setMat2x2(float pX, float pY, float pZ, float pW) {
   }

   public void setMat2x3(float p_166485_, float p_166486_, float p_166487_, float p_166488_, float p_166489_, float p_166490_) {
   }

   public void setMat2x4(float p_166491_, float p_166492_, float p_166493_, float p_166494_, float p_166495_, float p_166496_, float p_166497_, float p_166498_) {
   }

   public void setMat3x2(float p_166544_, float p_166545_, float p_166546_, float p_166547_, float p_166548_, float p_166549_) {
   }

   public void setMat3x3(float p_166499_, float p_166500_, float p_166501_, float p_166502_, float p_166503_, float p_166504_, float p_166505_, float p_166506_, float p_166507_) {
   }

   public void setMat3x4(float p_166508_, float p_166509_, float p_166510_, float p_166511_, float p_166512_, float p_166513_, float p_166514_, float p_166515_, float p_166516_, float p_166517_, float p_166518_, float p_166519_) {
   }

   public void setMat4x2(float p_166550_, float p_166551_, float p_166552_, float p_166553_, float p_166554_, float p_166555_, float p_166556_, float p_166557_) {
   }

   public void setMat4x3(float p_166558_, float p_166559_, float p_166560_, float p_166561_, float p_166562_, float p_166563_, float p_166564_, float p_166565_, float p_166566_, float p_166567_, float p_166568_, float p_166569_) {
   }

   public void setMat4x4(float p_166520_, float p_166521_, float p_166522_, float p_166523_, float p_166524_, float p_166525_, float p_166526_, float p_166527_, float p_166528_, float p_166529_, float p_166530_, float p_166531_, float p_166532_, float p_166533_, float p_166534_, float p_166535_) {
   }

   public void set(Matrix4f pMatrix) {
   }

   public void set(Matrix3f p_200933_) {
   }
}