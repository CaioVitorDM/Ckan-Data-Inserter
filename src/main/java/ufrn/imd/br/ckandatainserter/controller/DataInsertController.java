package ufrn.imd.br.ckandatainserter.controller;

import org.springframework.web.bind.annotation.*;
import ufrn.imd.br.ckandatainserter.service.CkanDataService;

import java.io.IOException;

@RestController
@RequestMapping("/data")
public class DataInsertController {

    private CkanDataService service;

     DataInsertController(CkanDataService service, CkanDataService ckanDataService){
         this.service = service;
     }


     @PostMapping("/create-dataset")
    public void createDataSet(){
         service.createDataset();
     }

    @PostMapping("/insert-data")
    public void insertDataToDataSet() {
        try {
            service.uploadEmptyCsv();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
