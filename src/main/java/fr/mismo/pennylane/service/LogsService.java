package fr.mismo.pennylane.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import fr.mismo.pennylane.dao.entity.ForumEntity;
import fr.mismo.pennylane.dao.entity.ForumLigneEntity;
import fr.mismo.pennylane.dao.entity.LogEntity;
import fr.mismo.pennylane.dao.entity.SynchroEntity;
import fr.mismo.pennylane.dao.repository.ForumLigneRepository;
import fr.mismo.pennylane.dao.repository.ForumRepository;
import fr.mismo.pennylane.dao.repository.LogRepository;
import fr.mismo.pennylane.dao.repository.SynchroRepository;
import fr.mismo.pennylane.dto.LogDTO;
import fr.mismo.pennylane.settings.ConfigLogs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

@Service
@Slf4j
public class LogsService {

    @Autowired
    LogRepository logRepository;

    @Autowired
    SynchroRepository synchroRepository;

    @Autowired
    ForumLigneRepository forumLigneRepository;

    @Autowired
    ForumRepository forumRepository;

    @Autowired
    ConfigLogs configLogs;

    // SYNCHRO MARQUAGE.
    public void ajoutMarquage(String entite, String noEntite, String info, String refExt) {
        try {
            SynchroEntity entity = new SynchroEntity();
            entity.setNomEntite(entite);
            entity.setNumeroEntite(noEntite);
            entity.setDateSync(LocalDateTime.now());
            entity.setInfo(info);
            entity.setCreerLe(LocalDateTime.now());
            entity.setModifLe(LocalDateTime.now());
            entity.setModifPar(configLogs.initiateur);
            entity.setCreerPar(configLogs.initiateur);
            entity.setRefExt(refExt);
            entity.setCodeStatut("SYNCHRONISE");
            synchroRepository.save(entity);
            log.trace("Marquage ajouté avec succès : {}", entity);
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout du marquage", e);
        }
    }

    // Forum ligne
    public void ajoutForumLigne(String entite, String noEntite, String message) {
        try {
            ForumEntity forum = getOrCreateForumEntity(entite, noEntite);

            ForumLigneEntity ligne = new ForumLigneEntity();
            ligne.setMessage(message);
            ligne.setCodUser(configLogs.initiateur);
            ligne.setCreatedAt(new Date());
            ligne.setCreatedBy(configLogs.initiateur);
            ligne.setNoForum(forum.getNoForum());
            ligne.setTypeMessage("message");
            ligne.setNiveau(5);

            forumLigneRepository.save(ligne);
            log.trace("Ligne de forum ajoutée avec succès : {}", ligne);
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout de la ligne de forum", e);
        }
    }

    private ForumEntity getOrCreateForumEntity(String entite, String noEntite) {
        try {
            Optional<ForumEntity> forumEntityOpt = Optional.empty();
            switch (entite) {
                case "INCIDENT":
                    forumEntityOpt = forumRepository.findByNoIncident(noEntite).stream().findFirst();
                    break;
            }
            return forumEntityOpt.orElse(initiateForum(entite, noEntite));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération ou de la création du forum", e);
            throw e;
        }
    }

    public ForumEntity initiateForum(String entite, String noEntite) {
        try {
            ForumEntity newForum = new ForumEntity();

            newForum.setCreatedAt(new Date());
            newForum.setCreatedBy(configLogs.initiateur);
            switch (entite) {
                case "INCIDENT":
                    newForum.setNoIncident(noEntite);
                    break;
            }

            forumRepository.save(newForum);
            log.trace("Forum initié avec succès : {}", newForum);
            return newForum;
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation du forum", e);
            throw e;
        }
    }

