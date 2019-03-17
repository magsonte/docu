package com.codingkiwi.pdfSearch;

public class Query {

    private String rawInput;
    private String parsedInput;

    boolean isCommand() {

        if(this.parsedInput == null){
            this.parsedInput = this.rawInput.replaceAll("\\s+","");
        }

        return this.parsedInput.startsWith("@!");
    }

    Query(String input) {
        this.rawInput = input;
        this.parsedInput = null;
    }

    Command getCommand() {
        try{
            String cmd = this.parsedInput.substring(2);
            System.out.println(cmd);
            return Command.valueOf(cmd);
        }
        catch(IllegalArgumentException e){
            return Command.NOCMD;
        }

    }
}
