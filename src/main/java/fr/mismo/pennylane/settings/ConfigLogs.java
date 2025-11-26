package fr.mismo.pennylane.settings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class ConfigLogs {

    @Value("${Log.Actif}")
    public boolean actif;

    @Value("${Log.Initiateur}")
    public String initiateur;

    @Value("${Log.niveau.ERROR}")
    public String error;

    @Value("${Log.niveau.DEBUG}")
    public String warn;

    @Value("${Log.niveau.INFO}")
    public String info;

    @Value("${Log.niveau.DEBUG}")
    public String debug;

    @Value("${Log.niveau.TRACE}")
    public String trace;

}
