package org.peach.gateway.apis.init;

import org.peach.gateway.apis.loader.ApisLoader;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @Title: StartupLoaderEvent.java
 * @Description: springboot启动事件，启动时加载redis缓存
 * @author: Administrator
 * @date: 2026年5月29日 18:24:42
 */
@Slf4j
@Component
public class StartupLoaderEvent {

    private final ApisLoader snapshotLoader;

    public StartupLoaderEvent(ApisLoader snapshotLoader) {
        this.snapshotLoader = snapshotLoader;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadOnStartup(ApplicationReadyEvent event) {
        log.info("Application is ready, loading Redis snapshot...");
        snapshotLoader.refreshFromRedis();
        log.info("Redis snapshot loaded");
    }
}
