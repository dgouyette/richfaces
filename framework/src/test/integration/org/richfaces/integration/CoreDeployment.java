/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.richfaces.integration;

import static org.richfaces.integration.CoreFeature.CONFIGURATION_SERVICE;
import static org.richfaces.integration.CoreFeature.DEPENDENCY_INJECTOR;
import static org.richfaces.integration.CoreFeature.RESOURCE_CODEC;
import static org.richfaces.integration.CoreFeature.RESOURCE_HANDLER;
import static org.richfaces.integration.CoreFeature.SERVICE_LOADER;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.component.UIOutput;
import javax.faces.event.PostConstructApplicationEvent;
import javax.faces.event.PreDestroyApplicationEvent;
import javax.servlet.ServletContainerInitializer;

import org.richfaces.util.URLToStreamHelper;
import org.richfaces.util.base64.Codec;
import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.facesconfig20.WebFacesConfigDescriptor;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.richfaces.VersionBean;
import org.richfaces.application.CoreConfiguration;
import org.richfaces.application.DependencyInjectionServiceImpl;
import org.richfaces.application.DependencyInjector;
import org.richfaces.application.Initializable;
import org.richfaces.application.InitializationListener;
import org.richfaces.application.MessageFactory;
import org.richfaces.application.Module;
import org.richfaces.application.ServiceException;
import org.richfaces.application.ServiceLoader;
import org.richfaces.application.ServiceTracker;
import org.richfaces.application.ServicesFactory;
import org.richfaces.application.ServicesFactoryImpl;
import org.richfaces.application.Uptime;
import org.richfaces.application.configuration.ConfigurationItem;
import org.richfaces.application.configuration.ConfigurationItemSource;
import org.richfaces.application.configuration.ConfigurationItemsBundle;
import org.richfaces.application.configuration.ConfigurationService;
import org.richfaces.application.configuration.ConfigurationServiceHelper;
import org.richfaces.application.configuration.ConfigurationServiceImpl;
import org.richfaces.application.configuration.ValueExpressionHolder;
import org.richfaces.application.push.MessageDataSerializer;
import org.richfaces.application.push.MessageException;
import org.richfaces.application.push.PushContext;
import org.richfaces.application.push.PushContextFactory;
import org.richfaces.application.push.Request;
import org.richfaces.application.push.Session;
import org.richfaces.application.push.SessionFactory;
import org.richfaces.application.push.SessionManager;
import org.richfaces.application.push.SessionSubscriptionEvent;
import org.richfaces.application.push.SessionUnsubscriptionEvent;
import org.richfaces.application.push.Topic;
import org.richfaces.application.push.TopicEvent;
import org.richfaces.application.push.TopicKey;
import org.richfaces.application.push.TopicListener;
import org.richfaces.application.push.TopicsContext;
import org.richfaces.application.push.impl.AbstractTopic;
import org.richfaces.application.push.impl.DefaultMessageDataSerializer;
import org.richfaces.application.push.impl.MessageDataScriptString;
import org.richfaces.application.push.impl.PushContextFactoryImpl;
import org.richfaces.application.push.impl.PushContextImpl;
import org.richfaces.application.push.impl.RequestImpl;
import org.richfaces.application.push.impl.SessionFactoryImpl;
import org.richfaces.application.push.impl.SessionImpl;
import org.richfaces.application.push.impl.SessionManagerImpl;
import org.richfaces.application.push.impl.SessionQueue;
import org.richfaces.application.push.impl.TopicImpl;
import org.richfaces.application.push.impl.TopicsContextImpl;
import org.richfaces.application.push.impl.jms.JMSTopicsContextImpl;
import org.richfaces.cache.Cache;
import org.richfaces.ui.core.UIResource;
import org.richfaces.ui.core.UITransient;
import org.richfaces.deployment.Deployment;
import org.richfaces.el.GenericsIntrospectionService;
import org.richfaces.el.util.ConstantValueExpression;
import org.richfaces.el.util.ELUtils;
import org.richfaces.javascript.JavaScriptService;
import org.richfaces.l10n.BundleLoader;
import org.richfaces.l10n.InterpolationException;
import org.richfaces.l10n.MessageBundle;
import org.richfaces.l10n.MessageInterpolator;
import org.richfaces.log.JavaLogger;
import org.richfaces.log.LogFactory;
import org.richfaces.log.Logger;
import org.richfaces.log.RichfacesLogger;
import org.richfaces.ui.ajax.AjaxDataSerializer;
import org.richfaces.ui.core.ResourceLibraryRenderer;
import org.richfaces.ui.core.ResourceRenderer;
import org.richfaces.resource.AbstractBaseResource;
import org.richfaces.resource.AbstractCacheableResource;
import org.richfaces.resource.BaseResourceWrapper;
import org.richfaces.resource.CachedResourceImpl;
import org.richfaces.resource.CompiledCSSResource;
import org.richfaces.resource.DefaultCodecResourceRequestData;
import org.richfaces.resource.DefaultResourceCodec;
import org.richfaces.resource.Java2DAnimatedUserResource;
import org.richfaces.resource.Java2DAnimatedUserResourceWrapperImpl;
import org.richfaces.resource.Java2DUserResource;
import org.richfaces.resource.Java2DUserResourceWrapperImpl;
import org.richfaces.resource.ResourceCodec;
import org.richfaces.resource.ResourceFactoryImpl;
import org.richfaces.resource.ResourceHandlerImpl;
import org.richfaces.resource.ResourceLibrary;
import org.richfaces.resource.ResourceLibraryFactory;
import org.richfaces.resource.ResourceLibraryFactoryImpl;
import org.richfaces.resource.ResourceLoadingOptimization;
import org.richfaces.resource.ResourceMappingConfiguration;
import org.richfaces.resource.ResourceMappingFeature;
import org.richfaces.resource.ResourceParameterELResolver;
import org.richfaces.resource.ResourceUtils;
import org.richfaces.resource.StaticResourceLibrary;
import org.richfaces.resource.UserResourceWrapperImpl;
import org.richfaces.resource.external.ExternalMapping;
import org.richfaces.resource.external.ExternalResourceTracker;
import org.richfaces.resource.external.ExternalResourceTrackerWrapper;
import org.richfaces.resource.external.ExternalStaticResource;
import org.richfaces.resource.external.ExternalStaticResourceFactory;
import org.richfaces.resource.external.ExternalStaticResourceFactoryImpl;
import org.richfaces.resource.external.MojarraExternalResourceTracker;
import org.richfaces.resource.external.MyFacesExternalResourceTracker;
import org.richfaces.shrinkwrap.descriptor.PropertiesAsset;
import org.richfaces.skin.SkinFactory;
import org.richfaces.util.ObjectInputStreamImpl;
import org.richfaces.util.PropertiesUtil;
import org.richfaces.util.RequestStateManager;
import org.richfaces.util.URLUtils;
import org.richfaces.util.Util;
import org.richfaces.wait.Condition;
import org.richfaces.wait.Wait;
import org.richfaces.wait.WaitTimeoutException;
import org.richfaces.webapp.GenericServletContainerInitializer;
import org.richfaces.webapp.PushFilter;
import org.richfaces.webapp.PushHandlerFilter;
import org.richfaces.webapp.PushServlet;
import org.richfaces.webapp.PushServletContainerInitializer;
import org.richfaces.webapp.ServletConfigDefaultsWrapper;

