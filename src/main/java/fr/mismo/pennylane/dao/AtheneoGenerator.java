package fr.mismo.pennylane.dao;

import fr.mismo.pennylane.dao.entity.Identifiable;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.ParameterMode;
import java.io.Serializable;
import java.util.Properties;

@Component
@Slf4j
public class AtheneoGenerator implements IdentifierGenerator, Configurable {

    @Autowired
    EntityManager entityManager;

    private static final String COMPTEUR_PARAM = "COMPTEUR";
    private static final String PAS_PARAM = "PAS";
    private static final String CHAMP_PARAM = "CHAMP";
    public static final String IDENTIFIER = "identifier";
    private String compteur;

    @Override
    public Serializable generate(final SharedSessionContractImplementor session, final Object obj) {
        if (obj instanceof Identifiable) {
            final Identifiable identifiable = (Identifiable) obj;
            final int id = identifiable.getId();
            if (id > 0)
                return id;
        }

        log.trace("Génération d'un identifiant pour le compteur {}", compteur);


        final ProcedureCall call = session.createStoredProcedureCall("sp_COMPTEUR");
        call.registerParameter(CHAMP_PARAM, String.class, ParameterMode.IN);
        call.registerParameter(PAS_PARAM, Integer.class, ParameterMode.IN); // Doit être IN, pas OUT
        call.registerParameter(COMPTEUR_PARAM, Integer.class, ParameterMode.OUT); // Ce paramètre est OUT

        call.setParameter(CHAMP_PARAM, compteur);
        call.setParameter(PAS_PARAM, 1); // Paramètre d'entrée

        call.getOutputs();  // Exécuter la procédure stockée
        return (Integer) call.getOutputParameterValue(COMPTEUR_PARAM);
    }

    @Override
    public void configure(final Type type, final Properties params, final ServiceRegistry serviceRegistry) {
        compteur = params.getProperty(IDENTIFIER);
    }
}
