package com.logspectra.ai.config;

import com.logspectra.ai.LogSpectraAiFacade;
import com.logspectra.ai.codecontext.CascadingCodeContextProvider;
import com.logspectra.ai.codecontext.CodeContextProvider;
import com.logspectra.ai.codecontext.GitCloneCodeContextProvider;
import com.logspectra.ai.codecontext.LocalFilesystemCodeContextProvider;
import com.logspectra.ai.codecontext.NoOpCodeContextProvider;
import com.logspectra.ai.codecontext.RemoteCodeContextProvider;
import com.logspectra.ai.codecontext.RemoteSourceReader;
import com.logspectra.ai.codecontext.ServiceSourceLocator;
import com.logspectra.ai.codecontext.ServiceSourceRegistry;
import com.logspectra.ai.codecontext.StackTraceFrameParser;
import com.logspectra.ai.model.ServiceSourceMapping;
import com.logspectra.ai.prompt.CodeAwarePromptEnricher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(LogSpectraAiProperties.class)
@ConditionalOnProperty(prefix = "logspectra.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LogSpectraAiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StackTraceFrameParser stackTraceFrameParser() {
        return new StackTraceFrameParser();
    }

    /**
     * YAML fallback registry. Also satisfies {@link ServiceSourceLocator} unless the host app
     * provides a {@code @Primary} locator (e.g. Redis-backed).
     */
    @Bean
    @ConditionalOnMissingBean(ServiceSourceRegistry.class)
    public ServiceSourceRegistry serviceSourceRegistry(LogSpectraAiProperties properties) {
        List<ServiceSourceMapping> mappings = new ArrayList<>();
        Map<String, LogSpectraAiProperties.ServiceMapping> services =
            properties.getCodeContext() != null ? properties.getCodeContext().getServices() : Map.of();
        for (Map.Entry<String, LogSpectraAiProperties.ServiceMapping> entry : services.entrySet()) {
            LogSpectraAiProperties.ServiceMapping cfg = entry.getValue();
            if (cfg == null) {
                continue;
            }
            mappings.add(ServiceSourceMapping.builder()
                .serviceName(entry.getKey())
                .localPath(cfg.getLocalPath())
                .gitUrl(cfg.getGitUrl())
                .defaultBranch(cfg.getDefaultBranch())
                .sourceRoots(cfg.getSourceRoots())
                .build());
        }
        return new ServiceSourceRegistry(mappings);
    }

    @Bean
    @ConditionalOnMissingBean
    public CodeAwarePromptEnricher codeAwarePromptEnricher() {
        return new CodeAwarePromptEnricher();
    }

    @Bean
    @ConditionalOnMissingBean
    public CodeContextProvider codeContextProvider(LogSpectraAiProperties properties,
                                                   ServiceSourceLocator locator,
                                                   StackTraceFrameParser frameParser,
                                                   ObjectProvider<RemoteSourceReader> remoteSourceReader) {
        LogSpectraAiProperties.CodeContext ctx = properties.getCodeContext();
        if (ctx == null || !ctx.isEnabled()) {
            return new NoOpCodeContextProvider();
        }

        LocalFilesystemCodeContextProvider local = new LocalFilesystemCodeContextProvider(
            locator,
            frameParser,
            ctx.getMaxSnippets(),
            ctx.getContextLines(),
            ctx.getMaxFileChars()
        );

        RemoteSourceReader reader = remoteSourceReader.getIfAvailable();
        RemoteCodeContextProvider remote = reader == null ? null : new RemoteCodeContextProvider(
            locator,
            frameParser,
            reader,
            ctx.getMaxSnippets(),
            ctx.getContextLines(),
            ctx.getMaxFileChars()
        );

        String provider = ctx.getProvider() == null ? "local" : ctx.getProvider().trim().toLowerCase();
        if ("github".equals(provider) || "remote".equals(provider)) {
            if (remote != null) {
                return remote;
            }
            return local;
        }
        if ("git".equals(provider)) {
            if (remote != null) {
                return new CascadingCodeContextProvider(local, remote);
            }
            return new GitCloneCodeContextProvider(locator, local);
        }
        // local (default): prefer disk, fall back to GitHub Contents when host provides RemoteSourceReader
        if (remote != null) {
            return new CascadingCodeContextProvider(local, remote);
        }
        return local;
    }

    @Bean
    @ConditionalOnMissingBean
    public LogSpectraAiFacade logSpectraAiFacade(LogSpectraAiProperties properties,
                                                 CodeContextProvider codeContextProvider,
                                                 CodeAwarePromptEnricher promptEnricher) {
        boolean enabled = properties.isEnabled()
            && properties.getCodeContext() != null
            && properties.getCodeContext().isEnabled();
        return new LogSpectraAiFacade(codeContextProvider, promptEnricher, enabled);
    }
}
