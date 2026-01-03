package com.sss.sync.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class SpaConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/**")
      .addResourceLocations("classpath:/static/")
      .resourceChain(true)
      .addResolver(new PathResourceResolver() {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
          Resource requestedResource = location.createRelative(resourcePath);

          // If the resource exists (e.g., JS, CSS files), return it
          if (requestedResource.exists() && requestedResource.isReadable()) {
            return requestedResource;
          }

          // For any other path (client-side routes), return index.html
          // But skip API paths and conflict paths
          if (!resourcePath.startsWith("api/") && 
              !resourcePath.startsWith("conflicts/") &&
              !resourcePath.startsWith("swagger-ui/") &&
              !resourcePath.startsWith("v3/api-docs/")) {
            return new ClassPathResource("static/index.html");
          }

          return null;
        }
      });
  }
}