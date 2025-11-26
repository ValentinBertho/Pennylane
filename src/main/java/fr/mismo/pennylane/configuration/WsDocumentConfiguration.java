package fr.mismo.pennylane.configuration;

import fr.mismo.pennylane.settings.WsDocumentProperties;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

@Configuration
@Profile("!dev")
public class WsDocumentConfiguration {

  @Autowired
  private WsDocumentProperties wsDocumentProperties;

  @Bean
  public Jaxb2Marshaller marshaller() {
    final Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    marshaller.setContextPath("fr.mismo.wsdocument.wsdl");
    return marshaller;
  }

  @Bean
  public WebServiceTemplate webServiceTemplate(final Jaxb2Marshaller marshaller) {
    final WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
    webServiceTemplate.setDefaultUri(wsDocumentProperties.getDefaultUri());
    webServiceTemplate.setMarshaller(marshaller);
    webServiceTemplate.setUnmarshaller(marshaller);
    webServiceTemplate.setMessageSender(httpComponentsMessageSender());
    return webServiceTemplate;
  }

  @Bean
  public HttpComponentsMessageSender httpComponentsMessageSender() {
    final HttpComponentsMessageSender httpComponentsMessageSender = new HttpComponentsMessageSender();
    // set the basic authorization credentials
    httpComponentsMessageSender.setCredentials(usernamePasswordCredentials());
    httpComponentsMessageSender.setReadTimeout(wsDocumentProperties.getTimeouts().getReadTimeout());
    httpComponentsMessageSender.setConnectionTimeout(wsDocumentProperties.getTimeouts().getConnectionTimeout());
    return httpComponentsMessageSender;
  }

  @Bean
  public UsernamePasswordCredentials usernamePasswordCredentials() {
    // pass the user name and password to be used
    return new UsernamePasswordCredentials(wsDocumentProperties.getLogin(), wsDocumentProperties.getPassword());
  }

}