import com.google.common.base.Function;
import com.google.common.collect.Sets;

/**
 * Provides base for Core test deployment
 *
 * @author Lukas Fryc
 */
public class CoreDeployment extends Deployment {

    private static final String TESTING_MODULE = "META-INF/richfaces/testing-module.properties";

    private PropertiesAsset testingModules = new PropertiesAsset();
    private EnumSet<CoreFeature> addedFeatures = EnumSet.noneOf(CoreFeature.class);
    private Set<String> mavenDependencies = Sets.newHashSet();

    /**
     * Constructs base Core deployment with dependencies, base classes and utilities.
     */
    public CoreDeployment(Class<?> testClass) {
        super(testClass);

        // TODO versions have to be loaded from POM
        mavenDependencies.add("com.google.guava:guava");
        mavenDependencies.add("net.sourceforge.cssparser:cssparser:0.9.5");
        mavenDependencies.add("org.w3c.css:sac:1.3");

        this.withBaseClasses().withUtilities().withLogging();

        this.withArquillianExtensions().withWaiting();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.richfaces.deployment.Deployment#getFinalArchive()
     */
    @Override
    public WebArchive getFinalArchive() {
        WebArchive finalArchive = super.getFinalArchive();

        finalArchive.addManifest();
        Asset manifest = finalArchive.get("META-INF/MANIFEST.MF").getAsset();
        finalArchive.addAsResource(manifest, "META-INF/MANIFEST.MF");

        // add testing modules list
        finalArchive.addAsResource(testingModules, TESTING_MODULE);

        // add library dependencies
        addMavenDependencies(finalArchive);

        return finalArchive;
    }
    
    /**
     * Resolves maven dependencies, either by {@link MavenDependencyResolver} or from file cache
     */
    private void addMavenDependencies(WebArchive finalArchive) {
        
        Set<File> jarFiles =  Sets.newHashSet();
        
        for (String dependency : mavenDependencies) {
            File cacheDir = new File("target/shrinkwrap-resolver-cache/" + dependency);
            if (!cacheDir.exists()) {
                resolveMavenDependency(dependency, cacheDir);
            }
            File[] listFiles = cacheDir.listFiles(new FilenameFilter() {
                
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            });
            jarFiles.addAll(Arrays.asList(listFiles));
        }
        
        File[] files = jarFiles.toArray(new File[jarFiles.size()]);
        finalArchive.addAsLibraries(files);
    }
    
    /**
     * Resolves Maven dependency and writes it to the cache, so it can be reused next run
     */
    private void resolveMavenDependency(String missingDependency, File dir) {
        Collection<JavaArchive> dependencies = DependencyResolvers
                .use(MavenDependencyResolver.class)
                .loadEffectivePom("pom.xml")
                .artifact(missingDependency).resolveAs(JavaArchive.class);
        
        for (JavaArchive archive : dependencies) {
            dir.mkdirs();
            File outputFile = new File(dir, archive.getName());
            InputStream zipStream = archive.as(ZipExporter.class).exportAsInputStream();
            try {
                IOUtils.copy(zipStream, new FileOutputStream(outputFile));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Adds maven artifact as library dependency
     */
    public CoreDeployment addMavenDependency(String... dependencies) {
        mavenDependencies.addAll(Arrays.asList(dependencies));
        return this;
    }

    /**
     * Avoids to load given feature several times.
     *
     * Adds given {@link CoreFeature} to the list of loaded features.
     *
     * Returns whether the given feature was already loaded.
     *
     * @param feature a feature to load
     * @return true if a given feature was already loaded; false otherwise
     */
    private boolean addFeature(CoreFeature feature) {
        boolean alreadyAdded = addedFeatures.contains(feature);
        addedFeatures.add(feature);
        return alreadyAdded;
    }

    /**
     * Adds base classes which are necessary for each Core test
     */
    public CoreDeployment withBaseClasses() {
        archive().addClasses(VersionBean.class);
        return this;
    }

    /**
     * Adds all utility classes
     */
    public CoreDeployment withUtilities() {
        archive().addClasses(Util.class, PropertiesUtil.class, URLUtils.class);
        archive().addClasses(RequestStateManager.class);
        archive().addClasses(URLToStreamHelper.class, ObjectInputStreamImpl.class);
        archive().addClasses(ELUtils.class, ConstantValueExpression.class);
        return this;
    }

    /**
     * Adds {@link ConfigurationService} service
     */
    public CoreDeployment withConfigurationService() {
        if (addFeature(CONFIGURATION_SERVICE)) {
            return this;
        }

        withService(ConfigurationService.class, ConfigurationServiceImpl.class);
        archive()
            .addClasses(ConfigurationService.class, ConfigurationServiceImpl.class)
            .addClasses(ConfigurationServiceHelper.class)
            .addClasses(ConfigurationItem.class, ConfigurationItemsBundle.class, ConfigurationItemSource.class)
            .addClasses(ValueExpressionHolder.class);
        return this;
    }

    /**
     * Adds {@link DependencyInjector} service
     */
    public CoreDeployment withDependencyInjector() {
        if (addFeature(DEPENDENCY_INJECTOR)) {
            return this;
        }

        withService(DependencyInjector.class, DependencyInjectionServiceImpl.class);
        archive().addClasses(DependencyInjectionServiceImpl.class);

        return this;
    }

    /**
     * Adds core localization classes
     */
    public CoreDeployment withLocalization() {
        if (addFeature(CoreFeature.LOCALIZATION)) {
            return this;
        }

        archive()
            .addClasses(BundleLoader.class, InterpolationException.class, MessageBundle.class, MessageInterpolator.class);


        return this;
    }

    /**
     * Adds {@link ResourceCodec} service
     * @return
     */
    public CoreDeployment withResourceCodec() {
        if (addFeature(RESOURCE_CODEC)) {
            return this;
        }

        withService(ResourceCodec.class, DefaultResourceCodec.class);
        archive().addClasses(ResourceCodec.class, DefaultResourceCodec.class);

        return this;
    }

    /**
     * Adds Push and all its dependencies
     */
    public CoreDeployment withPush() {
        if (addFeature(CoreFeature.PUSH)) {
            return this;
        }

        withService(PushContextFactory.class, PushContextFactoryImpl.class);

        StringAsset pushServletInitializer = new StringAsset(PushServletContainerInitializer.class.getName());

        // According to ServletContainerInitializer, the service must be defined in own JAR
        JavaArchive servletInitializer = ShrinkWrap.create(JavaArchive.class, "push-classes.jar")
                .addAsResource(pushServletInitializer, "META-INF/services/" + ServletContainerInitializer.class.getName())
                .addClasses(PushServlet.class, PushFilter.class, PushHandlerFilter.class, ServletConfigDefaultsWrapper.class)
                .addClasses(PushServletContainerInitializer.class, GenericServletContainerInitializer.class)
                .addClasses(TopicsContext.class, TopicsContextImpl.class)
                .addClasses(SessionManager.class, SessionManagerImpl.class, SessionQueue.class)
                .addClasses(SessionFactory.class, SessionFactoryImpl.class)
                .addClasses(Session.class, SessionImpl.class)
                .addClasses(Topic.class, TopicImpl.class, AbstractTopic.class, TopicKey.class, TopicEvent.class, TopicListener.class)
                .addClasses(MessageDataSerializer.class, DefaultMessageDataSerializer.class, MessageDataScriptString.class)
                .addClasses(Request.class, RequestImpl.class)
                .addClasses(PushContext.class, PushContextImpl.class)
                .addClasses(PushContextFactory.class, PushContextFactoryImpl.class)
                .addClasses(SessionSubscriptionEvent.class, SessionUnsubscriptionEvent.class)
                .addClasses(MessageException.class)
                .addClasses(JMSTopicsContextImpl.class);


        archive()
            .addAsLibrary(servletInitializer);

        // TODO versions have to be loaded from POM
        addMavenDependency(
                "org.atmosphere:atmosphere-runtime:1.0.0.beta5",
                "org.atmosphere:atmosphere-compat-jbossweb:1.0.0.beta5",
                "org.atmosphere:atmosphere-compat-tomcat:1.0.0.beta5",
                "org.atmosphere:atmosphere-compat-tomcat7:1.0.0.beta5");

        return this;
    }

    /**
     * Adds Resource Libraries feature
     * @return
     */
    public CoreDeployment withResourceLibraries() {
        if (addFeature(CoreFeature.RESOURCE_LIBRARIES)) {
            return this;
        }

        archive()
            .addClasses(ResourceLibrary.class, ResourceLibraryFactory.class, ResourceLibraryFactoryImpl.class)
            .addClasses(ResourceLibraryRenderer.class, StaticResourceLibrary.class)
            .addClasses(ResourceRenderer.class, UIResource.class, UITransient.class);

        withService(ResourceLibraryFactory.class, ResourceLibraryFactoryImpl.class);

        facesConfig(new Function<WebFacesConfigDescriptor, WebFacesConfigDescriptor>() {

            @Override
            public WebFacesConfigDescriptor apply(WebFacesConfigDescriptor input) {
                return input
                        .getOrCreateRenderKit()
                            .createRenderer()
                                .componentFamily(UIOutput.COMPONENT_FAMILY)
                                .rendererType(ResourceLibraryRenderer.RENDERER_TYPE)
                                .rendererClass(ResourceLibraryRenderer.class.getName())
                            .up()
                        .up();
            }
        });

        return this;
    }

    /**
     * Adds core logging classes
     */
    public CoreDeployment withLogging() {
        if (addFeature(CoreFeature.LOGGING)) {
            return this;
        }

        archive()
            // log
            .addClasses(RichfacesLogger.class, JavaLogger.class, LogFactory.class, Logger.class);

        this.withLocalization();

        return this;
    }

    /**
     * Adds {@link ResourceHandlerImpl} and its dependencies
     */
    public CoreDeployment withResourceHandler() {
        if (addFeature(RESOURCE_HANDLER)) {
            return this;
        }

        archive()
            // resource
            .addClasses(ResourceHandlerImpl.class, ResourceFactoryImpl.class)
            .addClasses(ResourceLoadingOptimization.class, ResourceMappingConfiguration.class, ResourceMappingFeature.class)
            .addClasses(Java2DAnimatedUserResource.class, Java2DUserResource.class, Java2DAnimatedUserResourceWrapperImpl.class, Java2DUserResourceWrapperImpl.class)
            .addClasses(AbstractBaseResource.class, AbstractCacheableResource.class, CachedResourceImpl.class)
            .addClasses(BaseResourceWrapper.class, UserResourceWrapperImpl.class)
            .addClasses(CompiledCSSResource.class, DefaultResourceCodec.class, DefaultCodecResourceRequestData.class)
            .addClasses(ResourceUtils.class, ResourceParameterELResolver.class)
            // resource.external
            .addClasses(ExternalMapping.class, ExternalResourceTracker.class, ExternalResourceTrackerWrapper.class)
            .addClasses(MojarraExternalResourceTracker.class, MyFacesExternalResourceTracker.class)
            .addClasses(ExternalStaticResource.class, ExternalStaticResourceFactory.class, ExternalStaticResourceFactoryImpl.class)
            // codec
            .addClasses(Codec.class)
            // cache
            .addClasses(Cache.class);

        withService(ExternalStaticResourceFactory.class, ExternalStaticResourceFactoryImpl.class);
        withService(ExternalResourceTracker.class, ExternalResourceTrackerWrapper.class);

        facesConfig(new Function<WebFacesConfigDescriptor, WebFacesConfigDescriptor>() {

            @Override
            public WebFacesConfigDescriptor apply(WebFacesConfigDescriptor facesConfig) {
                return facesConfig
                    .getOrCreateApplication()
                        .resourceHandler(ResourceHandlerImpl.class.getName())
                    .up();
            }
        });

        return this;
    }

    /**
     * Adds {@link CoreTestingRemoteExtension} (for testing Core with Arquillian) and its dependencies
     * @return
     */
    private CoreDeployment withArquillianExtensions() {
        archive().addAsServiceProvider(RemoteLoadableExtension.class, CoreTestingRemoteExtension.class);
        archive().addClasses(CoreTestingRemoteExtension.class, CoreServicesEnricher.class);
        archive().addClasses(ConfigurationService.class, SkinFactory.class, AjaxDataSerializer.class,
                ResourceCodec.class, Cache.class, Uptime.class, DependencyInjector.class, MessageFactory.class,
                ResourceLibraryFactory.class, PushContextFactory.class, JavaScriptService.class,
                GenericsIntrospectionService.class, ExternalResourceTracker.class, ExternalStaticResourceFactory.class);
        return this;
    }

    /**
     * Adds base classes for using waiting with timeouts
     */
    private CoreDeployment withWaiting() {
        archive().addClasses(Condition.class, Wait.class, WaitTimeoutException.class);
        return this;
    }

    /**
     * Adds service for given type and implementation
     */
    public CoreDeployment withService(Class<?> type, Class<?> implementation) {
        withServiceTracker();
        testingModules = testingModules.key(type.getName()).value(implementation.getName());
        return this;
    }

    /**
     * Adds {@link ServiceTracker}, its initializer and module which is able to load services added by
     * {@link #withService(Class, Class)}.
     */
    public CoreDeployment withServiceTracker() {
        if (addFeature(SERVICE_LOADER)) {
            return this;
        }

        // add base dependencies
        archive()
            .addClasses(InitializationListener.class, CoreConfiguration.class, ServicesFactory.class, ServicesFactoryImpl.class)
            .addClasses(ServiceLoader.class, ServiceException.class, ServiceTracker.class, Module.class, Initializable.class);

        withConfigurationService();

        // register testing module
        StringAsset moduleAsset = new StringAsset(TestingModule.class.getName());

        archive().addClass(TestingModule.class).addAsResource(moduleAsset, "META-INF/services/" + Module.class.getName());

        // register initialization listener
        archive().addClass(TestingInitializationListener.class);
        facesConfig(new Function<WebFacesConfigDescriptor, WebFacesConfigDescriptor>() {

            @Override
            public WebFacesConfigDescriptor apply(WebFacesConfigDescriptor facesConfig) {
                return facesConfig.getOrCreateApplication().createSystemEventListener()
                        .systemEventClass(PostConstructApplicationEvent.class.getName())
                        .systemEventListenerClass(TestingInitializationListener.class.getName()).up()
                        .createSystemEventListener().systemEventClass(PreDestroyApplicationEvent.class.getName())
                        .systemEventListenerClass(TestingInitializationListener.class.getName()).up().up();
            }
        });

        return this;
    }

    /**
     * Initialization listener which does not load any module by default, and delegates rather to modules added by
     * {@link TestingModule}.
     */
    public static class TestingInitializationListener extends InitializationListener {
        @Override
        protected void addDefaultModules(List<Module> modules) {
        }
    }

    /**
     * Loads the list of services from properties file.
     *
     * You can add those using {@link CoreDeployment#withService(Class, Class)}.
     */
    public static class TestingModule implements Module {
        @Override
        public void configure(ServicesFactory factory) {
            ClassLoader classLoader = Module.class.getClassLoader();

            Map<String, String> map = PropertiesUtil.loadProperties(TESTING_MODULE);

            for (Entry<String, String> entry : map.entrySet()) {
                try {
                    Class<?> factoryClass = classLoader.loadClass(entry.getKey());
                    Class<?> implementationClass = classLoader.loadClass(entry.getValue());
                    Object implementationInstance = implementationClass.newInstance();

                    Method method = factory.getClass().getMethod("setInstance", new Class<?>[] { Class.class, Object.class });
                    method.invoke(factory, factoryClass, implementationInstance);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }
}