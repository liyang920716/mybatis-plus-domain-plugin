package com.github.liyang920716.domain.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author liyang
 * @description
 * @date 2022-05-28 20:15:15
 */
@ConfigurationProperties(value = "domain")
public class DomainProperties {

    private String domainName;
    @Value(value = "${domain.enable:false}")
    private boolean enable;

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
