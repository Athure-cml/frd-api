package com.furuiduo.quote.mail;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(QuoteMailProperties.class)
public class QuoteMailConfig {}
