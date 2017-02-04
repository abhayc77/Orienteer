package org.orienteer.core.service;

import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import org.apache.wicket.guice.GuiceWebApplicationFactory;
import org.apache.wicket.protocol.http.WicketFilter;
import org.orienteer.core.loader.OLoaderStorage;
import ru.ydn.wicket.wicketorientdb.rest.InterceptContentFilter;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vitaliy Gonchar
 */
public class OrienteerFilterInitModule extends ServletModule {
    private static final Map<String, String> INIT_PARAMS = new HashMap<>();
    static {
        INIT_PARAMS.put(WicketFilter.FILTER_MAPPING_PARAM, "/*");
        INIT_PARAMS.put("applicationFactoryClassName", GuiceWebApplicationFactory.class.getName());
        INIT_PARAMS.put("injectorContextAttribute", Injector.class.getName());
    }

    @Override
    protected void configureServlets() {
        bind(InterceptContentFilter.class).asEagerSingleton();
        filter("/orientdb/*").through(InterceptContentFilter.class);
        bind(WicketFilter.class).toProvider(new Provider<WicketFilter>() {
            @Override
            public WicketFilter get() {
                return new WicketFilter() {
                    @Override
                    protected ClassLoader getClassLoader() {
                        return OLoaderStorage.getRootLoader();
                    }
                };
            }
        }).in(Singleton.class);
        bind(ReloadFilter.class).in(Singleton.class);
        filter("/*").through(ReloadFilter.class);
        filter("/*").through(WicketFilter.class);
        bind(FilterConfigProvider.class).in(Singleton.class);
        bind(FilterConfig.class).toProvider(FilterConfigProvider.class);
    }

    /**
     * Provide FilterConfig
     */
    @Singleton
    public static class FilterConfigProvider implements Provider<FilterConfig> {
        private String filterName;
        private ServletContext servletContext;

        public FilterConfigProvider setFilterName(String filterName) {
            this.filterName = filterName;
            return this;
        }

        public FilterConfigProvider setServletContext(ServletContext servletContext) {
            this.servletContext = servletContext;
            return this;
        }

        @Override
        public FilterConfig get() {
            return new FilterConfig() {
                @Override
                public String getFilterName() {
                    return filterName;
                }

                @Override
                public ServletContext getServletContext() {
                    return servletContext;
                }

                @Override
                public String getInitParameter(String name) {
                    return INIT_PARAMS.get(name);
                }

                @Override
                public Enumeration<String> getInitParameterNames() {
                    return Collections.enumeration(INIT_PARAMS.keySet());
                }
            };
        }
    }

}