    // Log
    public void ajoutLog(String niveau, String traitement, String message) {
        try {
            if (configLogs.actif) {
                log.trace("Ajout de log : [Niveau={}, Traitement={}, Message={}]", niveau, traitement, message);
                logRepository.logEnregistrer(niveau, traitement, configLogs.initiateur, message);
                log.trace("Log ajouté avec succès : [Niveau={}, Traitement={}, Message={}]", niveau, traitement, message);
            } else {
                log.debug("Le système de log est désactivé dans le fichier YML.");
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout du log", e);
        }
    }

    // Recherche avec pagination
    public Page<LogDTO> rechercherLogs(
            String niveau,
            String traitement,
            String initiateur,
            String application,
            String environnement,
            Date dateDebut,
            Date dateFin,
            String message,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        // Normaliser les chaînes vides en null
        niveau = normalizeString(niveau);
        traitement = normalizeString(traitement);
        initiateur = normalizeString(initiateur);
        application = normalizeString(application);
        environnement = normalizeString(environnement);
        message = normalizeString(message);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<LogEntity> entities = logRepository.rechercheAvancee(
                niveau, traitement, initiateur, application, environnement,
                dateDebut, dateFin, message, pageable
        );

        log.info("Recherche avec: niveau={}, traitement={}, initiateur={}, app={}, env={}, debut={}, fin={}, message={}",
                niveau, traitement, initiateur, application, environnement, dateDebut, dateFin, message);

        return entities.map(this::convertToDTO);
    }

    // Méthode utilitaire
    private String normalizeString(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    // Récupérer un log par ID
    public LogDTO getLogById(Integer id) {
        Optional<LogEntity> entity = logRepository.findById(id);
        return entity.map(this::convertToDTO).orElse(null);
    }

    // Obtenir les statistiques par niveau
    public Map<String, Long> getStatistiquesParNiveau(Date dateDebut) {
        List<Object[]> stats = logRepository.getStatistiquesParNiveau(dateDebut);
        Map<String, Long> result = new LinkedHashMap<>();

        for (Object[] stat : stats) {
            String niveau = (String) stat[0];
            Long count = (Long) stat[1];
            result.put(niveau != null ? niveau : "INCONNU", count);
        }

        return result;
    }

    // Obtenir les statistiques par application
    public Map<String, Long> getStatistiquesParApplication(Date dateDebut) {
        List<Object[]> stats = logRepository.getStatistiquesParApplication(dateDebut);
        Map<String, Long> result = new LinkedHashMap<>();

        for (Object[] stat : stats) {
            String app = (String) stat[0];
            Long count = (Long) stat[1];
            result.put(app != null ? app : "INCONNU", count);
        }

        return result;
    }

    // Récupérer les logs avec erreurs
    public Page<LogDTO> getLogsAvecErreurs(Date dateDebut, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateLog"));
        Page<LogEntity> entities = logRepository.findLogsAvecErreurs(dateDebut, pageable);
        return entities.map(this::convertToDTO);
    }

    // Récupérer les logs lents
    public Page<LogDTO> getLogsLents(Long seuilMs, Date dateDebut, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LogEntity> entities = logRepository.findLogsLents(seuilMs, dateDebut, pageable);
        return entities.map(this::convertToDTO);
    }

    // Obtenir la liste des applications
    public List<String> getApplications() {
        return logRepository.findDistinctApplications();
    }

    // Obtenir la liste des environnements
    public List<String> getEnvironnements() {
        return logRepository.findDistinctEnvironnements();
    }

    // Obtenir la liste des niveaux
    public List<String> getNiveaux() {
        return logRepository.findDistinctNiveaux();
    }

    // Conversion Entity -> DTO
    private LogDTO convertToDTO(LogEntity entity) {
        LogDTO dto = new LogDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    // Obtenir un dashboard de statistiques
    public Map<String, Object> getDashboardStats() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7); // 7 derniers jours
        Date derniereSemaine = cal.getTime();

        Map<String, Object> stats = new HashMap<>();
        stats.put("statsParNiveau", getStatistiquesParNiveau(derniereSemaine));
        stats.put("statsParApplication", getStatistiquesParApplication(derniereSemaine));

        // Compter le total de logs
        long totalLogs = logRepository.count();
        stats.put("totalLogs", totalLogs);

        return stats;
    }

    // Générer un PDF pour un log
    public byte[] generateLogPdf(LogDTO log) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, baos);

        document.open();

