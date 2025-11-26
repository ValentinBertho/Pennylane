package fr.mismo.pennylane.service;

import fr.mismo.pennylane.dto.Document;
import fr.mismo.pennylane.settings.WsDocumentProperties;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

@Service
public class DocumentService {

    @Autowired
    private WsDocumentService wsDocumentService;

    @Autowired
    private WsDocumentProperties wsDocumentProperties;

    // Taille maximale d'un paquet (exemple, ajustez selon vos besoins)
    private static final int taillePaquetMax = 8192;

    public void creerDocumentFromBase64(Document document, String auteurDocument) {
        Path tempFile = null;
        try {
            // Décodage du document encodé en base64 en un tableau d'octets
            byte[] contenuDocument = Base64.getDecoder().decode(document.getDocument());

            // Création d'un fichier temporaire pour stocker le contenu décodé
            tempFile = Files.createTempFile("tempDocument", "." + document.getExtension());
            Files.write(tempFile, contenuDocument);

            // Préparation des propriétés du document
            String titreDocument = document.getTitle();
            String extension = document.getExtension();

            // Clés/valeurs supplémentaires à passer lors de la création du document
            Map<String, String> listeCleValeur = new HashMap<>();

            listeCleValeur.put("NO_A_FACTURE", document.getNo()); // Pour les abonnements

            WsDocumentProperties.NewDocumentProperties properties;

            // Ajout d'une logique spécifique pour les justificatifs QF
            switch (document.getType()) {
                default:
                    properties = wsDocumentProperties.getProprieteDocument();
                    break;
            }

            // Appel du service pour créer le document
            wsDocumentService.creerDocument(properties, titreDocument, extension, tempFile, listeCleValeur, auteurDocument);

        } catch (IOException e) {
            throw new ServiceException("Erreur lors de la création du document à partir du fichier base64", e);
        } finally {
            // Suppression du fichier temporaire après utilisation, même en cas d'erreur
            try {
                if (tempFile != null) {
                    Files.deleteIfExists(tempFile);
                }
            } catch (IOException e) {
                // Log l'erreur sans interrompre le flux principal
                System.err.println("Erreur lors de la suppression du fichier temporaire: " + e.getMessage());
            }
        }
    }

    public Document fetchDocument(String documentUrl, String type) {
        Document document = new Document();

        try {
            // Étape 1: Récupérer le contenu du fichier depuis l'URL
            URL url = new URL(documentUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            InputStream inputStream = connection.getInputStream();

            // Étape 2: Lire le fichier en tant que bytes et encoder en base64
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            byte[] fileBytes = byteArrayOutputStream.toByteArray();
            String base64Content = Base64.getEncoder().encodeToString(fileBytes);

            // Étape 3: Remplir l'objet Document
            document.setDocument(base64Content);
            document.setType(type); // Le type MIME pour un PDF
            document.setExtension(".pdf"); // L'extension du fichier

        } catch (IOException e) {
            e.printStackTrace();
        }

        return document;
    }
}
