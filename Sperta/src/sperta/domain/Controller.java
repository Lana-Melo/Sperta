package sperta.domain;

public class Controller extends Thread{
        /*
         * Ora bem aquilo q espero q controller faça (delegação de competências)
         * -> o cliente pede pra criar casa (enum create) o controller chama o Repository pra criar a casa
         * -> o cliente pede pra adicionar um user (enum add) o controller chama o security pra ver se esse user é o dono da casa 
         * se n nao pode adicionar outro user e depois chama o repository pra adicionar o user 
         * -> lá aquelas outras operations q é pra enviar ficheiros, tem de primeiro passar pelo security e depois vai ao logger buscar os dados 
         * pra criar ficheiro e asim por diante lalala 
         * 
         * 
         */
}
