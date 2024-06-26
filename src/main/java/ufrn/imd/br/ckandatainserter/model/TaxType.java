package ufrn.imd.br.ckandatainserter.model;

public enum TaxType {
    IRPF("Imposto de Renda sobre Pessoa Física"),
    IRPJ("Imposto de Renda sobre Pessoa Jurídica"),
    ISI("Imposto sobre Importação");

    private final String description;

    TaxType(String description){
        this.description = description;
    }

    public String getDescription(){
        return this.description;
    }


}
