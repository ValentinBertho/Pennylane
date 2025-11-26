package fr.mismo.pennylane.service;

import fr.mismo.pennylane.api.AccountsApi;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dao.repository.LogRepository;
import fr.mismo.pennylane.dto.Address;
import fr.mismo.pennylane.dto.accounting.Item;
import fr.mismo.pennylane.dto.ath.Tiers;
import fr.mismo.pennylane.dto.customer.Customer;
import fr.mismo.pennylane.dto.supplier.LedgerAccount;
import fr.mismo.pennylane.dto.supplier.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class TiersMapper {

    private static final Logger log = LoggerFactory.getLogger(TiersMapper.class);

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private AccountsApi accountsApi;

    // Expression régulière pour valider les adresses email
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
            Pattern.CASE_INSENSITIVE
    );

    public Customer mapToCustomer(Tiers tiers, SiteEntity site, String noFacture) {
        try {
            if (tiers == null) {
                throw new IllegalArgumentException("Le tiers ne peut pas être null");
            }
            if (site == null) {
                throw new IllegalArgumentException("Le site ne peut pas être null");
            }

            String compteComptable = tiers.getCompteComptable();
            if (compteComptable == null || compteComptable.trim().isEmpty()) {
                log.warn("Le compte comptable est null ou vide pour le tiers ID: {}", tiers.getIdUnique());
                compteComptable = "";
            }
            String targetNumber = removeTrailingZerosString(compteComptable);

            Customer customer = new Customer();
            mapCommonFields(tiers, customer, noFacture);

            // Gestion plan item
            Item ledger = accountsApi.getLedgerAccountByNumber(sanitizeAccountNumber(targetNumber), site);

            LedgerAccount ledgerAccount = new LedgerAccount();
            ledgerAccount.setId(Long.valueOf(ledger.getId()));
            customer.setLedgerAccount(ledgerAccount);

            return customer;
        } catch (Exception e) {
            String tiersId = tiers != null && tiers.getIdUnique() != null ? tiers.getIdUnique().toString() : "inconnu";
            log.error("Erreur lors du mapping du tiers en client pour le tiers {}: {}", tiersId, e.getMessage(), e);
            logRepository.ajouterLigneForum("V_FACTURE", noFacture, "Erreur dans mapToCustomer : " + e.getMessage(), 2);
            throw e;
        }
    }

    private void mapCommonFields(Tiers tiers, Object entity, String noFacture) {
        try {
            if (tiers == null) {
                throw new IllegalArgumentException("Le tiers ne peut pas être null");
            }
            if (entity == null) {
                throw new IllegalArgumentException("L'entité cible ne peut pas être null");
            }

            if (entity instanceof Customer) {
                Customer customer = (Customer) entity;
                customer.setName(Optional.ofNullable(tiers.getRaisonSociale()).orElse(""));
                customer.setId(tiers.getIdUnique() != null ? Long.valueOf(tiers.getIdUnique()) : null);

                if (isValidEmail(tiers.getEmailRelance())) {
                    customer.setEmails(Collections.singletonList(tiers.getEmailRelance()));
                } else {
                    customer.setEmails(null);
                }

                // Billing address
                Address billingAddress = new Address();
                billingAddress.setAddress(Optional.ofNullable(tiers.getAdresse1()).orElse(""));
                billingAddress.setPostalCode(Optional.ofNullable(tiers.getCp()).orElse(""));
                billingAddress.setCity(Optional.ofNullable(tiers.getVille()).orElse(""));
                billingAddress.setCountryAlpha2(getCountryISOCode(Optional.ofNullable(tiers.getPays()).orElse("").trim()));
                customer.setBillingAddress(billingAddress);

                // Delivery address (copied from billing for now)
                Address deliveryAddress = new Address();
                deliveryAddress.setAddress(billingAddress.getAddress());
                deliveryAddress.setPostalCode(billingAddress.getPostalCode());
                deliveryAddress.setCity(billingAddress.getCity());
                deliveryAddress.setCountryAlpha2(billingAddress.getCountryAlpha2());
                customer.setDeliveryAddress(deliveryAddress);

                customer.setVatNumber(validateVatNumber(Optional.ofNullable(tiers.getTva()).orElse(""), billingAddress.getCountryAlpha2()));

                String siret = tiers.getSiret();
                if (siret != null && siret.length() >= 9) {
                    customer.setRegNo(siret.substring(0, 9)); // SIREN
                } else {
                    customer.setRegNo(null);
                }

                customer.setPhone(Optional.ofNullable(tiers.getTelephone()).orElse("").trim());
                customer.setPaymentConditions(Optional.ofNullable(tiers.getCodRglt()).orElse("").trim());

                // Champs supplémentaires qu'on peut mapper par défaut
                customer.setReference("REF-" + tiers.getIdUnique()); // Exemple, à adapter
                customer.setNotes("Créé automatiquement depuis Tiers");
                customer.setBillingIban(null); // à mapper ailleurs si dispo
                customer.setExternalReference(UUID.randomUUID().toString()); // exemple
                customer.setBillingLanguage("fr_FR");

            } else if (entity instanceof Supplier) {
                Supplier supplier = (Supplier) entity;
                supplier.setName(Optional.ofNullable(tiers.getRaisonSociale()).orElse(""));

                String siret = tiers.getSiret();
                if (siret != null && siret.length() >= 14) {
                    supplier.setEstablishmentNo(siret); // SIRET complet
                } else if (siret != null && siret.length() >= 9) {
                    supplier.setEstablishmentNo(siret);
                } else {
                    supplier.setEstablishmentNo(null);
                }

                supplier.setId(tiers.getIdUnique() != null ? Long.valueOf(tiers.getIdUnique()) : null);

                if (isValidEmail(tiers.getEmailRelance())) {
                    supplier.setEmails(Collections.singletonList(tiers.getEmailRelance()));
                } else {
                    supplier.setEmails(null);
                }

                Address address = new Address();
                address.setAddress(Optional.ofNullable(tiers.getAdresse1()).orElse(""));
                address.setPostalCode(Optional.ofNullable(tiers.getCp()).orElse(""));
                address.setCity(Optional.ofNullable(tiers.getVille()).orElse(""));
                address.setCountryAlpha2(getCountryISOCode(Optional.ofNullable(tiers.getPays()).orElse("").trim()));
                supplier.setPostalAddress(address);

                supplier.setVatNumber(validateVatNumber(Optional.ofNullable(tiers.getTva()).orElse(""), address.getCountryAlpha2()));

                supplier.setLedgerAccount(null);
                supplier.setIban(null);
                supplier.setSupplierPaymentMethod(null);
                supplier.setSupplierDueDateDelay(null);
                supplier.setSupplierDueDateRule(null);
                supplier.setExternalReference(null);
                supplier.setCreatedAt(null);
                supplier.setUpdatedAt(null);

            } else {
                throw new IllegalArgumentException("Type d'entité non supporté : " + entity.getClass().getName());
            }

        } catch (Exception e) {
            String tiersId = tiers != null && tiers.getIdUnique() != null ? tiers.getIdUnique().toString() : "inconnu";
            log.error("Erreur lors du mapping des champs communs pour le tiers {}: {}", tiersId, e.getMessage(), e);
            logRepository.ajouterLigneForum("V_FACTURE", noFacture, "Erreur dans mapCommonFields : " + e.getMessage(), 2);
            throw e;
        }
    }



    public static String removeTrailingZerosString(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input.replaceAll("0+$", ""); // Supprime les zéros en fin de chaîne
    }

    private String getCountryISOCode(String countryName) {
        try {
            if (countryName == null || countryName.isEmpty()) {
                return "FR";
            }

            Optional<String> countryCode = Optional.ofNullable(Locale.getISOCountries())
                    .flatMap(countries ->
                            Arrays.stream(countries)
                                    .map(code -> new Locale("", code))
                                    .filter(locale -> locale.getDisplayCountry(Locale.ENGLISH).equalsIgnoreCase(countryName) ||
                                            locale.getDisplayCountry(Locale.FRENCH).equalsIgnoreCase(countryName))
                                    .map(Locale::getCountry)
                                    .findFirst()
                    );

            return countryCode.orElse("FR"); // Valeur par défaut si le pays n'est pas trouvé
        } catch (Exception e) {
            log.error("Erreur lors de la conversion du nom de pays en code ISO: {}", e.getMessage(), e);
            return "FR";
        }
    }

    private String validateVatNumber(String vatNumber, String countryCode) {
        try {
            if (vatNumber == null || vatNumber.isEmpty() || countryCode == null || countryCode.isEmpty()) {
                return "";
            }
            String vatPattern = "^[A-Z]{2}\\d{2,13}$";
            if (vatNumber.matches(vatPattern) && vatNumber.startsWith(countryCode)) {
                return vatNumber;
            }
            return "";
        } catch (Exception e) {
            log.error("Erreur lors de la validation du numéro de TVA: {}", e.getMessage(), e);
            return "";
        }
    }

    public String sanitizeAccountNumber(String accountNumber) {
        try {
            if (accountNumber == null || accountNumber.isEmpty()) {
                return "";
            }
            return accountNumber.replaceAll("[^A-Z0-9]", "");
        } catch (Exception e) {
            log.error("Erreur lors de la sanitisation du numéro de compte: {}", e.getMessage(), e);
            return "";
        }
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        try {
            return EMAIL_PATTERN.matcher(email).find();
        } catch (Exception e) {
            log.error("Erreur lors de la validation de l'email: {}", e.getMessage(), e);
            return false;
        }
    }
}