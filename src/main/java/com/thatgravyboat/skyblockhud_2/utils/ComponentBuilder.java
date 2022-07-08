package com.thatgravyboat.skyblockhud_2.utils;

public class ComponentBuilder {

    public StringBuilder builder;

    public ComponentBuilder() {
        this.builder = new StringBuilder();
    }

    public ComponentBuilder apd(String text) {
        return apd(text, '7');
    }

    public ComponentBuilder apd(String text, char... colors) {
        for (char color : colors) {
            builder.append("\u00A7").append(color);
        }
        builder.append(text).append("\u00A7").append('r');
        return this;
    }

    public ComponentBuilder apd(String text, char color) {
        builder.append("\u00A7").append(color).append(text).append("\u00A7").append('r');
        return this;
    }

    public ComponentBuilder nl() {
        builder.append("\n");
        return this;
    }

    public ComponentBuilder nl(String text, char color) {
        apd(text, color);
        builder.append("\n");
        return this;
    }

    public ComponentBuilder nl(String text, char... colors) {
        apd(text, colors);
        builder.append("\n");
        return this;
    }

    public ComponentBuilder nl(String text) {
        apd(text);
        builder.append("\n");
        return this;
    }

    public String build() {
        return builder.toString();
    }
}
