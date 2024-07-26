package ufrn.imd.br.ckandatainserter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CkanConnection {

    @Autowired
    private RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(CkanConnection.class);

    public void testCkanConnection() {
        String ckanUrl = "http://10.7.41.210:80/api/3/action/site_read";
        ResponseEntity<String> response = restTemplate.getForEntity(ckanUrl, String.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            logger.info("Conexão com CKAN bem-sucedida!");
        } else {
            logger.error("Falha na conexão com CKAN. Status: {}", response.getStatusCode());
        }
    }
}
