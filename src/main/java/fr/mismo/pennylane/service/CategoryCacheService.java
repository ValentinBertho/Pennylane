package fr.mismo.pennylane.service;

import fr.mismo.pennylane.api.InvoiceApi;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dto.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryCacheService {

    @Autowired
    private InvoiceApi invoiceApi;

    @Cacheable(value = "categoriesBySite", key = "#site.id")
    public List<Category> getCategories(SiteEntity site) {
        // Cette méthode sera appelée une fois par site et le résultat sera mis en cache
        return invoiceApi.listAllCategories(site);
    }
}

