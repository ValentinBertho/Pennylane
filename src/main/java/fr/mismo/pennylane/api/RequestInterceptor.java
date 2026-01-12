package fr.mismo.pennylane.api;

import fr.mismo.pennylane.service.LogHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RequestInterceptor implements ClientHttpRequestInterceptor {

    private final LogHelper logHelper;

    // ✅ Injection par constructeur (plus fiable)
    public RequestInterceptor(LogHelper logHelper) {
        this.logHelper = logHelper;
    }

    @Override
    public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
                                        final ClientHttpRequestExecution execution) throws IOException {
        traceRequest(request, body);

        final long startTime = System.currentTimeMillis();
        ClientHttpResponse response = null;

        try {
            response = execution.execute(request, body);
            traceResponse(response, request, body, startTime);
        } catch (Exception e) {
            // ✅ Vérification de sécurité au cas où
            if (logHelper != null) {
                try {
                    logHelper.error("API_CALL", "Erreur lors de l'appel HTTP vers " + request.getURI(), e);
                } catch (Exception logException) {
                    log.error("Impossible de logger en base : {}", logException.getMessage());
                }
            } else {
                log.error("LogHelper est null - Erreur API : {}", e.getMessage(), e);
            }
            throw e;
        }

        return response;
    }

    private void traceRequest(final HttpRequest request, final byte[] body) {
        log.trace("===========================request begin================================================");
        log.trace("URI         : {}", request.getURI());
        log.trace("Method      : {}", request.getMethod());
        log.trace("Headers     : {}", request.getHeaders());
        log.trace("Request body: {}", new String(body, StandardCharsets.UTF_8));
        log.trace("==========================request end================================================");
    }

    private static String readResponseBody(ClientHttpResponse response) throws IOException {
        try (InputStream inputStream = response.getBody();
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return bufferedReader.lines().collect(Collectors.joining("\n"));
        }
    }

    private void traceResponse(final ClientHttpResponse response, final HttpRequest request,
                               final byte[] body, long startTime) throws IOException {
        boolean isError = response.getStatusCode().isError();
        long dureeMs = System.currentTimeMillis() - startTime;
        String url = request.getURI().toString();
        String methode = request.getMethod().toString();

        if (!isError) {
            log.trace("============================response begin==========================================");
            log.trace("Status code  : {}", response.getStatusCode());
            log.trace("Status text  : {}", response.getStatusText());
            log.trace("Headers      : {}", response.getHeaders());
            log.trace("=======================response end=================================================");

        } else {
            String responseTxt = readResponseBody(response);

            log.debug("=========================== [ERREUR] request begin ================================================");
            log.debug("URI         : {}", request.getURI());
            log.debug("Method      : {}", request.getMethod());
            log.debug("Headers     : {}", request.getHeaders());
            log.debug("Request body: {}", new String(body, StandardCharsets.UTF_8));
            log.debug("========================== [ERREUR] request end ================================================");

            log.debug("============================ [ERREUR] response begin ==========================================");
            log.debug("[ERREUR] Status code  : {}", response.getStatusCode());
            log.debug("[ERREUR] Status text  : {}", response.getStatusText());
            log.debug("[ERREUR] Body response: {}", responseTxt);
            log.debug("[ERREUR] Headers      : {}", response.getHeaders());
            log.debug("======================= [ERREUR] response end =================================================");

            // ✅ Protection contre le null
            if (logHelper != null) {
                try {
                    logHelper.logRestCall(
                            "API_CALL",
                            methode,
                            url,
                            response.getStatusCode().value(),
                            dureeMs,
                            new String(body, StandardCharsets.UTF_8),
                            responseTxt
                    );
                } catch (Exception e) {
                    log.error("Impossible de logger en base : {}", e.getMessage());
                }
            } else {
                log.error("LogHelper est null - impossible de logger l'erreur REST en base de données");
            }
        }
    }
}