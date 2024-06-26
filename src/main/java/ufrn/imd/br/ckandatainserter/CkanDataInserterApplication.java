package ufrn.imd.br.ckandatainserter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class CkanDataInserterApplication implements CommandLineRunner {

    @Autowired
    private CkanConnection ckanConnection;

    public static void main(String[] args) {
        SpringApplication.run(CkanDataInserterApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        ckanConnection.testCkanConnection();
    }
}
