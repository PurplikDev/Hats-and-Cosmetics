package net.minecraft.advancements;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;

public class CriterionProgress {
   private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
   @Nullable
   private Date obtained;

   public boolean isDone() {
      return this.obtained != null;
   }

   public void grant() {
      this.obtained = new Date();
   }

   public void revoke() {
      this.obtained = null;
   }

   @Nullable
   public Date getObtained() {
      return this.obtained;
   }

   public String toString() {
      return "CriterionProgress{obtained=" + (this.obtained == null ? "false" : this.obtained) + "}";
   }

   public void serializeToNetwork(FriendlyByteBuf pBuffer) {
      pBuffer.writeBoolean(this.obtained != null);
      if (this.obtained != null) {
         pBuffer.writeDate(this.obtained);
      }

   }

   public JsonElement serializeToJson() {
      return (JsonElement)(this.obtained != null ? new JsonPrimitive(DATE_FORMAT.format(this.obtained)) : JsonNull.INSTANCE);
   }

   public static CriterionProgress fromNetwork(FriendlyByteBuf pBuffer) {
      CriterionProgress criterionprogress = new CriterionProgress();
      if (pBuffer.readBoolean()) {
         criterionprogress.obtained = pBuffer.readDate();
      }

      return criterionprogress;
   }

   public static CriterionProgress fromJson(String pDateTime) {
      CriterionProgress criterionprogress = new CriterionProgress();

      try {
         criterionprogress.obtained = DATE_FORMAT.parse(pDateTime);
         return criterionprogress;
      } catch (ParseException parseexception) {
         throw new JsonSyntaxException("Invalid datetime: " + pDateTime, parseexception);
      }
   }
}