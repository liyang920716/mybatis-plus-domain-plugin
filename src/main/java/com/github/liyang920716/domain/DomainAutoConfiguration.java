package com.github.liyang920716.domain;

import com.github.liyang920716.domain.plugin.DomainPlugin;
import com.github.liyang920716.domain.properties.DomainProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author liyang
 * @description
 * @date 2022-05-28 20:20:10
 */
@Configuration
@EnableConfigurationProperties(value = DomainProperties.class)
@ConditionalOnProperty(prefix = "domain", name = "enable", havingValue = "true")
public class DomainAutoConfiguration {

    @Autowired
    private DomainProperties domainProperties;

    @Bean
    public DomainPlugin domainPlugin() {
        return new DomainPlugin(domainProperties.getDomainName());
    }

}
