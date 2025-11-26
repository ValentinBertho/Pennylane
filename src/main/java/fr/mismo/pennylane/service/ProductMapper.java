package fr.mismo.pennylane.service;

import fr.mismo.pennylane.dao.repository.LogRepository;
import fr.mismo.pennylane.dto.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProductMapper {

    private static final Logger log = LoggerFactory.getLogger(ProductMapper.class);

    @Autowired
    private LogRepository logRepository;

    public Product mapToProduct(Product product, String noFacture) {
        try {
            if (product == null) {
                throw new IllegalArgumentException("Le produit ne peut pas être null");
            }

            Integer productId = product.getId();
            String sourceId = productId != null ? String.valueOf(productId) : null;

            // Copie défensive pour éviter de modifier l'original si jamais il est réutilisé ailleurs
            Product mappedProduct = new Product();

            // Mapping des champs avec gestion des null
            mappedProduct.setId(productId);
            mappedProduct.setExternalReference(product.getExternalReference());
            mappedProduct.setId(sourceId != null ? Integer.valueOf(sourceId.trim()) : null);
            mappedProduct.setVatRate(Optional.ofNullable(product.getVatRate()).map(String::trim).orElse(""));
            mappedProduct.setLabel(Optional.ofNullable(product.getLabel()).map(String::trim).orElse(""));
            mappedProduct.setDescription(Optional.ofNullable(product.getDescription()).map(String::trim).orElse(""));
            mappedProduct.setPriceBeforeTax(product.getPriceBeforeTax());
            mappedProduct.setReference(Optional.ofNullable(product.getReference()).map(String::trim).orElse(""));
            mappedProduct.setCurrency(product.getCurrency());
            mappedProduct.setUnit(product.getUnit());

            return mappedProduct;
        } catch (Exception e) {
            String productId = product != null && product.getId() != null ? product.getId().toString() : "inconnu";
            log.error("Erreur lors du mapping du produit pour le produit {}: {}", productId, e.getMessage(), e);
            logRepository.ajouterLigneForum("V_FACTURE", noFacture, "Erreur dans mapToProduct : " + e.getMessage(), 2);
            throw e;
        }
    }


    /**
     * Mise à jour d'un produit existant avec de nouvelles données
     * @param existingProduct Le produit existant à mettre à jour
     * @param newData Les nouvelles données à appliquer
     * @return Le produit mis à jour et encapsulé
     */
    public Product updateProduct(Product existingProduct, Product newData,String noFacture) {
        try {
            if (existingProduct == null) {
                throw new IllegalArgumentException("Le produit existant ne peut pas être null");
            }
            if (newData == null) {
                throw new IllegalArgumentException("Les nouvelles données produit ne peuvent pas être null");
            }

            // Préservation de l'ID original
            Integer productId = existingProduct.getId();

            // Application des nouvelles données avec gestion des null
            existingProduct.setVatRate(Optional.ofNullable(newData.getVatRate()).map(String::trim).orElse(existingProduct.getVatRate()));
            existingProduct.setLabel(Optional.ofNullable(newData.getLabel()).map(String::trim).orElse(existingProduct.getLabel()));
            existingProduct.setDescription(Optional.ofNullable(newData.getDescription()).map(String::trim).orElse(existingProduct.getDescription()));

            if (newData.getPriceBeforeTax() != null) {
                existingProduct.setPriceBeforeTax(newData.getPriceBeforeTax());
            }

            existingProduct.setReference(Optional.ofNullable(newData.getReference()).map(String::trim).orElse(existingProduct.getReference()));

            if (newData.getCurrency() != null) {
                existingProduct.setCurrency(newData.getCurrency());
            }

            if (newData.getUnit() != null) {
                existingProduct.setUnit(newData.getUnit());
            }

            return existingProduct;
        } catch (Exception e) {
            String productId = existingProduct != null && existingProduct.getId() != null ? existingProduct.getId().toString() : "inconnu";
            log.error("Erreur lors de la mise à jour du produit {}: {}", productId, e.getMessage(), e);
            logRepository.ajouterLigneForum("V_FACTURE", noFacture, "Erreur dans updateProduct : " + e.getMessage(), 2);
            throw e;
        }
    }

    /**
     * Valide un objet Product et retourne les erreurs éventuelles
     * @param product Le produit à valider
     * @return Message d'erreur ou null si le produit est valide
     */
    public String validateProduct(Product product) {
        try {
            if (product == null) {
                return "Le produit ne peut pas être null";
            }

            if (product.getLabel() == null || product.getLabel().trim().isEmpty()) {
                return "Le libellé du produit est obligatoire";
            }

            if (product.getVatRate() == null || product.getVatRate().trim().isEmpty()) {
                return "Le taux de TVA est obligatoire";
            }

            // Validation du prix
            if (product.getPriceBeforeTax() == null) {
                return "Le prix doit être défini";
            }

            return null; // Produit valide
        } catch (Exception e) {
            String productId = product != null && product.getId() != null ? product.getId().toString() : "inconnu";
            log.error("Erreur lors de la validation du produit {}: {}", productId, e.getMessage(), e);
            return "Erreur technique lors de la validation du produit: " + e.getMessage();
        }
    }
}