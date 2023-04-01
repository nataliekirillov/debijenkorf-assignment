package com.debijenkorf.assignment.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class S3DirectoryStrategyTest {
    private S3DirectoryStrategy strategy;

    @BeforeEach
    public void setUp() {
        this.strategy = new S3DirectoryStrategy();
    }

    @ParameterizedTest
    @MethodSource("directoryStrategyParams")
    void testGetDirectoryStrategy(String typeName, String filename, String expectedResult) {
        String result = strategy.getDirectoryStrategy(typeName, filename);
        assertEquals(expectedResult, result);
    }

    static Stream<Arguments> directoryStrategyParams() {
        return Stream.of(
                Arguments.of("thumbnail", "abcdefghij.jpg", "thumbnail/abcd/efgh/abcdefghij.jpg"),
                Arguments.of("thumbnail", "abcde.jpg", "thumbnail/abcd/abcde.jpg"),
                Arguments.of("thumbnail", "/somedir/anotherdir/abcdef.jpg",
                        "thumbnail/_som/edir/_somedir_anotherdir_abcdef.jpg"),
                Arguments.of("thumbnail", "%2Fsomedir%2Fanotherdir%2Fabcdef.jpg",
                        "thumbnail/_som/edir/_somedir_anotherdir_abcdef.jpg")
        );
    }
}