package org.avarion.pluginhider.exceptions;

public class SectionExpectedException extends IllegalStateException {
    public SectionExpectedException() {
        super("Expected to see a section first!");
    }
}