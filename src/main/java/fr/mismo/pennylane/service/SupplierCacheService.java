package fr.mismo.pennylane.service;

import fr.mismo.pennylane.api.SupplierApi;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dto.supplier.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class SupplierCacheService {

    @Autowired
    private SupplierApi supplierApi;

    @Cacheable(value = "supplierBySiteAndId", key = "#site.id + '-' + #supplierId")
    public Supplier getSupplier(String supplierId, SiteEntity site) {
        // Cette méthode est appelée une fois par couple (site, fournisseur) et le résultat est mis en cache
        return supplierApi.retrieveSupplier(supplierId, site);
    }
}
