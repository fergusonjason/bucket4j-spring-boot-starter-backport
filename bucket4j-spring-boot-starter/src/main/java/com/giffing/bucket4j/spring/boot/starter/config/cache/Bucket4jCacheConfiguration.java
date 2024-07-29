package com.giffing.bucket4j.spring.boot.starter.config.cache;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.giffing.bucket4j.spring.boot.starter.config.cache.jcache.JCacheBucket4jConfiguration;
import com.giffing.bucket4j.spring.boot.starter.config.condition.ConditionalOnBucket4jEnabled;

@Configuration
@ConditionalOnBucket4jEnabled
@AutoConfigureAfter(CacheAutoConfiguration.class)
@Import(value = {
        JCacheBucket4jConfiguration.class
})
public class Bucket4jCacheConfiguration {
}
