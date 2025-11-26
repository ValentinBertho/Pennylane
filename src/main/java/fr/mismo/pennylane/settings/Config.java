package fr.mismo.pennylane.settings;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "facture")
public class Config {

    private String statusAFiltrer;

    private List<String> categoriesAFiltrer;

    private String daysBackward;
    private LocalDateTime lastInsertPurchases;

}