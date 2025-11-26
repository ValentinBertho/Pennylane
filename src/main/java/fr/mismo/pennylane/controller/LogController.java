package fr.mismo.pennylane.controller;

import fr.mismo.pennylane.dto.LogDTO;
import fr.mismo.pennylane.service.LogsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Controller
@RequestMapping("/logs")
public class LogController {

    @Autowired
    private LogsService logService;

    // Page d'accueil - Dashboard
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, Object> stats = logService.getDashboardStats();
        model.addAttribute("stats", stats);
        return "logs/dashboard";
    }

    // Page de recherche de logs
    @GetMapping("/recherche")
    public String recherche(
            @RequestParam(required = false) String niveau,
            @RequestParam(required = false) String traitement,
            @RequestParam(required = false) String initiateur,
            @RequestParam(required = false) String application,
            @RequestParam(required = false) String environnement,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateDebut,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFin,
            @RequestParam(required = false) String message,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "dateLog") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            Model model
    ) {
        // Charger les listes pour les filtres
        model.addAttribute("applications", logService.getApplications());
        model.addAttribute("environnements", logService.getEnvironnements());
        model.addAttribute("niveaux", logService.getNiveaux());

        // Rechercher les logs
        Page<LogDTO> logs = logService.rechercherLogs(
                niveau, traitement, initiateur, application, environnement,
                dateDebut, dateFin, message, page, size, sortBy, sortDirection
        );

        // Ajouter les données au modèle
        model.addAttribute("logs", logs);
        model.addAttribute("niveau", niveau);
        model.addAttribute("traitement", traitement);
        model.addAttribute("initiateur", initiateur);
        model.addAttribute("application", application);
        model.addAttribute("environnement", environnement);
        model.addAttribute("dateDebut", dateDebut);
        model.addAttribute("dateFin", dateFin);
        model.addAttribute("message", message);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDirection", sortDirection);

        return "logs/recherche";
    }

    // Page de détail d'un log
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        LogDTO log = logService.getLogById(id);
        if (log == null) {
            return "redirect:/logs/recherche";
        }
        model.addAttribute("log", log);
        return "logs/detail";
    }

    // Export d'un log en PDF
    @GetMapping("/export/pdf/{id}")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Integer id) {
        LogDTO log = logService.getLogById(id);
        if (log == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] pdfBytes = logService.generateLogPdf(log);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "log_" + id + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Page des erreurs
    @GetMapping("/erreurs")
    public String erreurs(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateDebut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model
    ) {
        if (dateDebut == null) {
            // Par défaut, dernières 24 heures
            dateDebut = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        }

        Page<LogDTO> logs = logService.getLogsAvecErreurs(dateDebut, page, size);
        model.addAttribute("logs", logs);
        model.addAttribute("dateDebut", dateDebut);

        return "logs/erreurs";
    }

    // Page des logs lents
    @GetMapping("/lents")
    public String logsLents(
            @RequestParam(defaultValue = "1000") Long seuilMs,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateDebut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model
    ) {
        if (dateDebut == null) {
            // Par défaut, dernières 24 heures
            dateDebut = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        }

        Page<LogDTO> logs = logService.getLogsLents(seuilMs, dateDebut, page, size);
        model.addAttribute("logs", logs);
        model.addAttribute("seuilMs", seuilMs);
        model.addAttribute("dateDebut", dateDebut);

        return "logs/lents";
    }
}