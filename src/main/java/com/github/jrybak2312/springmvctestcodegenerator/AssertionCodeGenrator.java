package com.github.jrybak2312.springmvctestcodegenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.lang.System.lineSeparator;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;

/**
 * @author Igor Rybak
 * @since 11-Oct-2018
 */
public class AssertionCodeGenrator {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected 2 args but found " + args.length);
        }

        String source = args[0];
        String destination = args[1];
        String code = new AssertionCodeGenrator().generate(source);
        try (PrintWriter out = new PrintWriter(destination)) {
            out.println(code);
        }
    }

    private String generate(String source) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(source);
        JsonNode jsonNode = objectMapper.readTree(file);
        List<String> asserts = new ArrayList<>();

        iterateNode(asserts, jsonNode, singletonList("$"));

        return asserts.stream()
                .collect(joining(lineSeparator()));
    }

    private void iterateNode(List<String> asserts, JsonNode jsonNode, List<String> tokens) {
        if (jsonNode instanceof ArrayNode) {

            String path = getPath(tokens);
            asserts.add("");
            asserts.add(".andExpect(jsonPath(\"" + path + "\", hasSize(" + jsonNode.size() + ")))");

            Iterator<JsonNode> elements = jsonNode.elements();
            int i = 0;
            while (elements.hasNext()) {
                String newToken = "[" + i + "]";
                List<String> newTokens = createNewTokens(tokens, newToken);
                iterateNode(asserts, elements.next(), newTokens);
                i++;
                asserts.add("");
            }
        } else if (jsonNode instanceof ObjectNode) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            objectNode.fields().forEachRemaining(entry -> {
                String newToken = "." + entry.getKey();
                List<String> newTokens = createNewTokens(tokens, newToken);
                iterateNode(asserts, entry.getValue(), newTokens);
            });
        } else {
            String path = getPath(tokens);
            String assertionCode;
            if (jsonNode.isNull()) {
                assertionCode = ".andExpect(jsonPath(\"" + path + "\", is(nullValue())))";
            } else {
                assertionCode = ".andExpect(jsonPath(\"" + path + "\", is(" + jsonNode + ")))";
            }
            asserts.add(assertionCode);
        }
    }

    private String getPath(List<String> tokens) {
        return String.join("", tokens);
    }

    private List<String> createNewTokens(List<String> tokens, String newToken) {
        List<String> newTokens = new ArrayList<>(tokens);
        newTokens.add(newToken);
        return newTokens;
    }
}
