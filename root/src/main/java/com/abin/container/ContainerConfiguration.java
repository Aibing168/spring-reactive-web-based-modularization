package com.abin.container;

import com.abin.common.Module;
import com.abin.common.ModuleHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Configuration
public class ContainerConfiguration {

    @Bean
    ModuleManager moduleManager(ApplicationContext context) {
        return new ModuleManager(context);
    }

    @Bean
    ContainerWebFilter containerWebFilter(ModuleManager moduleManager) {
        return new ContainerWebFilter(moduleManager);
    }

    public static class ContainerWebFilter implements WebFilter {

        final ModuleManager moduleManager;

        public ContainerWebFilter(ModuleManager moduleManager) {
           this.moduleManager = moduleManager;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            return moduleManager.handle(exchange, chain);
        }

    }

    @Slf4j
    public static class ModuleManager {

        ApplicationContext parent;
        Collection<Module> modules = new ConcurrentLinkedQueue<>();

        public ModuleManager(ApplicationContext parent) {
            this.parent = parent;
            initialize();
        }

        void initialize() {
            //1. load modules
            Collection<Module> modules = new LinkedList<>();
            String dir = parent.getEnvironment().getProperty("modulesDir");
//            String dir = "/Users/zhaoaibing/ws/code/github/container/modules/hello/build/libs/";
            File folder = new File(dir);
            File[] files = folder.listFiles();
            List<String> jarFiles = new LinkedList<>();
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".jar")) {
                    jarFiles.add(file.getAbsolutePath());
                }
            }

            for (String jarFile : jarFiles) {
                modules.add(load(jarFile));
            }

            modules = modules.stream().filter(Objects::nonNull).sorted(Comparator.comparingInt(Module::priority))
                    .collect(ConcurrentLinkedQueue::new, ConcurrentLinkedQueue::add, ConcurrentLinkedQueue::addAll);

            //2. enable modules to serve
            this.modules = modules;
        }

        Mono<Void> handle(ServerWebExchange exchange, WebFilterChain chain) {
            for(Module module : modules) {
                if(module.supports(exchange, chain)) {
                    return module.handle(exchange, chain);
                }
            }
            //TODO: NotModuleFoundHandler
            return Mono.empty();
        }

        Module load(String path) {
            Module module = null;
            try {
                String resourcePath = "META-INF/module.properties";
                URLClassLoader classLoader = new URLClassLoader(new URL[]{new URL("file:" + path)}, getClass().getClassLoader());
                InputStream inputStream = classLoader.getResourceAsStream(resourcePath);
                Properties properties = new Properties();
                properties.load(new InputStreamReader(inputStream));
                String[] classes = ((String) properties.get("configuration")).split(",");
                AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
                context.setClassLoader(classLoader);
                context.setParent(parent);

                for (String clazz : classes) {
                    clazz = clazz.trim();
                    context.register(classLoader.loadClass(clazz));
                }
                context.refresh();

                Map<String, ModuleHandler> handlerMap = context.getBeansOfType(ModuleHandler.class);
                List<ModuleHandler> handlers = new ArrayList<>(handlerMap.values());
                handlers.sort(Comparator.comparingInt(ModuleHandler::priority));
                module = new Module(
                        properties.getProperty("name", "name-" + UUID.randomUUID()),
                        Integer.parseInt(properties.getProperty("priority", "0")),
                        classLoader,
                        context,
                        handlers);
            } catch (Exception e) {
                log.warn("failed to load module {}", path, e);
            }
            return module;
        }

        void serve() {

        }

        void unload(String name) {
            Optional<Module> optional = modules.stream().filter(m -> name.equals(m.name())).findFirst();
            optional.ifPresent(module -> modules.remove(module));
        }

    }

}