        // Définir les polices
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.DARK_GRAY);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
        Font codeFont = FontFactory.getFont(FontFactory.COURIER, 8, BaseColor.BLACK);

        // En-tête du document
        addPdfHeader(document, log, titleFont);

        document.add(new Paragraph("\n"));

        // Informations générales
        addPdfSection(document, "INFORMATIONS GÉNÉRALES", headerFont);
        addPdfInfoTable(document, log, labelFont, normalFont);

        document.add(new Paragraph("\n"));

        // Informations techniques
        if (hasTehnicalInfo(log)) {
            addPdfSection(document, "INFORMATIONS TECHNIQUES", headerFont);
            addPdfTechnicalTable(document, log, labelFont, normalFont);
            document.add(new Paragraph("\n"));
        }

        // Message
        addPdfSection(document, "MESSAGE", headerFont);
        Paragraph message = new Paragraph(log.getMessage() != null ? log.getMessage() : "N/A", normalFont);
        message.setAlignment(Element.ALIGN_JUSTIFIED);
        message.setSpacingAfter(10);
        document.add(message);

        document.add(new Paragraph("\n"));

        // Stack Trace
        if (log.getStackTrace() != null && !log.getStackTrace().isEmpty()) {
            document.add(new Paragraph("\n"));
            addPdfSection(document, "STACK TRACE", headerFont);
            Paragraph stackTrace = new Paragraph(log.getStackTrace(), codeFont);
            stackTrace.setAlignment(Element.ALIGN_LEFT);
            stackTrace.setSpacingAfter(10);
            document.add(stackTrace);
        }

        // Trames REST
        if (log.getTrameRequete() != null || log.getTrameReponse() != null) {
            document.add(new Paragraph("\n"));

            if (log.getTrameRequete() != null) {
                addPdfSection(document, "TRAME REQUÊTE", headerFont);
                Paragraph requete = new Paragraph(log.getTrameRequete(), codeFont);
                requete.setSpacingAfter(10);
                document.add(requete);
                document.add(new Paragraph("\n"));
            }

            if (log.getTrameReponse() != null) {
                addPdfSection(document, "TRAME RÉPONSE", headerFont);
                Paragraph reponse = new Paragraph(log.getTrameReponse(), codeFont);
                reponse.setSpacingAfter(10);
                document.add(reponse);
            }
        }

        // Pied de page
        addPdfFooter(document, normalFont);

        document.close();
        writer.close();

        return baos.toByteArray();
    }

    private void addPdfHeader(Document document, LogDTO log, Font titleFont) throws DocumentException {
        Paragraph title = new Paragraph("RAPPORT DE LOG #" + log.getId(), titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(5);
        document.add(title);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Paragraph subtitle = new Paragraph("Généré le " + sdf.format(new Date()),
                FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY));
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);

        // Ligne de séparation
        LineSeparator line = new LineSeparator();
        line.setLineColor(BaseColor.LIGHT_GRAY);
        document.add(new Chunk(line));
    }

    private void addPdfSection(Document document, String sectionTitle, Font headerFont) throws DocumentException {
        Paragraph section = new Paragraph(sectionTitle, headerFont);
        section.setSpacingBefore(10);
        section.setSpacingAfter(10);
        document.add(section);
    }

    private void addPdfInfoTable(Document document, LogDTO log, Font labelFont, Font normalFont) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);
        table.setWidths(new int[]{30, 70});

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        addPdfTableRow(table, "Date du Log", log.getDateLog() != null ? sdf.format(log.getDateLog()) : "N/A", labelFont, normalFont);
        addPdfTableRow(table, "Niveau", log.getNiveau() != null ? log.getNiveau() : "N/A", labelFont, normalFont);
        addPdfTableRow(table, "Application", log.getApplication() != null ? log.getApplication() : "N/A", labelFont, normalFont);
        addPdfTableRow(table, "Environnement", log.getEnvironnement() != null ? log.getEnvironnement() : "N/A", labelFont, normalFont);
        addPdfTableRow(table, "Traitement", log.getTraitement() != null ? log.getTraitement() : "N/A", labelFont, normalFont);
        addPdfTableRow(table, "Initiateur", log.getInitiateur() != null ? log.getInitiateur() : "N/A", labelFont, normalFont);
        if (log.getIpSource() != null) {
            addPdfTableRow(table, "IP Source", log.getIpSource(), labelFont, normalFont);
        }

        document.add(table);
    }

    private void addPdfTechnicalTable(Document document, LogDTO log, Font labelFont, Font normalFont) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);
        table.setWidths(new int[]{30, 70});

        if (log.getClasse() != null) {
            addPdfTableRow(table, "Classe", log.getClasse(), labelFont, normalFont);
        }
        if (log.getMethode() != null) {
            addPdfTableRow(table, "Méthode", log.getMethode(), labelFont, normalFont);
        }
        if (log.getUrlAppellee() != null) {
            addPdfTableRow(table, "URL Appelée", log.getUrlAppellee(), labelFont, normalFont);
        }
        if (log.getMethodeHttp() != null) {
            addPdfTableRow(table, "Méthode HTTP", log.getMethodeHttp(), labelFont, normalFont);
        }
        if (log.getCodeRetourHttp() != null) {
            addPdfTableRow(table, "Code HTTP", log.getCodeRetourHttp().toString(), labelFont, normalFont);
        }
        if (log.getDureeMs() != null) {
            addPdfTableRow(table, "Durée", log.getDureeMs() + " ms", labelFont, normalFont);
        }

        document.add(table);
    }

    private void addPdfTableRow(PdfPTable table, String label, String value, Font labelFont, Font normalFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        labelCell.setPadding(8);
        labelCell.setBorder(Rectangle.BOX);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, normalFont));
        valueCell.setPadding(8);
        valueCell.setBorder(Rectangle.BOX);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addPdfFooter(Document document, Font normalFont) throws DocumentException {
        document.add(new Paragraph("\n\n"));

        LineSeparator line = new LineSeparator();
        line.setLineColor(BaseColor.LIGHT_GRAY);
        document.add(new Chunk(line));

        Paragraph footer = new Paragraph(
                "Ce rapport a été généré automatiquement par le Portail de Logs.\n" +
                        "Pour toute question, contactez l'équipe technique.",
                FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY)
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(10);
        document.add(footer);
    }

    private boolean hasTehnicalInfo(LogDTO log) {
        return log.getClasse() != null || log.getMethode() != null ||
                log.getUrlAppellee() != null || log.getMethodeHttp() != null ||
                log.getCodeRetourHttp() != null || log.getDureeMs() != null;
    }
}
