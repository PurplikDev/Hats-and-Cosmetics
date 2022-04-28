package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.server.packs.PackResources;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ResourceLoadStateTracker {
   private static final Logger LOGGER = LogManager.getLogger();
   @Nullable
   private ResourceLoadStateTracker.ReloadState reloadState;
   private int reloadCount;

   public void startReload(ResourceLoadStateTracker.ReloadReason pReloadReason, List<PackResources> pPacks) {
      ++this.reloadCount;
      if (this.reloadState != null && !this.reloadState.finished) {
         LOGGER.warn("Reload already ongoing, replacing");
      }

      this.reloadState = new ResourceLoadStateTracker.ReloadState(pReloadReason, pPacks.stream().map(PackResources::getName).collect(ImmutableList.toImmutableList()));
   }

   public void startRecovery(Throwable p_168561_) {
      if (this.reloadState == null) {
         LOGGER.warn("Trying to signal reload recovery, but nothing was started");
         this.reloadState = new ResourceLoadStateTracker.ReloadState(ResourceLoadStateTracker.ReloadReason.UNKNOWN, ImmutableList.of());
      }

      this.reloadState.recoveryReloadInfo = new ResourceLoadStateTracker.RecoveryInfo(p_168561_);
   }

   public void finishReload() {
      if (this.reloadState == null) {
         LOGGER.warn("Trying to finish reload, but nothing was started");
      } else {
         this.reloadState.finished = true;
      }

   }

   public void fillCrashReport(CrashReport p_168563_) {
      CrashReportCategory crashreportcategory = p_168563_.addCategory("Last reload");
      crashreportcategory.setDetail("Reload number", this.reloadCount);
      if (this.reloadState != null) {
         this.reloadState.fillCrashInfo(crashreportcategory);
      }

   }

   @OnlyIn(Dist.CLIENT)
   static class RecoveryInfo {
      private final Throwable error;

      RecoveryInfo(Throwable pError) {
         this.error = pError;
      }

      public void fillCrashInfo(CrashReportCategory p_168569_) {
         p_168569_.setDetail("Recovery", "Yes");
         p_168569_.setDetail("Recovery reason", () -> {
            StringWriter stringwriter = new StringWriter();
            this.error.printStackTrace(new PrintWriter(stringwriter));
            return stringwriter.toString();
         });
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum ReloadReason {
      INITIAL("initial"),
      MANUAL("manual"),
      UNKNOWN("unknown");

      final String name;

      private ReloadReason(String p_168579_) {
         this.name = p_168579_;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class ReloadState {
      private final ResourceLoadStateTracker.ReloadReason reloadReason;
      private final List<String> packs;
      @Nullable
      ResourceLoadStateTracker.RecoveryInfo recoveryReloadInfo;
      boolean finished;

      ReloadState(ResourceLoadStateTracker.ReloadReason pReloadReason, List<String> pPacks) {
         this.reloadReason = pReloadReason;
         this.packs = pPacks;
      }

      public void fillCrashInfo(CrashReportCategory p_168593_) {
         p_168593_.setDetail("Reload reason", this.reloadReason.name);
         p_168593_.setDetail("Finished", this.finished ? "Yes" : "No");
         p_168593_.setDetail("Packs", () -> {
            return String.join(", ", this.packs);
         });
         if (this.recoveryReloadInfo != null) {
            this.recoveryReloadInfo.fillCrashInfo(p_168593_);
         }

      }
   }
}