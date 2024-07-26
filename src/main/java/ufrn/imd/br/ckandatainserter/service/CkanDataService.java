package ufrn.imd.br.ckandatainserter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import ufrn.imd.br.ckandatainserter.model.TaxeData;
import ufrn.imd.br.ckandatainserter.repository.TaxeDataRepository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

@Service
public class CkanDataService {
    @Autowired
    private RestTemplate restTemplate;

    private TaxeDataRepository taxeDataRepository;

    private String ckanUrl = "http://10.7.41.210:80";
    private String jwtToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJJX1h3VkxDX3ZaS1Y4R3pZVWg0SXBYOUlGRHYxcTBNdDNwT3FDZS1yaEJBIiwiaWF0IjoxNzIyMDEyNzkzfQ.InSBvipZldB_TmiNCecBFqvcxsVh-b8Gg4wee2Uaey8";
    private String datasetId = "taxes-dataset"; // ID do dataset
    private String resourceId;
    private int currentPage = 0;
    private final int pageSize = 12;
    private boolean shouldCleanCsv = false;

    public CkanDataService(TaxeDataRepository taxeDataRepository) {
        this.taxeDataRepository = taxeDataRepository;
    }

    public void createDataset() {
        String requestData = """
    {
    "name": "taxes-dataset",
    "title": "Taxes DataSet",
    "notes": "Descrição detalhada do dataset",
    "author": "Nome do Autor",
    "author_email": "email@exemplo.com",
    "private": false,
    "license_id": "cc-by",
    "owner_org": "taxes-org"
    }
    """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", jwtToken);

        HttpEntity<String> entity = new HttpEntity<>(requestData, headers);

        String apiUrl = "http://10.7.41.210:80/api/3/action/package_create";
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
        System.out.println("Resposta: " + response.getBody());

    }

    public void uploadEmptyCsv() throws IOException {
        Path tempFile = createEmptyCsv();

        MultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv", Files.readAllBytes(tempFile));
        this.resourceId = uploadCsv(file);

        Files.delete(tempFile);
    }

    private Path createEmptyCsv() throws IOException {
        Path tempFile = Files.createTempFile("empty", ".csv");
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
            // Escrever um arquivo CSV completamente vazio
        }
        return tempFile;
    }

    private void cleanAndRestartCsv() throws IOException {
        // Create an empty CSV file
        Path tempFile = createEmptyCsv();

        // Upload the empty CSV to CKAN using resource_update
        MultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv", Files.readAllBytes(tempFile));
        updateResourceCsv(file);

        Files.delete(tempFile);
    }

    private MultiValueMap<String, Object> createBody(Path file) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("package_id", datasetId);
        body.add("upload", new FileSystemResource(file.toFile()));
        body.add("name", "data.csv");
        body.add("format", "csv");
        return body;
    }

    private String parseResourceId(String responseBody) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(responseBody);
        return rootNode.path("result").path("id").asText();
    }

    @Scheduled(fixedRate = 10000)
    public void updateCsv() {
        if (this.resourceId == null) {
            return;
        }

        try {
            if(shouldCleanCsv){
                cleanAndRestartCsv();
                shouldCleanCsv = false;
                System.out.println("Cleaned the CSV file with resource id: " + this.resourceId);
                return;
            }
            Path existingFile = downloadCsvFromCkan();
            List<TaxeData> next12Records;
            List<String> existingLines = new ArrayList<>();

            // Read existing file content
            try (Scanner scanner = new Scanner(existingFile)) {
                while (scanner.hasNextLine()) {
                    existingLines.add(scanner.nextLine());
                }
            }

            // Fetch next 12 records and append to existing lines
            next12Records = fetchNext12RecordsFromDatabase();
            List<String> newLines = next12Records.stream()
                    .map(record -> String.format("%d,%s,%s,%d,%s",
                            record.getId(),
                            record.getYear(),
                            record.getMonth(),
                            record.getValue(),
                            record.getTaxType().name()))
                    .collect(Collectors.toList());

            existingLines.addAll(newLines);

            // Write the updated content to a temporary file
            Path tempFile = Files.createTempFile("data-update", ".csv");
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
                for (String line : existingLines) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            // Upload the updated file to CKAN
            MultipartFile file = new MockMultipartFile("file", "data-update.csv", "text/csv", Files.readAllBytes(tempFile));
            updateResourceCsv(file);
            Files.delete(tempFile);

            if (next12Records.size() < pageSize) {
                // Reset to the first page if we have less records than page size (end of data)
                shouldCleanCsv = true;
                currentPage = 0;
            } else {
                currentPage++;
            }

            Files.delete(existingFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<TaxeData> fetchNext12RecordsFromDatabase() {
        Page<TaxeData> page = taxeDataRepository.findAll(PageRequest.of(currentPage, pageSize));
        return page.getContent();
    }

    private String uploadCsv(MultipartFile file) throws IOException {
        String url = ckanUrl + "/api/3/action/resource_create";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", jwtToken);

        Path tempFile = Files.createTempFile("ckan-upload", ".csv");
        Files.write(tempFile, file.getBytes());

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(createBody(tempFile), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        Files.delete(tempFile);

        return parseResourceId(response.getBody());
    }

    private void updateResourceCsv(MultipartFile file) throws IOException {
        String url = ckanUrl + "/api/3/action/resource_update";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", jwtToken);

        Path tempFile = Files.createTempFile("ckan-update", ".csv");
        Files.write(tempFile, file.getBytes());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", this.resourceId);
        body.add("upload", new FileSystemResource(tempFile.toFile()));

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        Files.delete(tempFile);
        System.out.println("Response from CKAN: " + response.getBody());
    }

    private Path downloadCsvFromCkan() throws IOException {
        // Primeiro, obtenha os detalhes do recurso para encontrar a URL de download
        String resourceDetailsUrl = String.format("%s/api/3/action/resource_show?id=%s", ckanUrl, resourceId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", jwtToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.postForEntity(resourceDetailsUrl, entity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new IOException("Failed to fetch resource details from CKAN, status code: " + response.getStatusCode());
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(response.getBody());
        String downloadUrl = rootNode.path("result").path("url").asText();

        if (downloadUrl == null || downloadUrl.isEmpty()) {
            throw new IOException("Download URL is empty or null");
        }

        // Adicionar log para verificar a URL de download
        System.out.println("Download URL: " + downloadUrl);

        // Agora, baixe o CSV a partir da URL obtida usando InputStream
        HttpHeaders downloadHeaders = new HttpHeaders();
        downloadHeaders.set("Authorization", jwtToken);

        HttpEntity<Void> downloadEntity = new HttpEntity<>(downloadHeaders);

        ResponseEntity<byte[]> downloadResponse = restTemplate.exchange(
                downloadUrl, HttpMethod.GET, downloadEntity, byte[].class);

        if (downloadResponse.getStatusCode() != HttpStatus.OK) {
            throw new IOException("Failed to download CSV from CKAN, status code: " + downloadResponse.getStatusCode());
        }

        byte[] body = downloadResponse.getBody();
        if (body == null || body.length == 0) {
            // Log a mensagem indicando que o arquivo está vazio
            System.out.println("Download CSV from CKAN is empty");
            body = new byte[0]; // Criar um corpo de resposta vazio
        }

        Path tempFile = Files.createTempFile("ckan-download", ".csv");
        Files.write(tempFile, body);
        return tempFile;
    }

    public String getResourceId(){
        return this.resourceId;
    }

}
