package net.minecraft;

import java.lang.Thread.UncaughtExceptionHandler;
import org.slf4j.Logger;

public class DefaultUncaughtExceptionHandler implements UncaughtExceptionHandler {
   private final Logger logger;

   public DefaultUncaughtExceptionHandler(Logger pLogger) {
      this.logger = pLogger;
   }

   public void uncaughtException(Thread pThread, Throwable pException) {
      this.logger.error("Caught previously unhandled exception :", pException);
   }
}