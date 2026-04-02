package edu.arsw.proyecto.SchedulingService.infrastructure.config;

import edu.arsw.proyecto.SchedulingService.application.port.out.DomainLockPort;
import edu.arsw.proyecto.SchedulingService.domain.service.SchedulingDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public SchedulingDomainService schedulingDomainService(DomainLockPort domainLockPort) {
        return new SchedulingDomainService(domainLockPort);
    }
}
