package net.minecraft.network.chat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;

/**
 * A Component that shows the score for an entity which is selected by an {@link EntitySelector}.
 */
public class ScoreComponent extends BaseComponent implements ContextAwareComponent {
   private static final String SCORER_PLACEHOLDER = "*";
   private final String name;
   @Nullable
   private final EntitySelector selector;
   private final String objective;

   @Nullable
   private static EntitySelector parseSelector(String pEntitySelector) {
      try {
         return (new EntitySelectorParser(new StringReader(pEntitySelector))).parse();
      } catch (CommandSyntaxException commandsyntaxexception) {
         return null;
      }
   }

   public ScoreComponent(String pEntitySelector, String pObjective) {
      this(pEntitySelector, parseSelector(pEntitySelector), pObjective);
   }

   private ScoreComponent(String pName, @Nullable EntitySelector pSelector, String pObjective) {
      this.name = pName;
      this.selector = pSelector;
      this.objective = pObjective;
   }

   /**
    * Gets the name of the entity who owns this score.
    */
   public String getName() {
      return this.name;
   }

   @Nullable
   public EntitySelector getSelector() {
      return this.selector;
   }

   /**
    * Gets the name of the objective for this score.
    */
   public String getObjective() {
      return this.objective;
   }

   private String findTargetName(CommandSourceStack pCommandSourceStack) throws CommandSyntaxException {
      if (this.selector != null) {
         List<? extends Entity> list = this.selector.findEntities(pCommandSourceStack);
         if (!list.isEmpty()) {
            if (list.size() != 1) {
               throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
            }

            return list.get(0).getScoreboardName();
         }
      }

      return this.name;
   }

   private String getScore(String pUsername, CommandSourceStack pCommandSourceStack) {
      MinecraftServer minecraftserver = pCommandSourceStack.getServer();
      if (minecraftserver != null) {
         Scoreboard scoreboard = minecraftserver.getScoreboard();
         Objective objective = scoreboard.getObjective(this.objective);
         if (scoreboard.hasPlayerScore(pUsername, objective)) {
            Score score = scoreboard.getOrCreatePlayerScore(pUsername, objective);
            return Integer.toString(score.getScore());
         }
      }

      return "";
   }

   /**
    * Creates a copy of this component, losing any style or siblings.
    */
   public ScoreComponent plainCopy() {
      return new ScoreComponent(this.name, this.selector, this.objective);
   }

   public MutableComponent resolve(@Nullable CommandSourceStack pCommandSourceStack, @Nullable Entity pEntity, int pRecursionDepth) throws CommandSyntaxException {
      if (pCommandSourceStack == null) {
         return new TextComponent("");
      } else {
         String s = this.findTargetName(pCommandSourceStack);
         String s1 = pEntity != null && s.equals("*") ? pEntity.getScoreboardName() : s;
         return new TextComponent(this.getScore(s1, pCommandSourceStack));
      }
   }

   public boolean equals(Object p_131069_) {
      if (this == p_131069_) {
         return true;
      } else if (!(p_131069_ instanceof ScoreComponent)) {
         return false;
      } else {
         ScoreComponent scorecomponent = (ScoreComponent)p_131069_;
         return this.name.equals(scorecomponent.name) && this.objective.equals(scorecomponent.objective) && super.equals(p_131069_);
      }
   }

   public String toString() {
      return "ScoreComponent{name='" + this.name + "'objective='" + this.objective + "', siblings=" + this.siblings + ", style=" + this.getStyle() + "}";
   }
}