package com.senars.ui;

import com.senars.core.Thought;

public class ConsolePrinter {

    public void printExplanation(Thought report) {
        String border = "=======================================================================";
        String header = "🤖 S E N A R S :   E X P L A N A T I O N";
        System.out.println();
        System.out.println(border);
        System.out.println(header);
        System.out.println(border);
        System.out.println();
        // Simple word wrap for the explanation text
        String[] words = report.content().text().split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() + word.length() + 1 > border.length()) {
                System.out.println(line);
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }
        System.out.println(line); // Print the last line
        System.out.println();
        System.out.println(border);
        System.out.println();
        System.out.print("> "); // Re-print the prompt
    }
}
