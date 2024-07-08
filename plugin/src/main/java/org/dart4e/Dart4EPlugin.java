/*
 * SPDX-FileCopyrightText: © The Dart4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/dart4e/dart4e
 */
package org.dart4e;

import org.dart4e.flutter.launch.FlutterHotReloadListener;
import org.dart4e.launch.DartHotReloadListener;
import org.dart4e.navigation.DartDependenciesUpdater;
import org.dart4e.navigation.WindowListener;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.ImageRegistry;
import org.osgi.framework.BundleContext;

import de.sebthom.eclipse.commons.AbstractEclipsePlugin;
import de.sebthom.eclipse.commons.BundleResources;
import de.sebthom.eclipse.commons.logging.PluginLogger;
import de.sebthom.eclipse.commons.logging.StatusFactory;
import net.sf.jstuff.core.reflection.Fields;
import net.sf.jstuff.core.validation.Assert;

/**
 * @author Sebastian Thomschke
 */
public class Dart4EPlugin extends AbstractEclipsePlugin {

   /**
    * during runtime you can get ID with getBundle().getSymbolicName()
    */
   public static final String PLUGIN_ID = Dart4EPlugin.class.getPackageName();

   private static @Nullable Dart4EPlugin instance;

   /**
    * @return the shared instance
    */
   public static Dart4EPlugin get() {
      return Assert.notNull(instance, "Default plugin instance is still null.");
   }

   public static PluginLogger log() {
      return get().getLogger();
   }

   public static BundleResources resources() {
      return get().getBundleResources();
   }

   public static StatusFactory status() {
      return get().getStatusFactory();
   }

   @Override
   public BundleResources getBundleResources() {
      var bundleResources = this.bundleResources;
      if (bundleResources == null) {
         bundleResources = this.bundleResources = new BundleResources(this, "src/main/resources");
      }
      return bundleResources;
   }

   @Override
   protected void initializeImageRegistry(final ImageRegistry registry) {
      for (final var field : Constants.class.getFields()) {
         if (Fields.isStatic(field) && field.getType() == String.class && field.getName().startsWith("IMAGE_")) {
            final String imagePath = Fields.read(null, field);
            if (imagePath != null) {
               registerImage(registry, imagePath);
            }
         }
      }
   }

   @Override
   public void start(final BundleContext context) throws Exception {
      super.start(context);
      instance = this;

      DartDependenciesUpdater.INSTANCE.install();
      DartHotReloadListener.INSTANCE.install();
      FlutterHotReloadListener.INSTANCE.install();
      WindowListener.INSTANCE.attach();
   }

   @Override
   public void stop(final BundleContext context) throws Exception {
      DartDependenciesUpdater.INSTANCE.uninstall();
      DartHotReloadListener.INSTANCE.uninstall();
      FlutterHotReloadListener.INSTANCE.uninstall();
      WindowListener.INSTANCE.detatch();

      instance = null;
      super.stop(context);
   }
}
