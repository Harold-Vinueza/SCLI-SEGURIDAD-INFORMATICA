package com.uteq.SCLI.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private SessionAuthInterceptor sessionAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionAuthInterceptor)
                .addPathPatterns("/**")
                // ¡importante!: NO interceptar recursos públicos/estáticos
                .excludePathPatterns(
                        "/login",
                        "/logout",
                        "/error",
                        "/favicon.ico",
                        "/LABU.png",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/static/**",
                          "/cambiar-clave"   
                );
    }

    // Opcional: mapea explícitamente los estáticos (no hace daño)
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**").addResourceLocations("classpath:/static/images/");
        registry.addResourceHandler("/LABU.png").addResourceLocations("classpath:/static/LABU.png");
        registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/static/favicon.ico");
 String base = System.getProperty("user.dir") + "/uploads/";
    registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + base);
    }



}


/*package com.uteq.SCLI.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.web.filter.HiddenHttpMethodFilter;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/**");

        
    }

     @Bean HiddenHttpMethodFilter hiddenHttpMethodFilter() { return new HiddenHttpMethodFilter(); }

    


}*/